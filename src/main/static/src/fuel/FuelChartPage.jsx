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
  const chartsData = [];
  if (telemetryData.length > 0) {
    const numCharts = telemetryData[0].getQueryValuesList().length;
    for (let i = 0; i < numCharts; i++) {
      chartsData.push(telemetryData.map(data => data.getQueryValuesList()[i]));
    }
  }
  const drivers = [{driverName: 'Hardcoded Driver Name'}];

  const xAxis = {
    sessionTimes: telemetryData.map(data => data.getSessionTime()),
    driverDistances: telemetryData.map(data => data.getDriverDistance()),
  };

  return (
    <ChartContainer dataRange={dataRange} dataWindow={dataWindow} setDataWindow={setDataWindow} >
      {
        chartsData.map((chartData, index) => <Chart2 key={index} title={`Chart ${index}`} xAxis={xAxis} lines={[chartData]} drivers={drivers}></Chart2>)
      }
    </ChartContainer>
  )
}
