import React from "react";
import "./LapLogPage.css";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";
import PitChip from "./PitChip";

export default function LapLogPage(lapEntries) {
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