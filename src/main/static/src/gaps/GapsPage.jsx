import React from "react";

export default function GapsPage(gapEntries, drivers) {
  const gapRows = gapEntries
    .filter((gap) => drivers.has(gap.getCarId()))
    .map((gap, index) => (
      <tr>
        <td><div></div></td>
        <td>{ index + 1 }</td>
        <td>{ drivers.get(gap.getCarId()) }</td>
        <td>{ gap.getGap().toFixed(3) }</td>
      </tr>
    ));
  return (
    <div>
      <table>
        <thead>
          <tr>
            <td></td>
            <td>Position</td>
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
