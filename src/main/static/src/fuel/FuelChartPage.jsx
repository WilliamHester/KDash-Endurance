import React from "react";
import "./FuelChartPage.css";
import Chart2 from "../charts/Chart2";

export default function FuelChartPage(driverDistances, drivers) {
  return new Chart2(driverDistances, drivers);
}
