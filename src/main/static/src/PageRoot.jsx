import React, { useState, useEffect } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";
import { ConnectRequest, LiveTelemetryEvent } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import LapLogPage from "./laplog/LapLogPage";
import GapsPage from "./gaps/GapsPage";
import App from "./App";

export default function App2() {
  const [gapEntries, setGapEntries] = useState([]);
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient('http://localhost:8000/api');
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorCurrentGaps(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      setGapEntries([...response.getGapsList()]);
    });
    stream.on('status', status => {
      console.log(status);
    });
    stream.on('end', end => {
      console.log('Stream end');
    });
  }, []);

  const [lapEntries, setLapEntries] = useState([]);
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient('http://localhost:8000/api');
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorDriverLaps(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      entries.unshift(response);
      setLapEntries([...entries]);
    });
    stream.on('status', status => {
      console.log(status);
    });
    stream.on('end', end => {
      console.log('Stream end');
    });
  }, []);

  const [currentDrivers, setCurrentDrivers] = useState({});
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient('http://localhost:8000/api');
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorCurrentDrivers(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      console.log('response: ', response);
      const entries = new Map(
        response.getDriversList().map((driver) => [driver.getCarId(), driver.getDriverName()])
      );
      setCurrentDrivers(entries);
    });
    stream.on('status', status => {
      console.log(status);
    });
    stream.on('end', end => {
      console.log('Stream end');
    });
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={ App() }>
          <Route path="" element={ LapLogPage(lapEntries) } />
          <Route path="laps" element={ LapLogPage(lapEntries) } />
          <Route path="gaps" element={ GapsPage(gapEntries, currentDrivers) } />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};