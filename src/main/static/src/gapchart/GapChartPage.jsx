import React, {useEffect, useMemo, useRef, useState} from "react";
import { formatNumberAsDuration, formatDriverName } from "../utils.js";
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import "./GapChartPage.css";
import debounce from "lodash";

export default function GapChartPage(driverDistances, drivers) {
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
  const chartContainerRef = useRef(null);
  const [xAxisDataPercent, setXAxisDataPercent] = useState(100);
  const [targetZoomPercent, setTargetZoomPercent] = useState(100);

  const computedDomain = useMemo(() => {
    console.log("Recomputing domain", xAxisDataPercent);
    let minMax = [0, 0];
    if (driverDistances.map !== undefined) {
      minMax = [
        Math.min(...driverDistances.map(d => d.sessionTime)),
        Math.max(...driverDistances.map(d => d.sessionTime)),
      ];
    }
    const min = minMax[0];
    const max = minMax[1];
    const range = max - min;
    const newMin = range - range * (xAxisDataPercent / 100.0) + min;

    return [newMin, max];
  }, [xAxisDataPercent, driverDistances]); // Dependencies for useMemo

  useEffect(() => {
    let animationId;

    const animate = () => {
      console.log("Entered animate");
      // Smoothly interpolate towards the target zoom percent
      const currentZoomPercent = xAxisDataPercent;
      const difference = targetZoomPercent - currentZoomPercent;
      const animationSpeed = 0.05; // Adjust for animation speed (0.0 - 1.0)
      const nextZoomPercent = currentZoomPercent + difference * animationSpeed;

      console.log("Previous percent/new percent", currentZoomPercent, nextZoomPercent);
      setXAxisDataPercent(nextZoomPercent);

      if (Math.abs(difference) < 0.1) { // Stop animation when close enough
        cancelAnimationFrame(animationId);
        animationId = null;
        setXAxisDataPercent(targetZoomPercent); // Snap to target to avoid floating point issues
      } else {
        animationId = requestAnimationFrame(animate); // Continue animation
      }
    };

    let handledWheel = false;

    const handleWheel = (e) => {
      console.log(e);
      e.preventDefault();
      if (handledWheel) {
        console.log("Ignoring wheel. Already handled.");
        return;
      }
      handledWheel = true;

      const delta = e.deltaY;
      const direction = delta > 0 ? 1 : -1;

      const zoomFactor = 1;

      const newZoomPercent = Math.max(1, Math.min(100, targetZoomPercent + (1 + zoomFactor) * direction));
      setTargetZoomPercent(newZoomPercent);

      // console.log("New zoom percent", newZoomPercent);

      if (!animationId) {
        console.log("Animating?");
        animationId = requestAnimationFrame(animate);
      }

      // setXAxisDataPercent(newZoomPercent);
    };

    const chartContainer = chartContainerRef.current;
    if (chartContainer) {
      console.log("Adding event listener");
      chartContainer.addEventListener('wheel', handleWheel, { passive: false });

      return () => {
        console.log("Removing event listener");
        chartContainer.removeEventListener('wheel', handleWheel);

        if (animationId) {
          cancelAnimationFrame(animationId); // Stop animation on unmount
        }
      };
    } else {
      console.log("Did not add event listener");
    }
  }, [targetZoomPercent]);

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
  return (
    <div ref={chartContainerRef} className="centered-content-column" style={{height: '100%', width: '100%'}}>
      <ResponsiveContainer width="100%" height={800}>
        <LineChart data={driverDistances}>
          <XAxis dataKey="sessionTime" type="number" domain={computedDomain} allowDataOverflow />
          <YAxis />
          <Tooltip formatter={tooltipFormatter} contentStyle={{backgroundColor: '#1a1a1a'}} />
          { lines }
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
