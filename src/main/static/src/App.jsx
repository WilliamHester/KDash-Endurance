import React, { useEffect, useState } from "react";
import { Outlet } from "react-router";
import { Link, useNavigate } from "react-router-dom";
import { ConnectRequest, LiveTelemetryEvent } from "./live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "./live_telemetry_service_grpc_web_pb.js";
import "./base.css";
import { formatNumberAsDuration } from "./utils.js";
import PitChip from "./PitChip";

function App() {
  const [lapEntries, setLapEntries] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    const liveTelemetryServiceClient = new LiveTelemetryServiceClient('http://localhost:8000/api');
    const request = new ConnectRequest();
    const entries = [];
    const stream = liveTelemetryServiceClient.monitorDriverLaps(request, {}, (err, resp) => {
        console.log("went here");
        console.log(err, resp);
    });
    stream.on('data', response => {
      console.log('response: ', response);
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

  const lapRows = lapEntries.map(
    (lapEntry) => {
      return <tr>
        <td className="number">{ lapEntry.getLapNum() }</td>
        <td>{ lapEntry.getDriverName() }</td>
        <td className="number">{ lapEntry.getPosition() }</td>
        <td className="number">{ formatNumberAsDuration(lapEntry.getLapTime()) }</td>
        <td className="number">{ formatNumberAsDuration(lapEntry.getGapToLeader(), true) }</td>
        <td className="number">{ lapEntry.getFuelRemaining().toFixed(2) }</td>
        <td className="number">{ lapEntry.getFuelUsed().toFixed(3) }</td>
        <td className="number">{ lapEntry.getTrackTemp().toFixed(1) }</td>
        <td>{ lapEntry.getDriverIncidents() } / { lapEntry.getTeamIncidents() }</td>
        <td className="number">{ formatNumberAsDuration(lapEntry.getOptionalRepairsRemaining()) } / { formatNumberAsDuration(lapEntry.getRepairsRemaining()) }</td>
        <td className="number">{ PitChip(lapEntry) }</td>
      </tr>
    });

  console.log(lapRows);

  return (
    <div className="column">
      <header className="header">
        <div className="wrapper site-header__wrapper">
          <Link className="no-style-link" to="/">
            <span className="website-name">Stintlytics</span>
          </Link>
        </div>

        <nav className="nav-right">
          <Link className="no-style-link" to="/upload">
            <span className="upload">+</span>
          </Link>
        </nav>
      </header>

      <table>
        <thead>
          <tr>
            <th>Lap</th>
            <th>Driver</th>
            <th>Position</th>
            <th>Lap Time</th>
            <th>Gap to leader</th>
            <th>Fuel remaining</th>
            <th>Fuel used</th>
            <th>Track temp</th>
            <th>Incidents</th>
            <th>Repairs (optional/required)</th>
            <th>Pit</th>
          </tr>
        </thead>
        <tbody>
          {lapRows}
        </tbody>
      </table>
      { lapEntries.length }

      <Outlet />
    </div>
  );
};

export default App;