import React, { useState, useEffect } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";
import { ConnectRequest, LiveTelemetryEvent } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import LapLogPage from "./laplog/LapLogPage";
import OtherCarsLapLogPage from "./laplog/OtherCarsLapLogPage";
import GapsPage from "./gaps/GapsPage";
import GapChartPage from "./gapchart/GapChartPage";
import App from "./App";

export default function App2() {
  const [gapEntries, setGapEntries] = useState([]);
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient(`${location.origin}/api`);
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
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient(`${location.origin}/api`);
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

  const [otherCarLapEntries, setOtherCarLapEntries] = useState([]);
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient(`${location.origin}/api`);
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorOtherCarsLaps(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      entries.unshift(response);
      setOtherCarLapEntries([...entries]);
    });
    stream.on('status', status => {
      console.log(status);
    });
    stream.on('end', end => {
      console.log('Stream end');
    });
  }, []);

  const [currentDrivers, setCurrentDrivers] = useState(new Map());
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient(`${location.origin}/api`);
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorCurrentDrivers(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      console.log('response: ', response);
      const entries = new Map(
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
      setCurrentDrivers(entries);
    });
    stream.on('status', status => {
      console.log(status);
    });
    stream.on('end', end => {
      console.log('Stream end');
    });
  }, []);

  const [driverDistances, setDriverDistances] = useState(new Map());
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient(`${location.origin}/api`);
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorDriverDistances(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      const distances = response.getDistancesList();

//       const relativeDistance = Math.max(...distances.map((distance) => distance.getDriverDistance()));
      const relativeDistance = distances[0].getDriverDistance();

      const carDistances = distances.map((distance) => {
        return {
          'carIdx': distance.getCarId(),
          'gapToLeader': distance.getDriverDistance() - relativeDistance
        }
      });
      entries.push({
        'sessionTime': response.getSessionTime(),
        'distances': carDistances
      })

      setDriverDistances([...entries]);
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
          <Route path="otherlaps" element={ OtherCarsLapLogPage(otherCarLapEntries, currentDrivers) } />
          <Route path="gaps" element={ GapsPage(gapEntries, currentDrivers) } />
          <Route path="gapchart" element={ GapChartPage(driverDistances, currentDrivers) } />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};