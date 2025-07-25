import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";
import { ConnectRequest, QueryTelemetryRequest } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import LapLogPage from "./laplog/LapLogPage";
import OtherCarsLapLogPage from "./laplog/OtherCarsLapLogPage";
import GapsPage from "./gaps/GapsPage";
import GapChartPage from "./gapchart/GapChartPage";
import FuelChartPage from "./fuel/FuelChartPage";
import TrackMapPage from "./trackmap/TrackMapPage";
import App from "./App";

const MAX_GAP_POINTS = 1000;
const DOWNSAMPLE_THRESHOLD = 2000;
const DEFAULT_WINDOW_SIZE_SECONDS = 300;
const DEFAULT_WINDOW_SIZE_LAPS = 2;

export default function App2() {
  const [sampleRateHz, setSampleRateHz] = useState(8);
  const [dataStart, setDataStart] = useState(-1.0);
  const [dataRanges, setDataRanges] = useState({
    sessionTime: {
      min: 0,
      max: DEFAULT_WINDOW_SIZE_SECONDS,
    },
    driverDistance: {
      min: 0,
      max: DEFAULT_WINDOW_SIZE_LAPS,
    },
  });

  const [telemetryData, setTelemetryData] = useState([]);
  const [gapEntries, setGapEntries] = useState([]);
  const [lapEntries, setLapEntries] = useState([]);
  const [otherCarLapEntries, setOtherCarLapEntries] = useState([]);
  const [currentDrivers, setCurrentDrivers] = useState(new Map());
  const [driverDistances, setDriverDistances] = useState([]);

  const client = useRef(new LiveTelemetryServiceClient(`${location.origin}/api`)).current;

  const telemetryDataBuffer = useRef([]);
  const dataRangesBuffer = useRef(dataRanges);
  const gapBuffer = useRef([]);
  const lapBuffer = useRef([]);
  const otherCarLapBuffer = useRef([]);
  const driverDistancesBuffer = useRef([]);

  useEffect(() => {
    const liveTelemetryServiceClient = client;

    const setupStream = (rpcMethodName, buffer) => {
      const request = new ConnectRequest();
      const stream = liveTelemetryServiceClient[rpcMethodName](request, {});
      stream.on('data', response => {
        buffer.current.push(response);
      });
      return stream;
    };

    // We can't have more than 6 simultaneous TCP connections to the same domain. This means that we need to combine
    // the streams into a single stream. Having 6 causes the browser to lock up when refreshing the page,
    // presumably because the page itself would be a 7th connection.
    // setupStream('monitorCurrentGaps', gapBuffer);
    const driverLapsStream = setupStream('monitorDriverLaps', lapBuffer);
//     setupStream('monitorOtherCarsLaps', otherCarLapBuffer);
    // setupStream('monitorDriverDistances', driverDistancesBuffer);
    // setupStream('monitorFuelLevel', fuelBuffer);

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

    return () => {
      driverLapsStream.cancel();
      driversStream.cancel();
    };
  }, [client]);

  useEffect(() => {
    const request = new QueryTelemetryRequest();
    request.setSampleRateHz(sampleRateHz);
    request.setMinSessionTime(dataStart);
    const telemetryStream = client.queryTelemetry(request, {});
    telemetryStream.on('data', response => {
      if (response.hasDataRanges()) {
        const dataRanges = response.getDataRanges();
        const updatedDataRanges = {
          sessionTime: {
            min: dataRanges.getSessionTime().getMin(),
            max: dataRanges.getSessionTime().getMax(),
          },
          driverDistance: {
            min: dataRanges.getDriverDistance().getMin(),
            max: dataRanges.getDriverDistance().getMax(),
          },
        };
        dataRangesBuffer.current = updatedDataRanges;
      } else {
        const telemetryData = response.getData();
        telemetryDataBuffer.current.push(telemetryData);
        const sessionTime = dataRangesBuffer.current.sessionTime;
        const driverDistance = dataRangesBuffer.current.driverDistance;
        dataRangesBuffer.current.driverDistance.max = Math.max(driverDistance.max, telemetryData.getDriverDistance());
        dataRangesBuffer.current.sessionTime.max = Math.max(sessionTime.max, telemetryData.getSessionTime());
      }
    });

    return () => {
      telemetryStream.cancel();
      telemetryDataBuffer.current = [];
      setTelemetryData([]);
    };
  }, [client, sampleRateHz])

  useEffect(() => {
    const intervalId = setInterval(() => {
      if (telemetryDataBuffer.current.length > 0) {
        setTelemetryData(prev => {
          const updated = [...prev, ...telemetryDataBuffer.current];
          if (updated.length > DOWNSAMPLE_THRESHOLD) {
            // TODO: Update this with start and end times and a reasonable number of data points to request.
            // The client won't really know the current rate unless we do some wacky math.
            setSampleRateHz(current => Math.max(0.1, current / 2));
          }
          telemetryDataBuffer.current = [];
          return updated;
        });
        setDataRanges(dataRangesBuffer.current);
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
          <Route path="fuelcharts" element={
            <FuelChartPage telemetryData={telemetryData} dataRanges={dataRanges} />
          } />
          <Route path="trackmap" element={ <TrackMapPage /> } />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};