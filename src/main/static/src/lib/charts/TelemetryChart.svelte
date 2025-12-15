<script>
  import { onMount, onDestroy } from 'svelte';
  import uPlot from 'uplot';
  import 'uplot/dist/uPlot.min.css';

  export let title = '';
  export let data = [[], []]; // [ [x...], [y...] ]
  export let scaleStore; // A writable store [min, max] shared between charts
  export let height = 300;

  let chartDiv;
  let uplotInst;
  let resizeObserver;

  // -- 1. Configuration --
  // We recreate the options only when necessary, but uPlot handles simple updates well.
  const getOptions = (width) => ({
    title: title,
    width: width,
    height: height,
    cursor: {
      drag: { x: true, y: false, setScale: false },
      points: { show: false }
    },
    scales: {
      x: {
        time: false,
        // Bind the scale range to the svelte store logic
        range: (u, min, max) => $scaleStore
      }
    },
    series: [
      { label: "Session Time" }, // X-Axis
      { 
        label: "Value", 
        stroke: "#008AC5", // Cyan-ish blue
        width: 1.5,
        points: { show: false }
      }
    ],
    axes: [
      {
        stroke: "white",
        grid: { stroke: "rgba(255, 255, 255, 0.2)", width: 1 },
      },
      {
        stroke: "white",
        grid: { stroke: "rgba(255, 255, 255, 0.2)", width: 1 },
      }
    ],
    hooks: {
      // 2. Wheel Zoom Plugin (Ported from your React code)
      ready: [
        u => {
          let over = u.over;
          let rect = over.getBoundingClientRect();

          // Wheel Scroll Zoom
          over.addEventListener("wheel", e => {
            e.preventDefault();
            const factor = 0.95;
            let { left } = u.cursor;
            let leftPct = left / rect.width;
            let xVal = u.posToVal(left, "x");
            let xMin = u.scales.x.min;
            let xMax = u.scales.x.max;
            let xRange = xMax - xMin;
            
            // Calculate new range
            let nxRange = e.deltaY < 0 ? xRange * factor : xRange / factor;
            let nxMin = xVal - leftPct * nxRange;
            let nxMax = nxMin + nxRange;

            // Update the shared store (triggers reactivity in Parent and other charts)
            console.log('Setting new min and max: ', nxMin, nxMax);
            scaleStore.set([nxMin, nxMax]);
          });

          // Drag Pan logic handled by uPlot cursor naturally, 
          // but if you implemented custom drag in React, standard uPlot drag usually
          // sets the scale. To sync it, we hook into setSelect or setScale.
          // For now, let's stick to wheel zoom for simplicity unless you need specific drag behavior.
        }
      ],
      // Sync cursor logic: When this chart sets its cursor, we could update others.
      // uPlot has a heavy-duty sync plugin, but for basic shared zooming,
      // the shared store driving the x-range is often enough.
    }
  });

  // -- 3. Reactivity --
  
  // When data changes, push to uPlot immediately (fast)
  $: if (uplotInst && data) {
    uplotInst.setData(data);
  }

  // When the shared zoom window changes, tell uPlot to redraw scales
  $: if (uplotInst && $scaleStore) {
    // Force redraw of x-axis
    uplotInst.redraw(true, true);
  }

  // -- 4. Lifecycle --

  onMount(() => {
    // Initial draw
    uplotInst = new uPlot(getOptions(chartDiv.clientWidth), data, chartDiv);

    // Handle Resize
    resizeObserver = new ResizeObserver(entries => {
      for (let entry of entries) {
        if (uplotInst) {
          uplotInst.setSize({ width: entry.contentRect.width, height: height });
        }
      }
    });
    resizeObserver.observe(chartDiv);
  });

  onDestroy(() => {
    if (uplotInst) uplotInst.destroy();
    if (resizeObserver) resizeObserver.disconnect();
  });
</script>

<div bind:this={chartDiv} class="chart-container"></div>

<style>
  .chart-container {
    width: 100%;
    background-color: #121212; /* Dark background for charts */
    margin-bottom: 10px;
    border: 1px solid #333;
  }
</style>

