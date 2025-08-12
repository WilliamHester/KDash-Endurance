import React, { useState } from "react";
// import "./LapLogPage.css";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";
// import PitChip from "./PitChip";
import Select from 'react-select'
import Row from "../base/Row.jsx";

export default function OtherCarsLapLog({entries, drivers}) {
  const lapEntries = entries;
  const options = [];
  drivers.forEach((value, carId) => {
    options.push(
      {
        value: carId,
        label: `#${value.carNumber}: ${value.teamName}`
      }
    );
  });
  const [selectedDrivers, setSelectedDrivers] = useState([]);
  const fastestLap = Math.min(...lapEntries.map((entry) => entry.getLapTime()));

  function setValues(values) {
    setSelectedDrivers(values.map((value) => value.value));
  };

  const darkTheme = (theme) => ({
    ...theme,
    borderRadius: 4,
    colors: {
      ...theme.colors,
      primary: '#663399',
      primary75: '#7a4ca6',
      primary50: '#8f66b3',
      primary25: '#4d3366',
      
      danger: '#de350b',
      dangerLight: '#ffbdad',

      neutral0: '#2b2b2b',
      neutral5: '#363636',
      neutral10: '#4f4f4f',
      neutral20: '#5e5e5e',
      neutral30: '#757575',
      neutral40: '#a3a3a3',
      neutral50: '#a3a3a3',
      neutral60: '#cccccc',
      neutral80: '#f2f2f2',
      neutral90: '#ffffff',
    },
  });

  return (
    <div style={ {display: 'flex', flexDirection: 'column', width: '500px'} }>
      <Select
        options={options}
        defaultValue={selectedDrivers}
        isMulti
        onChange={setValues}
        theme={darkTheme}
        />
      <Row>
        {selectedDrivers.map((value) => {
          console.log(value);
          return (
            <table className="lapLogTable" key={value}>
              <thead>
                <tr>
                  <th>Lap</th>
                  <th>Driver</th>
                  <th>Lap Time</th>
                </tr>
              </thead>
              <tbody>
                {lapEntries
                  .filter((entry) => entry.getCarId() === value)
                  .map(
                    (lapEntry) => {
                      var lapTimeClassName = '';
                      if (lapEntry.getLapTime() === fastestLap) {
                        lapTimeClassName = 'purple';
                      }
                      return <tr key={ lapEntry.getLapNum() }>
                        <td className="number">{ lapEntry.getLapNum() }</td>
                        <td>{ formatDriverName(lapEntry.getDriverName()) }</td>
                        <td className={`number ${lapTimeClassName}`}>{ formatNumberAsDuration(lapEntry.getLapTime()) }</td>
                      </tr>
                    })}
              </tbody>
            </table>
          );
        })}
      </Row>
    </div>
  );
};