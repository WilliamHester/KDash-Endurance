import React, { useState } from "react";
import "./LapLogPage.css";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";
import PitChip from "./PitChip";
import Select from 'react-select'

export default function OtherCarsLapLogPage(lapEntries, drivers) {
  const options = [];
  drivers.forEach((value, carId) => {
    options.push(
      {
        value: carId,
        label: `#${value.carNumber}: ${value.teamName}`
      }
    );
  });
  const [selectedDriver, setSelectedDriver] = useState(0);
  const fastestLap = Math.min(...lapEntries.map((entry) => entry.getLapTime()));
  const lapRows = lapEntries
    .filter((entry) => entry.getCarId() === selectedDriver)
    .map(
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
          <td className="number">{ lapEntry.getTrackTemp().toFixed(1) }</td>
          <td className="number">{ PitChip(lapEntry) }</td>
        </tr>
      });

    function setValue(value) {
      setSelectedDriver(value.value);
    };

    return (
      <div style={ {display: 'flex', flexDirection: 'column'} }>
        <Select
          options={options}
          defaultValue={selectedDriver}
          onChange={setValue}
          theme={(theme) => ({
                ...theme,
                borderRadius: 0,
                colors: {
                  ...theme.colors,
                  primary: 'black',
                },
              })}
          />
        <table className="lapLogTable">
          <thead>
            <tr>
              <th>Lap</th>
              <th>Driver</th>
              <th>Pos</th>
              <th>Lap Time</th>
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