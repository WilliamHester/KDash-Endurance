import React, { useState, useEffect, useRef } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";
import { ConnectRequest } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import LapLogPage from "./laplog/LapLogPage";
import OtherCarsLapLogPage from "./laplog/OtherCarsLapLogPage";
import GapsPage from "./gaps/GapsPage";
// import GapChartPage from "./gapchart/GapChartPage";
import TelemetryPage from "./telemetry/TelemetryPage.jsx";
import TrackMapPage from "./trackmap/TrackMapPage";
import App from "./App";
import RaceOverviewPage from "./overview/RaceOverviewPage.jsx";

export default function App2() {
  const [gapEntries, setGapEntries] = useState([]);
  const [lapEntries, setLapEntries] = useState([]);
  const [stintEntries, setStintEntries] = useState([]);
  const [otherCarLapEntries, setOtherCarLapEntries] = useState([]);
  const [currentDrivers, setCurrentDrivers] = useState(new Map());

  const client = useRef(new LiveTelemetryServiceClient(`${location.origin}/api`)).current;

  const gapBuffer = useRef([]);
  const lapBuffer = useRef([]);
  const stintEntryBuffer = useRef([]);
  const otherCarLapBuffer = useRef([]);

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
    
    const request = new ConnectRequest();
    const driverLapsStream = liveTelemetryServiceClient.monitorLaps(request, {});
    driverLapsStream.on('data', response => {
      if (response.hasDriverLap()) {
        lapBuffer.current.push(response.getDriverLap());
      }
      if (response.hasDriverStint()) {
        stintEntryBuffer.current.push(response.getDriverStint());
      }
    });

    setupStream('monitorOtherCarsLaps', otherCarLapBuffer);
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
    const intervalId = setInterval(() => {
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

      if (stintEntryBuffer.current.length > 0) {
        setStintEntries(prevEntries => {
          const updated = [...stintEntryBuffer.current, ...prevEntries];
          stintEntryBuffer.current = []; // Clear buffer
          console.log(updated);
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
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App />}>
          <Route path="" element={<RaceOverviewPage drivers={currentDrivers} lapLog={lapEntries} otherCarLapEntries={otherCarLapEntries} />} />
          <Route path="laps" element={<LapLogPage entries={lapEntries} />} />
          <Route path="otherlaps" element={<OtherCarsLapLogPage entries={otherCarLapEntries} drivers={currentDrivers} />} />
          <Route path="gaps" element={<GapsPage entries={gapEntries} drivers={currentDrivers} />} />
          {/* <Route path="gapchart" element={<GapChartPage distances={driverDistances} drivers={currentDrivers} />} /> */}
          <Route path="telemetry" element={<TelemetryPage />} />
          <Route path="trackmap" element={<TrackMapPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};