import React from "react";
import "./OtherCarStintLogPage.css";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";

export default function OtherCarStintLogPage({entries}) {
  const fastestLap = Math.min(...entries.filter(entry => entry.getFastestLapTime() > 0).map((entry) => entry.getFastestLapTime()));
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
          <td className="number">{ stintEntry.getTrackTemp().toFixed(1) }</td>
        </tr>
      });

    return (
      <table className="lapLogTable">
        <thead>
          <tr>
            <th>Out</th>
            <th>In</th>
            <th>Driver</th>
            <th>Total Time</th>
            <th>Avg Lap</th>
            <th>Fast Lap</th>
            <th>Track Temp</th>
          </tr>
        </thead>
        <tbody>
          {lapRows}
        </tbody>
      </table>
    );
};
