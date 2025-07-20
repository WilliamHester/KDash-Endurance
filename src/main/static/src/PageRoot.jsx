import React, { useState, useEffect, useRef, useCallback } from "react";
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
import FuelChartPage from "./fuel/FuelChartPage";
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

  const [driverDistances, setDriverDistances] = useState([]);
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient(`${location.origin}/api`);
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorDriverDistances(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      const entry = [response.getSessionTime(), ...response.getDistancesList().map((distance) => distance.getDriverDistance())];
      if (entries.length === 0) {
        console.log("entry length", entry);
        entry.forEach((value) => entries.push([value]));
      } else {
        for (const [index, value] of entry.entries()) {
          entries[index].push(value);
        }
      }
      setDriverDistances([...entries]);
    });
    stream.on('status', status => {
      console.log(status);
    });
    stream.on('end', end => {
      console.log('Stream end');
    });
  }, []);

  function useBatchedState(initialState) {
    const [state, setState] = useState(initialState);
    const latestStateRef = useRef(initialState);
    const animationFrameId = useRef(null);

    const updateState = useCallback(() => {
      setState(latestStateRef.current);
      animationFrameId.current = requestAnimationFrame(updateState);
    }, []);

    useEffect(() => {
      animationFrameId.current = requestAnimationFrame(updateState);
      return () => cancelAnimationFrame(animationFrameId.current);
    }, [updateState]);

    const setBatchState = useCallback((newState) => {
      latestStateRef.current = newState;
    }, []);

    return [state, setBatchState];
  }

  const [fuelLevels, setFuelLevels] = useBatchedState([]);
  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient(`${location.origin}/api`);
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorFuelLevel(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      entries.push(response);
      setFuelLevels([...entries]);
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
          <Route path="fuelcharts" element={ FuelChartPage(fuelLevels) } />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};