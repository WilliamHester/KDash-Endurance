import React from "react";
import "./GapChartPage.css";
import Chart from "../charts/Chart";
import uPlot from "uplot";
import "uplot/dist/uPlot.min.css";

import UPlotReact from "uplot-react";

export default function GapChartPage(driverDistances, drivers) {
  const colors = [
    '#f0f8ff',
    '#00ffff',
    '#7fffd4',
    '#ffe4c4',
    '#0000ff',
    '#8a2be2',
    '#a52a2a',
    '#5f9ea0',
    '#7fff00',
    '#d2691e',
    '#ff7f50',
    '#6495ed',
    '#dc143c',
    '#00ffff',
    '#00008b',
    '#008b8b',
    '#b8860b',
    '#006400',
    '#8b008b',
    '#556b2f',
    '#ff8c00',
    '#9932cc',
    '#8b0000',
    '#e9967a',
    '#8fbc8f',
    '#483d8b',
    '#2f4f4f',
    '#2f4f4f',
    '#ff1493',
    '#00bfff',
    '#1e90ff',
    '#b22222',
    '#228b22',
    '#ff00ff',
    '#ffd700',
    '#008000',
    '#adff2f',
    '#ff69b4',
    '#cd5c5c',
    '#4b0082',
    '#f0e68c',
    '#7cfc00',
    '#7cfc00',
    '#f08080',
    '#e0ffff',
    '#90ee90',
    '#ffb6c1',
    '#ffa07a',
    '#20b2aa',
    '#87cefa',
    '#00ff00',
    '#32cd32',
    '#ff00ff',
    '#800000',
    '#66cdaa',
    '#ba55d3',
    '#9370db',
    '#3cb371',
    '#7b68ee',
    '#00fa9a',
    '#48d1cc',
    '#c71585'
  ]
// console.log(drivers);
// console.log(drivers.values());
  const driverLabels = drivers.values().map((driver, idx) => {
    return {
      label: driver.driverName,
      points: { show: false },
      stroke: colors[idx],
    }});
  const options = {
    title: "Driver Gaps",
    width: 800,
    height: 300,
    cursor: {
      drag: {
        x: true,
        y: false,
        setScale: false, // Disable uPlot's default drag-to-zoom
      }
    },
    axes: [
      {
        stroke: "white"
      },
      {
        stroke: "white"
      },
    ],
    series: [
      {
        label: "Date"
      },
      ...driverLabels,
    ],
    plugins: [
//       uplotDragPanZoom()
    ],
    scales: { x: { time: true } }
  };
//   console.log(driverDistances);
  return (
    <UPlotReact
      key="hooks-key"
      options={options}
      data={driverDistances}
    />
  );
}
