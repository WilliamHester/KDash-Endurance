import {useState} from "react";
import "./FuelChartPage.css";
import Chart2 from "../charts/Chart2";
import { ChartContainer } from "../charts/ChartContainer";

export default function FuelChartPage({telemetryData, dataRanges}) {
  const dataRange = dataRanges.driverDistance;
  const [dataWindow, setDataWindow] = useState([dataRange, dataRange]);

  const fuelLevels = telemetryData.map(data => data.getFuelLevel());
  const drivers = [{driverName: 'Hardcoded Driver Name'}];

  const xAxis = telemetryData.map(data => data.getDriverDistance());
  const yAxis = fuelLevels;

  const data = [xAxis, yAxis];

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

  console.log(dataRange);

  return (
    <ChartContainer dataRange={dataRange} dataWindow={dataWindow} setDataWindow={setDataWindow} >
      <Chart2 title={'Fuel remaining'} data={data} drivers={drivers}></Chart2>
      <Chart2 title={'Lap over Lap Fuel Usage'} data={usageData} drivers={drivers}></Chart2>
    </ChartContainer>
  )
}
