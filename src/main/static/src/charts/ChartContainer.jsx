import { ChartSyncContext } from './ChartSyncContext';

export const ChartContainer = ({ data, children, scales, setScales, dataRange }) => {
  return (
    <ChartSyncContext.Provider value={{ scales, setScales, dataRange }}>
      {children}
    </ChartSyncContext.Provider>
  );
};
