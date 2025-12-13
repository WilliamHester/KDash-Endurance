import React from "react";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";

export default function GapsPage({drivers, gaps, distances, staticSessionInfo}) {
  if (staticSessionInfo == null || gaps.length === 0) {
    return <div></div>;
  }
  const driverIdx = staticSessionInfo.getDriverCarIdx();
  const indexedGapsAndDistances = gaps.map((gap, index) => [index, gap, distances[index]]);
  const driverRow = indexedGapsAndDistances[driverIdx];
  const halfLapTime = staticSessionInfo.getDriverCarEstLapTime() / 2;
  const gapsAndDistancesLapBehind =
    indexedGapsAndDistances.filter(
      (row) =>
        // Other car is ahead of the current driver, and it's greater than half a lap ahead
        (row[1] > driverRow[1] && row[1] - driverRow[1] >= halfLapTime))
      .map((row) => [row[0], row[1] - staticSessionInfo.getDriverCarEstLapTime() - driverRow[1], row[2]])
      .sort((a, b) => b[1] - a[1]);
  const gapsAndDistancesAhead =
    indexedGapsAndDistances.filter(
      (row) =>
        // Other car is ahead of driver car within the same lap, and the other car is less than half a lap ahead
        (row[1] > driverRow[1] && (row[1] - driverRow[1]) < halfLapTime))
      .map((row) => [row[0], driverRow[1] - row[1], row[2]])
      .sort((a, b) => b[1] - a[1]);
  const gapsAndDistancesBehind =
    indexedGapsAndDistances.filter(
      (row) =>
        // Other car is behind the driver within the same lap, and it's less than half a lap behind
        (row[1] < driverRow[1] && (driverRow[1] - row[1]) < halfLapTime))
      .map((row) => [row[0], driverRow[1] - row[1], row[2]])
      .sort((a, b) => a[1] - b[1]);
  const gapsAndDistancesLapAhead =
    // Other car behind the driver car but more than half a lap behind
    indexedGapsAndDistances.filter((row) => row[1] < driverRow[1] && driverRow[1] - row[1] >= halfLapTime)
      .map((row) => [row[0], row[1] + staticSessionInfo.getDriverCarEstLapTime() - driverRow[1], row[2]])
      .sort((a, b) => a[1] - b[1]);
  const gaps2 = gapsAndDistancesLapBehind
    .concat(gapsAndDistancesAhead)
    .concat([[driverRow[0], 0, driverRow[2]]])
    .concat(gapsAndDistancesBehind)
    .concat(gapsAndDistancesLapAhead);
  const gapRows = gaps2
    .filter((gap) => drivers.has(gap[0]))
    .map((gap) => (
      <tr key={gap[0]}>
        <td><div></div></td>
        <td>{ gap[0] + 1 }</td>
        <td>#{ drivers.get(gap[0]).carNumber }</td>
        <td>{ formatDriverName(drivers.get(gap[0]).driverName) }</td>
        <td>{ formatNumberAsDuration(gap[1], true, true) }</td>
      </tr>
    ));

  // const gapRows = <div></div>
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
