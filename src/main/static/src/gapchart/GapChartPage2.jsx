import React, {useEffect, useRef, useState} from "react";
import { debounce } from 'lodash';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import "./GapChartPage.css";

export default function GapChartPage(driverDistances, drivers) {
  const asdf = {
    driverDistances: driverDistances,
  };
  const getXValue = (index) => (distances) => {
    if (index >= distances.length) {
      return 0;
    }
    const distance = distances.distances[index];
    if (distance === undefined) {
      console.log('Distance undefined for index', index);
      return 0.0;
    }
    return distance.gapToLeader;
  };
  function tooltipFormatter(value, name, props) {
    return [value.toFixed(3), name];
  }
  const colors = [
    '#f0f8ff',
    '#00ffff',
    '#7fffd4',
    '#ffe4c4',
    '#0000ff',
    '#8a2be2',
    '#a52a2a',
    '#5f9ea0',
    '#7fff00',
    '#d2691e',
    '#ff7f50',
    '#6495ed',
    '#dc143c',
    '#00ffff',
    '#00008b',
    '#008b8b',
    '#b8860b',
    '#006400',
    '#8b008b',
    '#556b2f',
    '#ff8c00',
    '#9932cc',
    '#8b0000',
    '#e9967a',
    '#8fbc8f',
    '#483d8b',
    '#2f4f4f',
    '#2f4f4f',
    '#ff1493',
    '#00bfff',
    '#1e90ff',
    '#b22222',
    '#228b22',
    '#ff00ff',
    '#ffd700',
    '#008000',
    '#adff2f',
    '#ff69b4',
    '#cd5c5c',
    '#4b0082',
    '#f0e68c',
    '#7cfc00',
    '#7cfc00',
    '#f08080',
    '#e0ffff',
    '#90ee90',
    '#ffb6c1',
    '#ffa07a',
    '#20b2aa',
    '#87cefa',
    '#00ff00',
    '#32cd32',
    '#ff00ff',
    '#800000',
    '#66cdaa',
    '#ba55d3',
    '#9370db',
    '#3cb371',
    '#7b68ee',
    '#00fa9a',
    '#48d1cc',
    '#c71585'
  ]
  const lines = [];
  drivers.forEach((value, carId) => {
    lines.push(
      <Line
        type="monotone"
        name={ value.driverName }
        dataKey={ getXValue(carId) }
        stroke={colors[carId]}
        dot={false}
        isAnimationActive={false} />
    );
  });
  const chartContainerRef = useRef(null);
  const [xAxisDataPercent, setXAxisDataPercent] = useState(100);

  useEffect(() => {
    console.log("Entered useEffect");
    const handleWheel = (e) => {
      console.log(e);
      e.preventDefault();

      const delta = e.deltaY; // Negative for zoom in, positive for zoom out
      console.log(xAxisDataPercent, delta);

      const zoomFactor = 0.3; // Adjust zoom sensitivity

      // Delta ranges from something like 1 or 2 to maybe 10. This happens in small bursts, so it might sum to something
      // like 15? Let's call each number a percent.

      const newZoomPercent = Math.max(1, Math.min(100, xAxisDataPercent + zoomFactor * delta));

      // console.log("New zoom percent", newZoomPercent);

      setXAxisDataPercent(newZoomPercent);
    };
    const debouncedHandleWheel = debounce(handleWheel, 50);

    const chartContainer = chartContainerRef.current;
    if (chartContainer) {
      console.log("Adding event listener");
      chartContainer.addEventListener('wheel', debouncedHandleWheel, { passive: false });

      return () => {
        console.log("Removing event listener");
        chartContainer.removeEventListener('wheel', debouncedHandleWheel);
      };
    } else {
      console.log("Did not add event listener");
    }
  }, [xAxisDataPercent]);

  function computeDomain(minMax) {
    const min = Math.min(minMax[0], 0);
    const max = Math.max(minMax[1], 0);

    // console.log("Provided min and max", min, max);

    // For now, let's anchor the max to be the same, and only adjust the min. This will skew the chart to look at the
    // newest data only.
    const range = max - min;
    const newMin = Math.max(0, range - range * (xAxisDataPercent / 100.0));

    // console.log("New min and max", newMin, max);

    return [newMin, max];
  }

  return (
    <div ref={chartContainerRef} className="centered-content-column" style={{height: '100%', width: '100%'}}>
      <ResponsiveContainer width="100%" height={800}>
        <LineChart data={driverDistances}>
          <XAxis dataKey="sessionTime" type="number" domain={computeDomain} allowDataOverflow />
          <YAxis />
          <Tooltip formatter={tooltipFormatter} contentStyle={{backgroundColor: '#1a1a1a'}} />
          { lines }
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
