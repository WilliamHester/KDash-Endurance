import React from "react";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";
import "./GapsPage.css";

export default function GapsPage(gapEntries, drivers) {
  const gapRows = gapEntries
    .filter((gap) => drivers.has(gap.getCarId()))
    .map((gap, index) => (
      <tr>
        <td><div></div></td>
        <td>{ index + 1 }</td>
        <td>#{ drivers.get(gap.getCarId()).carNumber }</td>
        <td>{ formatDriverName(drivers.get(gap.getCarId()).driverName) }</td>
        <td>{ formatNumberAsDuration(gap.getGap(), true, true) }</td>
      </tr>
    ));
  return (
    <div className="centered-content-column">
      <table className="gapsTable">
        <thead>
          <tr>
            <td></td>
            <td>Pos</td>
            <td>Num</td>
            <td>Driver</td>
            <td>Gap</td>
          </tr>
        </thead>
        <tbody>
          { gapRows }
        </tbody>
      </table>
    </div>
  );
}
