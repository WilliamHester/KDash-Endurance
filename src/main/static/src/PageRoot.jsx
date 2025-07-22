import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";
import { ConnectRequest, MonitorTelemetryRequest } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import LapLogPage from "./laplog/LapLogPage";
import OtherCarsLapLogPage from "./laplog/OtherCarsLapLogPage";
import GapsPage from "./gaps/GapsPage";
import GapChartPage from "./gapchart/GapChartPage";
import FuelChartPage from "./fuel/FuelChartPage";
import TrackMapPage from "./trackmap/TrackMapPage";
import App from "./App";

const MAX_FUEL_POINTS = 1000;
const MAX_GAP_POINTS = 1000;
const DOWNSAMPLE_THRESHOLD = 1000;

const downsample = (array) => {
  // Every time an array grows to be larger than DOWNSAMPLE_THRESHOLD items, cut it in half.
  if (array.length < DOWNSAMPLE_THRESHOLD) {
    return array;
  }
  const factor = Math.floor(array.length / DOWNSAMPLE_THRESHOLD);
  // console.log('Array length is %d downsampling', array.length);
  return array.filter((_value, index) => index % factor === 0);
}

export default function App2() {
  const [sessionTimes, setSessionTimes] = useState([]);
  const [targetDriverDistances, setTargetDriverDistances] = useState([]);
  const [gapEntries, setGapEntries] = useState([]);
  const [lapEntries, setLapEntries] = useState([]);
  const [otherCarLapEntries, setOtherCarLapEntries] = useState([]);
  const [currentDrivers, setCurrentDrivers] = useState(new Map());
  const [driverDistances, setDriverDistances] = useState([]);
  const [fuelLevels, setFuelLevels] = useState([]);

  const sampleRateHz = useRef(100);

  const sessionTimesBuffer = useRef([]);
  const targetDriverDistancesBuffer = useRef([]);
  const gapBuffer = useRef([]);
  const lapBuffer = useRef([]);
  const otherCarLapBuffer = useRef([]);
  const driverDistancesBuffer = useRef([]);
  const fuelBuffer = useRef([]);

  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient(`${location.origin}/api`);

    const setupStream = (rpcMethodName, buffer) => {
      const request = new ConnectRequest();
      const stream = liveTelemetryServiceClient[rpcMethodName](request, {});
      stream.on('data', response => {
        buffer.current.push(response);
      });
    };

    // We can't have more than 6 simultaneous TCP connections to the same domain. This means that we need to combine
    // the streams into a single stream. Having 6 causes the browser to lock up when refreshing the page,
    // presumably because the page itself would be a 7th connection.
    // setupStream('monitorCurrentGaps', gapBuffer);
    setupStream('monitorDriverLaps', lapBuffer);
//     setupStream('monitorOtherCarsLaps', otherCarLapBuffer);
    // setupStream('monitorDriverDistances', driverDistancesBuffer);
    // setupStream('monitorFuelLevel', fuelBuffer);

    const request = new MonitorTelemetryRequest();
    request.setSampleRateHz(sampleRateHz.current);
    const stream = liveTelemetryServiceClient.monitorTelemetry(request, {});
    stream.on('data', response => {
      sessionTimesBuffer.current.push(response.getSessionTime());
      targetDriverDistancesBuffer.current.push(response.getDriverDistance());

      fuelBuffer.current.push(response.getFuelLevel());
    });

    // This stream updates state directly because it's a single map, not a growing list.
    // The workload is minimal.
    const driversRequest = new ConnectRequest();
    const driversStream = liveTelemetryServiceClient.monitorCurrentDrivers(driversRequest, {});
    driversStream.on('data', response => {
        if (response && response.getDriversList()) {
          const driverMap = new Map(
            response.getDriversList().map((driver) => [
              driver.getCarId(),
              {
                'carClassId': driver.getCarClassId(),
                'carClassName': driver.getCarClassName(),
                'driverName': driver.getDriverName(),
                'teamName': driver.getTeamName(),
                'carNumber': driver.getCarNumber()
              }
            ])
          );
          setCurrentDrivers(driverMap);
        }
    });
  }, []);

  useEffect(() => {
    const intervalId = setInterval(() => {
      if (sessionTimesBuffer.current.length > 0) {
        setSessionTimes(prev => {
          const updated = [...prev, ...sessionTimesBuffer.current];
          sessionTimesBuffer.current = [];
          return updated;
        });
      }
      if (targetDriverDistancesBuffer.current.length > 0) {
        setTargetDriverDistances(prevLevels => {
          const updated = [...prevLevels, ...targetDriverDistancesBuffer.current];
          targetDriverDistancesBuffer.current = [];
          return updated;
        });
      }
      if (fuelBuffer.current.length > 0) {
        setFuelLevels(prevLevels => {
          const updated = [...prevLevels, ...fuelBuffer.current];
          fuelBuffer.current = [];
          return updated;
        });
      }

      // Process Gaps
      if (gapBuffer.current.length > 0) {
        const lastMessage = gapBuffer.current[gapBuffer.current.length - 1];
        if (lastMessage && lastMessage.getGapsList) {
            setGapEntries(lastMessage.getGapsList());
        }
        gapBuffer.current = []; // Clear buffer
      }

      // Process Driver Laps
      if (lapBuffer.current.length > 0) {
        setLapEntries(prevEntries => {
          const updated = [...lapBuffer.current, ...prevEntries];
          lapBuffer.current = []; // Clear buffer
          return updated;
        });
      }

      // Process Other Car Laps
      if (otherCarLapBuffer.current.length > 0) {
        setOtherCarLapEntries(prevEntries => {
          const updated = [...otherCarLapBuffer.current, ...prevEntries];
          otherCarLapBuffer.current = []; // Clear buffer
          return updated;
        });
      }

      // Process Driver Distances
      if (driverDistancesBuffer.current.length > 0) {
        setDriverDistances(prevData => {
            let newData = [...prevData];
            for (const response of driverDistancesBuffer.current) {
                if (response && response.getSessionTime && response.getDistancesList) {
                    const newRow = [response.getSessionTime(), ...response.getDistancesList().map(d => d.getDriverDistance())];
                    if (newData.length === 0) {
                        newData = newRow.map(value => [value]);
                    } else {
                        for (const [index, value] of newRow.entries()) {
                            if (newData[index]) {
                                newData[index].push(value);
                            }
                        }
                    }
                }
            }
            driverDistancesBuffer.current = []; // Clear buffer

            if (newData.length > 0) {
                newData = newData.map(column => column.slice(-MAX_GAP_POINTS));
            }
            return newData;
        });
      }

    }, 250); // Update the UI 4 times per second. Adjust as needed.

    return () => clearInterval(intervalId); // Cleanup interval on unmount
  }, []); // Empty dependency array means this runs only once.

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={ <App/> }>
          <Route path="" element={ <LapLogPage entries={lapEntries} /> } />
          <Route path="laps" element={ <LapLogPage entries={lapEntries} /> } />
          <Route path="otherlaps" element={ <OtherCarsLapLogPage entries={otherCarLapEntries} drivers={currentDrivers} /> } />
          <Route path="gaps" element={ <GapsPage entries={gapEntries} drivers={currentDrivers} /> } />
          <Route path="gapchart" element={ <GapChartPage distances={driverDistances} drivers={currentDrivers} /> } />
          <Route path="fuelcharts" element={ <FuelChartPage targetDriverDistances={downsample(targetDriverDistances)} fuelLevels={downsample(fuelLevels)} /> } />
          <Route path="trackmap" element={ <TrackMapPage /> } />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};