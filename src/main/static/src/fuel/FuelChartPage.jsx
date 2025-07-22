import React from "react";
import "./FuelChartPage.css";
import Chart2 from "../charts/Chart2";

export default function FuelChartPage(props) {
  const fuelLevels = props.telemetryData.map(data => data.getFuelLevel());
  const drivers = [{driverName: 'Hardcoded Driver Name'}];

  const xAxis = props.telemetryData.map(data => data.getDriverDistance());
  const yAxis = fuelLevels;

  const data = [xAxis, yAxis];

  const oneLapFuelUsage = [];
  const usageData = [[], []];
  for (let lastPos = xAxis.length - 1; lastPos > 0; lastPos--) {
    const lastX = xAxis[lastPos];
    const xMinus1 = lastX - 1;
    for (let i = lastPos - 1; i >= 0; i--) {
      if (xAxis[i] < xMinus1) {
        // We've found the data point that's 1 lap less than our current position.
        // We could interpolate here, but whatever
        usageData[0].unshift(lastX);
        usageData[1].unshift(yAxis[i] - yAxis[lastPos]);
        break;
      }
    }
  }

  return (
    <div>
      {Chart2('Fuel remaining', data, drivers)}
      {Chart2('Lap over Lap Fuel Usage', usageData, drivers)}
    </div>
  )
}
