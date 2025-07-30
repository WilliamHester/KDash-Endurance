import React from "react";
import { ChartSyncContext } from './ChartSyncContext';

export const ChartContainer = ({ children, dataRange, dataWindow, setDataWindow }) => {
  return (
    <ChartSyncContext.Provider value={{ dataRange, dataWindow, setDataWindow }}>
      {children}
    </ChartSyncContext.Provider>
  );
};
