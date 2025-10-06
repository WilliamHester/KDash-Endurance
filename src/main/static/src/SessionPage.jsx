import React, { useState, useEffect, useRef, use } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";
import { ConnectRequest } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import LapLogPage from "./laplog/LapLogPage.jsx";
import OtherCarsLapLogPage from "./laplog/OtherCarsLapLogPage.jsx";
import GapsPage from "./gaps/GapsPage.jsx";
// import GapChartPage from "./gapchart/GapChartPage";
import TelemetryPage from "./telemetry/TelemetryPage.jsx";
import TrackMapPage from "./trackmap/TrackMapPage.jsx";
import App from "./App.jsx";
import RaceOverviewPage from "./overview/RaceOverviewPage.jsx";

export default function SessionPage({ session }) {
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

    // We can't have more than 6 simultaneous TCP connections to the same domain. This means that we need to combine
    // the streams into a single stream. Having 6 causes the browser to lock up when refreshing the page,
    // presumably because the page itself would be a 7th connection.
    // setupStream('monitorCurrentGaps', gapBuffer);
    
    const request = new ConnectRequest();
    request.setSessionIdentifier(session);
    const driverLapsStream = liveTelemetryServiceClient.monitorLaps(request, {});
    driverLapsStream.on('data', response => {
      if (response.hasDriverLap()) {
        lapBuffer.current.push(response.getDriverLap());
      }
      if (response.hasDriverStint()) {
        stintEntryBuffer.current.push(response.getDriverStint());
      }
      if (response.hasOtherCarLap()) {
        otherCarLapBuffer.current.push(response.getOtherCarLap());
      }
    });

    // This stream updates state directly because it's a single map, not a growing list.
    // The workload is minimal.
    const driversStream = liveTelemetryServiceClient.monitorCurrentDrivers(request, {});
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
  }, [client, session]);

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
          <Route path="" element={
            <RaceOverviewPage session={session} drivers={currentDrivers} lapLog={lapEntries} otherCarLapEntries={otherCarLapEntries} stintLog={stintEntries} />
          } />
          <Route path="laps" element={<LapLogPage entries={lapEntries} />} />
          <Route path="otherlaps" element={<OtherCarsLapLogPage entries={otherCarLapEntries} drivers={currentDrivers} />} />
          <Route path="gaps" element={<GapsPage entries={gapEntries} drivers={currentDrivers} />} />
          {/* <Route path="gapchart" element={<GapChartPage distances={driverDistances} drivers={currentDrivers} />} /> */}
          <Route path="telemetry" element={<TelemetryPage session={session} />} />
          <Route path="trackmap" element={<TrackMapPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};