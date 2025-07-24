import React, { useState } from 'react';
import { ChartSyncContext } from './ChartSyncContext';

// Define the initial view or domain for the x-axis.
const INITIAL_DOMAIN = { x: [0, 100] };

export const ChartContainer = ({ data, children }) => {
  let minX, maxX;
  if (data.length > 0 && data[0].length > 0) {
    minX = data[0][0];
    maxX = data[0][data[0].length - 1];
  } else {
    minX = 0;
    maxX = 0;
  }
  const [scales, setScales] = useState({
    x: {
      min: minX,
      max: maxX,
    },
  });

  return (
    <ChartSyncContext.Provider value={{ scales, setScales }}>
      {children}
    </ChartSyncContext.Provider>
  );
};
