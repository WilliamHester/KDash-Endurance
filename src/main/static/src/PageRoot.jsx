import React, { useState, useEffect, useRef } from "react";
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
// import GapChartPage from "./gapchart/GapChartPage";
import FuelChartPage from "./fuel/FuelChartPage";
import TrackMapPage from "./trackmap/TrackMapPage";
import App from "./App";

const MAX_DATA_POINTS = 3000;

const ALLOWED_HZ = [0.5, 1, 3, 6, 12, 30, 60];

const coerceToAllowedHz = (goal) => ALLOWED_HZ.reduce(function (prev, curr) {
  return (Math.abs(curr - goal) < Math.abs(prev - goal) ? curr : prev);
});

export default function App2() {
  const telemetryQueries = [
    'FuelLevel',
    'LAP_DELTA(FuelLevel)',
    'TrackTempCrew',
    'Speed',
    'LAP_DELTA(Speed)',
  ];

  const [sampleRateHz, setSampleRateHz] = useState(8);
  const [dataRange, setDataRange] = useState({
    min: 0,
    max: 300,
  });
  const [dataWindow, setDataWindow] = useState([-1, 0]);
  const [counter, setCounter] = useState(0);

  const [telemetryData, setTelemetryData] = useState([]);
  const [gapEntries, setGapEntries] = useState([]);
  const [lapEntries, setLapEntries] = useState([]);
  const [otherCarLapEntries, setOtherCarLapEntries] = useState([]);
  const [currentDrivers, setCurrentDrivers] = useState(new Map());

  const client = useRef(new LiveTelemetryServiceClient(`${location.origin}/api`)).current;

  const timeoutRef = useRef(null);
  const shouldClearData = useRef(false);
  const telemetryDataBuffer = useRef([]);
  const dataRangeBuffer = useRef(dataRange);
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
    setupStream('monitorDriverLaps', driverDistancesBuffer);
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
    request.setMinSessionTime(dataWindow[0]);
    request.setMaxSessionTime(dataWindow[1]);
    request.setQueriesList(telemetryQueries);

    console.log('Querying for new data. dataStart: %s, dataEnd: %s, sampleRateHz: %s', dataWindow[0], dataWindow[1], sampleRateHz);
    const telemetryStream = client.queryTelemetry(request, {});
    telemetryStream.on('data', response => {
      if (response.hasDataRanges()) {
        const dataRanges = response.getDataRanges();
        const updatedDataRange = {
          min: dataRanges.getSessionTime().getMin(),
          max: dataRanges.getSessionTime().getMax(),
        };
        dataRangeBuffer.current = updatedDataRange;
      } else {
        const telemetryData = response.getData();
        telemetryDataBuffer.current.push(telemetryData);
        const sessionTime = dataRangeBuffer.current;
        dataRangeBuffer.current.max = Math.max(sessionTime.max, telemetryData.getSessionTime());
      }
    });

    return () => {
      telemetryStream.cancel();
      telemetryDataBuffer.current = [];
      shouldClearData.current = true;
    };
  }, [client, counter]);

  useEffect(() => {
    if (telemetryData.length === 0) {
      // Haven't loaded any telemetry data yet. Nothing to do.
      return;
    }
    const min = telemetryData[0];
    const max = telemetryData[telemetryData.length - 1];

    const newHz = coerceToAllowedHz(1000 / (dataWindow[1] - dataWindow[0]));

    if (dataWindow[0] < min.getSessionTime()
      || dataWindow[1] > max.getSessionTime()
      || sampleRateHz != newHz) {
      timeoutRef.current = setTimeout(() => {
        setCounter(currentCount => currentCount + 1);
        setSampleRateHz(newHz);
      }, 500);
      return () => {
        clearTimeout(timeoutRef.current);
      };
    }
  }, [dataWindow]);

  useEffect(() => {
    const intervalId = setInterval(() => {
      if (telemetryDataBuffer.current.length > 0) {
        setTelemetryData(prev => {
          let dataToPrepend;
          if (shouldClearData.current) {
            dataToPrepend = [];
            shouldClearData.current = false;
          } else {
            dataToPrepend = prev;
          }
          const updated = [...dataToPrepend, ...telemetryDataBuffer.current].slice(-MAX_DATA_POINTS);
          telemetryDataBuffer.current = [];
          // TODO: Fix this. I think this needs a ref or something.
          if (dataWindow[0] == -1 && dataWindow[1] == 0 && updated.length > 0) {
            setDataWindow([updated[0].getSessionTime(), updated[updated.length - 1].getSessionTime()]);
          }
          return updated;
        });
        setDataRange(dataRangeBuffer.current);
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
    }, 250); // Update the UI 4 times per second. Adjust as needed.

    return () => clearInterval(intervalId); // Cleanup interval on unmount
  }, [dataWindow]); // Maybe adding dataWindow here will solve it?

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App />}>
          <Route path="" element={<LapLogPage entries={lapEntries} />} />
          <Route path="laps" element={<LapLogPage entries={lapEntries} />} />
          <Route path="otherlaps" element={<OtherCarsLapLogPage entries={otherCarLapEntries} drivers={currentDrivers} />} />
          <Route path="gaps" element={<GapsPage entries={gapEntries} drivers={currentDrivers} />} />
          {/* <Route path="gapchart" element={<GapChartPage distances={driverDistances} drivers={currentDrivers} />} /> */}
          <Route path="fuelcharts" element={
            <FuelChartPage telemetryData={telemetryData} dataRange={dataRange} dataWindow={dataWindow} setDataWindow={setDataWindow} />
          } />
          <Route path="trackmap" element={<TrackMapPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};