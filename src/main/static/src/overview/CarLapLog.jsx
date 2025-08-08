import React from "react";
import "./CarLapLog.css";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";

export default function CarLapLog({entries}) {
  const fastestLap = Math.min(...entries.map((entry) => entry.getLapTime()));
  const lapRows = entries.map(
      (lapEntry) => {
        var lapTimeClassName = '';
        if (lapEntry.getLapTime() === fastestLap) {
          lapTimeClassName = 'purple';
        }
        return <tr key={ lapEntry.getLapNum() }>
          <td className="number">{ lapEntry.getLapNum() }</td>
          <td>{ formatDriverName(lapEntry.getDriverName()) }</td>
          <td className="number">{ lapEntry.getPosition() }</td>
          <td className={`number ${lapTimeClassName}`}>{ formatNumberAsDuration(lapEntry.getLapTime()) }</td>
          <td className="number">{ formatNumberAsDuration(lapEntry.getGapToLeader(), true) }</td>
        </tr>
      });

  return (
    <table className="carLapLog">
      <thead>
        <tr>
          <th>Lap</th>
          <th>Driver</th>
          <th>Pos</th>
          <th>Lap Time</th>
          <th>Gap to leader</th>
        </tr>
      </thead>
      <tbody>
        {lapRows}
      </tbody>
    </table>
  );
};