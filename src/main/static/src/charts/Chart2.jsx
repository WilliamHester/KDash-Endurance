import { useContext, useEffect, useRef, useState } from "react";
import "uplot/dist/uPlot.min.css";
import UPlotReact from "uplot-react";
import { ChartSyncContext } from './ChartSyncContext';

export default function Chart2({title, data, drivers}) {
  const { scales, setScales, dataRange } = useContext(ChartSyncContext);

  const minX = dataRange.min;
  let maxX;
  if (data.length > 0 && data[0].length > 0) {
    maxX = Math.max(dataRange.max, data[0][data[0].length - 1]);
  } else {
    maxX = dataRange.max;
  }

  const [dimensions, setDimensions] = useState({
    width: window.innerWidth,
    height: window.innerHeight,
  });
  useEffect(() => {
    function handleResize() {
      setDimensions({
        width: window.innerWidth,
        height: window.innerHeight,
      });
    }

    window.addEventListener('resize', handleResize);

    // Cleanup function
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []); // Empty dependency array ensures this effect runs only once on mount

  const parentRef = useRef(null);
  const [parentWidth, setParentWidth] = useState(0);

  useEffect(() => {
    if (parentRef.current) {
      setParentWidth(parentRef.current.offsetWidth);
    }
  }, [dimensions]);

  const colors = [
    '#008AC5',
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
  const driverLabels = drivers.values().map((driver, idx) => {
    return {
      label: driver.driverName,
      points: { show: false },
      stroke: colors[idx],
      width: 1.5,
    }});
  function wheelZoomPlugin(opts) {
    let factor = opts.factor || 0.75;

    let xMin, xMax, xRange;

    return {
      hooks: {
        ready: u => {
          xMin = u.scales.x.min;
          xMax = u.scales.x.max;
          xRange = xMax - xMin;

          let over = u.over;
          let rect = over.getBoundingClientRect();

          // wheel drag pan
          over.addEventListener("mousedown", e => {
            if (e.button == 0) {
              e.preventDefault();

              let left0 = e.clientX;

              let scXMin0 = u.scales.x.min;
              let scXMax0 = u.scales.x.max;

              let xUnitsPerPx = u.posToVal(1, 'x') - u.posToVal(0, 'x');

              function onmove(e) {
                e.preventDefault();

                let left1 = e.clientX;

                let dx = xUnitsPerPx * (left1 - left0);

                let min, max;
                if (dx > 0) {
                  min = Math.max(scXMin0 - dx, minX);
                  max = min + xRange;
                } else {
                  max = Math.min(scXMax0 - dx, maxX);
                  min = Math.max(minX, max - xRange);
                }

                setScales(
                  {
                    x: {
                      min: min,
                      max: max,
                    }
                  }
                );
              }

              function onup(e) {
                document.removeEventListener("mousemove", onmove);
                document.removeEventListener("mouseup", onup);
              }

              document.addEventListener("mousemove", onmove);
              document.addEventListener("mouseup", onup);
            }
          });

          // wheel scroll zoom
          over.addEventListener("wheel", e => {
            e.preventDefault();

            let {left} = u.cursor;

            let leftPct = left / rect.width;
            let xVal = u.posToVal(left, "x");
            let oxRange = u.scales.x.max - u.scales.x.min;

            // 30
            let nxRange = Math.min(e.deltaY < 0 ? oxRange * factor : oxRange / factor, maxX - minX);
            let nxMin = xVal - leftPct * nxRange;
            let nxMax = nxMin + nxRange;

            // Adjust the next min and max in the case that we hit the ends of the ranges.
            if (nxMax > maxX) {
              // The next max value would be greater than the allowed max, so pin the max to the end...
              nxMax = maxX;
              // And set the min to be max minus the range.
              nxMin = nxMax - nxRange;
            } else if (nxMin < minX) {
              // The next min value would be greater than the allowed minimum, so pin the min to the end...
              nxMin = minX;
              // And set the max to be the min plus the range.
              nxMax = nxMin + nxRange;
            }

            u.batch(() => {
              setScales({
                x: {
                  min: nxMin,
                  max: nxMax,
                }
              });
            });
          });
        }
      },
    };
  }
  const options = {
    title: title,
    width: parentWidth,
    height: 300,
    cursor: {
      drag: {
        x: true,
        y: false,
        setScale: false, // Disable uPlot's default drag-to-zoom
      }
    },
    axes: [
      {
        stroke: "white",
        grid: {
          stroke: "rgba(255, 255, 255, 0.2)",
          width: 1,
        },
      },
      {
        stroke: "white",
        grid: {
          stroke: "rgba(255, 255, 255, 0.2)",
          width: 1,
        },
      },
    ],
    series: [
      {
        label: "Date"
      },
      ...driverLabels,
    ],
    plugins: [
      wheelZoomPlugin({factor: 0.95}),
    ],
    scales: scales,
    legend: {
      // TODO: find a better way to show the legend that doesn't take up a ton of space
      show: false,
    }
  };
  return (
    <div ref={parentRef}>
      <UPlotReact
        key="hooks-key"
        options={options}
        data={data}
      />
    </div>
  );
}
