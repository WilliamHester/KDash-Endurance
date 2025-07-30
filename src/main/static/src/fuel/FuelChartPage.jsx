import React from "react";
import PropTypes from "prop-types";
import "./FuelChartPage.css";
import Chart2 from "../charts/Chart2";
import { ChartContainer } from "../charts/ChartContainer";

FuelChartPage.propTypes = {
  telemetryData: PropTypes.array.isRequired,
  dataRange: PropTypes.object.isRequired,
  dataWindow: PropTypes.array.isRequired,
  setDataWindow: PropTypes.func.isRequired,
};

export default function FuelChartPage({telemetryData, dataRange, dataWindow, setDataWindow}) {
  const fuelLevels = telemetryData.map(data => data.getFuelLevel());
  const drivers = [{driverName: 'Hardcoded Driver Name'}];

  const xAxis = {
    sessionTimes: telemetryData.map(data => data.getSessionTime()),
    driverDistances: telemetryData.map(data => data.getDriverDistance()),
  };

  // const lines = [yAxis];

  // const usageData = [[], []];
  // for (let lastPos = xAxis.length - 1; lastPos > 0; lastPos--) {
  //   const lastX = xAxis[lastPos];
  //   const xMinus1 = lastX - 1;
  //   for (let i = lastPos - 1; i >= 0; i--) {
  //     if (xAxis[i] < xMinus1) {
  //       // We've found the data point that's 1 lap less than our current position.
  //       // We could interpolate here, but whatever
  //       usageData[0].unshift(lastX);
  //       usageData[1].unshift(yAxis[i] - yAxis[lastPos]);
  //       break;
  //     }
  //   }
  // }

  return (
    <ChartContainer dataRange={dataRange} dataWindow={dataWindow} setDataWindow={setDataWindow} >
      <Chart2 title={'Fuel remaining'} xAxis={xAxis} lines={[fuelLevels]} drivers={drivers}></Chart2>
      {/* <Chart2 title={'Lap over Lap Fuel Usage'}  xAxis={xAxis} lines={lines} drivers={drivers}></Chart2> */}
    </ChartContainer>
  )
}
