import React, { useState, useEffect, useRef, useMemo } from "react";
import {
  Routes,
  Route,
  useParams,
} from "react-router-dom";
import { ConnectRequest, Session } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import LapLogPage from "./laplog/LapLogPage.jsx";
import OtherCarsLapLogPage from "./laplog/OtherCarsLapLogPage.jsx";
import GapsPage from "./gaps/GapsPage.jsx";
import TelemetryPage from "./telemetry/TelemetryPage.jsx";
import TrackMapPage from "./trackmap/TrackMapPage.jsx";
import App from "./App.jsx";
import RaceOverviewPage from "./overview/RaceOverviewPage.jsx";

export default function SessionPage() {
  const params = useParams();

  const session = useMemo(() => {
    const s = new Session();
    s.setSessionId(parseInt(params.sessionId));
    s.setSubSessionId(parseInt(params.subSessionId));
    s.setSimSessionNumber(parseInt(params.simSessionNumber));
    s.setCarNumber(params.carNumber);
    return s;
  }, [params.sessionId, params.subSessionId, params.simSessionNumber, params.carNumber]);

  const [gapEntries, setGapEntries] = useState([]);
  const [lapEntries, setLapEntries] = useState([]);
  const [stintEntries, setStintEntries] = useState([]);
  const [otherCarLapEntries, setOtherCarLapEntries] = useState([]);
  const [otherCarStintEntries, setOtherCarStintEntries] = useState([]);
  const [currentDrivers, setCurrentDrivers] = useState(new Map());

  const client = useRef(new LiveTelemetryServiceClient(`${location.origin}/api`)).current;

  const gapBuffer = useRef([]);
  const lapBuffer = useRef([]);
  const stintEntryBuffer = useRef([]);
  const otherCarLapBuffer = useRef([]);
  const otherCarStintBuffer = useRef([]);

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
      if (response.hasOtherCarStint()) {
        otherCarStintBuffer.current.push(response.getOtherCarStint());
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
      if (gapBuffer.current.length > 0) {
        const lastMessage = gapBuffer.current[gapBuffer.current.length - 1];
        if (lastMessage && lastMessage.getGapsList) {
          setGapEntries(lastMessage.getGapsList());
        }
        gapBuffer.current = [];
      }

      if (lapBuffer.current.length > 0) {
        setLapEntries(prevEntries => {
          const updated = [...lapBuffer.current, ...prevEntries];
          lapBuffer.current = [];
          return updated;
        });
      }

      if (stintEntryBuffer.current.length > 0) {
        setStintEntries(prevEntries => {
          const updated = [...stintEntryBuffer.current, ...prevEntries];
          stintEntryBuffer.current = [];
          console.log(updated);
          return updated;
        });
      }

      if (otherCarLapBuffer.current.length > 0) {
        setOtherCarLapEntries(prevEntries => {
          const updated = [...otherCarLapBuffer.current, ...prevEntries];
          otherCarLapBuffer.current = [];
          return updated;
        });
      }

      if (otherCarStintBuffer.current.length > 0) {
        setOtherCarStintEntries(prevEntries => {
          const updated = [...otherCarStintBuffer.current, ...prevEntries];
          otherCarStintBuffer.current = [];
          return updated;
        });
      }
    }, 250);

    return () => clearInterval(intervalId); // Cleanup interval on unmount
  }, []);

  return (
    <App>
      <Routes>
        <Route index element={
          <RaceOverviewPage
            session={session} 
            drivers={currentDrivers} 
            lapLog={lapEntries} 
            otherCarLapEntries={otherCarLapEntries} 
            stintLog={stintEntries}
            otherCarStintEntries={otherCarStintEntries}
            />
        } />
        <Route path="laps" element={<LapLogPage entries={lapEntries} />} />
        <Route path="otherlaps" element={<OtherCarsLapLogPage entries={otherCarLapEntries} drivers={currentDrivers} />} />
        <Route path="gaps" element={<GapsPage entries={gapEntries} drivers={currentDrivers} />} />
        {/* <Route path="gapchart" element={<GapChartPage distances={driverDistances} drivers={currentDrivers} />} /> */}
        <Route path="telemetry" element={<TelemetryPage session={session} />} />
        <Route path="trackmap" element={<TrackMapPage />} />
      </Routes>
    </App>
  );
};