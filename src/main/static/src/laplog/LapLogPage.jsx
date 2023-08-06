import React, { useEffect, useState } from "react";
import { Outlet } from "react-router";
import { Link, useNavigate } from "react-router-dom";
import { ConnectRequest, LiveTelemetryEvent } from "../live_telemetry_service_pb.js";
import { LiveTelemetryServiceClient } from "../live_telemetry_service_grpc_web_pb.js";
import "./LapLogPage.css";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";
import PitChip from "./PitChip";

export default function LapLogPage() {
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
          <td>{ formatDriverName(lapEntry.getDriverName()) }</td>
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

    return (
      <table>
        <thead>
          <tr>
            <th>Lap</th>
            <th>Driver</th>
            <th>Pos</th>
            <th>Lap Time</th>
            <th>Gap to leader</th>
            <th>Fuel remaining</th>
            <th>Fuel used</th>
            <th>Track temp</th>
            <th>Incidents</th>
            <th>Repairs</th>
            <th>Pit</th>
          </tr>
        </thead>
        <tbody>
          {lapRows}
        </tbody>
      </table>
    );
};