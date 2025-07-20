import React from "react";
import "./GapChartPage.css";
import Chart2 from "../charts/Chart2";

export default function GapChartPage(driverDistances, drivers) {
  return new Chart2('Driver Gaps', driverDistances, drivers);
}
