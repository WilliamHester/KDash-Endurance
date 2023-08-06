import React, { useEffect, useState } from "react";
import { formatNumberAsDuration } from "../utils.js";
import "./PitChip.css";

export default function PitChip(lapEntry) {
  const inLap = lapEntry.getPitIn();
  const outLap = lapEntry.getPitOut();
  const pitTime = lapEntry.getPitTime();

  if (!inLap && !outLap && pitTime == 0) {
    return null;
  }

  var pitText = '';
  if (outLap && inLap) {
    pitText = 'OUT/IN';
  } else if (outLap) {
    pitText = 'OUT';
  } else if (inLap) {
    pitText = 'IN';
  }

  var pitTimeSpan;
  var pitTextClass;
  if (pitTime > 0) {
    pitTextClass = "inOut";
    pitTimeSpan = <span className="pitTime">{ formatNumberAsDuration(pitTime, true) }</span>;
  } else {
    pitTextClass = "pitChip";
    pitTimeSpan = null;
  }

  return (
    <div>
      <span className={pitTextClass}>{ pitText }</span>
      { pitTimeSpan }
    </div>
  );
}
