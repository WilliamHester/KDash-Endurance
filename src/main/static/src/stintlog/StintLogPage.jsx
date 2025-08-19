import React from "react";
import "./StintLogPage.css";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";

export default function StintLogPage({entries}) {
  const fastestLap = Math.min(...entries.map((entry) => entry.getFastestLapTime()));
  const lapRows = entries.map(
      (stintEntry) => {
        var lapTimeClassName = '';
        if (stintEntry.getFastestLapTime() === fastestLap) {
          lapTimeClassName = 'purple';
        } else {
//           console.log(lapEntry.getLapTime(), fastestLap);
        }
        return <tr key={ stintEntry.getOutLap() }>
          <td className="number">{ stintEntry.getOutLap() }</td>
          <td className="number">{ stintEntry.getInLap() }</td>
          <td>{ formatDriverName(stintEntry.getDriverName()) }</td>
          <td className="number">{ formatNumberAsDuration(stintEntry.getTotalTime()) }</td>
          <td className="number">{ formatNumberAsDuration(stintEntry.getAverageLapTime()) }</td>
          <td className={`number ${lapTimeClassName}`}>{ formatNumberAsDuration(stintEntry.getFastestLapTime()) }</td>
          <td className="number">{ stintEntry.getTrackTemp() }</td>
          <td className="number">{ stintEntry.getIncidents() }</td>
        </tr>
      });

    return (
      <table className="lapLogTable">
        <thead>
          <tr>
            <th>Out Lap</th>
            <th>In Lap</th>
            <th>Driver Name</th>
            <th>Total Time</th>
            <th>Average Lap Time</th>
            <th>Fastest Lap Time</th>
            <th>Track Temp</th>
            <th>Incidents</th>
          </tr>
        </thead>
        <tbody>
          {lapRows}
        </tbody>
      </table>
    );
};