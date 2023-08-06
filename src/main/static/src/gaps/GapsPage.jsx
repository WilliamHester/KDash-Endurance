import React from "react";

export default function GapsPage(gapEntries) {
  return (
    <div>
      { gapEntries.map((gap) => <div>{ gap.getGap().toFixed(3) }</div>)}
    </div>
  );
}
