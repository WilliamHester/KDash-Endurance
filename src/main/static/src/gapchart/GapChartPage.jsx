import React from "react";
import "./GapChartPage.css";
import Chart2 from "../charts/Chart2";

export default function GapChartPage(props) {
  return new Chart2('Driver Gaps', props.distances, props.drivers);
}
