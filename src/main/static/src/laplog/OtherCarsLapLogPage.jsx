import React from "react";
import "./LapLogPage.css";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";
import PitChip from "./PitChip";

export default function OtherCarsLapLogPage(lapEntries) {
  const fastestLap = Math.min(...lapEntries.map((entry) => entry.getLapTime()));
  const lapRows = lapEntries.map(
      (lapEntry) => {
        var lapTimeClassName = '';
        if (lapEntry.getLapTime() === fastestLap) {
          lapTimeClassName = 'purple';
        } else {
//           console.log(lapEntry.getLapTime(), fastestLap);
        }
        return <tr>
          <td className="number">{ lapEntry.getLapNum() }</td>
          <td>{ formatDriverName(lapEntry.getDriverName()) }</td>
          <td className="number">{ lapEntry.getPosition() }</td>
          <td className={`number ${lapTimeClassName}`}>{ formatNumberAsDuration(lapEntry.getLapTime()) }</td>
          <td className="number">{ formatNumberAsDuration(lapEntry.getGapToLeader(), true) }</td>
          <td className="number">{ lapEntry.getTrackTemp().toFixed(1) }</td>
          <td className="number">{ PitChip(lapEntry) }</td>
        </tr>
      });

    return (
      <div>
        <table className="lapLogTable">
          <thead>
            <tr>
              <th>Lap</th>
              <th>Driver</th>
              <th>Pos</th>
              <th>Lap Time</th>
              <th>Gap to leader</th>
              <th>Track temp</th>
              <th>Pit</th>
            </tr>
          </thead>
          <tbody>
            {lapRows}
          </tbody>
        </table>
      </div>
    );
};