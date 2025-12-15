<script>
  import { onMount, onDestroy } from 'svelte';
  import { writable, get } from 'svelte/store';
  import { page } from '$app/stores';
  import { LiveTelemetryServiceDefinition } from '$lib/grpc/live_telemetry_service';
  import { createChannel, createClient } from 'nice-grpc-web';
  import { ClientError } from 'nice-grpc-common';

  import TelemetryChart from '$lib/charts/TelemetryChart.svelte';

  // -- State --

  // 1. Zoom/Pan Window (Shared Store)
  // Initialize with [0, 300] seconds or similar
  const dataWindow = writable([0, 300]);

  // 2. Data Storage
  // We keep a buffer of objects like React did, but we also need transposed arrays for uPlot
  // Structure: { "Speed": [y1, y2...], "Fuel": [y1, y2...], "SessionTime": [x1, x2...] }
  let chartsData = {};
  let sessionTimes = []; // X-Axis

  // 3. Connection Config
  let sampleRateHz = 10;
  let abortController;
  let client;
  let debounceTimer;

  const QUERIES = [
    'DECREASING_SUM(FuelLevel, 1)',
    'FuelLevel',
    'LAP_DELTA(FuelLevel)',
    'TrackTempCrew',
    'Speed',
    'LAP_DELTA(Speed)'
  ];

  // Helper to transpose data for uPlot [[x...], [y...]]
  function updateCharts(newDataPoint) {
    const time = newDataPoint.sessionTime;
    const values = newDataPoint.queryValues;

    // Append X axis
    sessionTimes.push(time);

    // Append Y axes
    QUERIES.forEach((q, idx) => {
      if (!chartsData[q]) chartsData[q] = [];
      chartsData[q].push(values[idx]);
    });

    // Limit buffer size (keep last 5000 points to prevent memory leak)
    if (sessionTimes.length > 5000) {
      sessionTimes.shift();
      QUERIES.forEach(q => chartsData[q].shift());
    }

    // Trigger Svelte reactivity
    sessionTimes = sessionTimes;
    chartsData = chartsData;
  }

  // -- Logic --

  async function connect(minTime, maxTime, hz) {
    if (!client) {
      return;
    }
    if (abortController) {
      abortController.abort();
    }
    abortController = null; // new AbortController();

    // Reset data on new connection (or keep and append? React cleared it)
    sessionTimes = [];
    chartsData = {};

    const { sessionId, subId, simId, carNum } = $page.params;

    const request = {
      sessionIdentifier: {
        sessionId: Number(sessionId),
        subSessionId: Number(subId),
        simSessionNumber: Number(simId),
        carNumber: carNum
      },
      sampleRateHz: hz,
      minSessionTime: minTime,
      maxSessionTime: maxTime,
      queries: QUERIES
    };

    console.log(`Fetching Telemetry: ${minTime.toFixed(1)}s - ${maxTime.toFixed(1)}s @ ${hz}Hz`);

    try {
      const stream = client.queryTelemetry(request); //, { signal: abortController.signal });
      for await (const response of stream) {
        if (response.data) {
          updateCharts(response.data);
        } else if (response.dataRanges) {
          const newDataWindow = response.dataRanges.sessionTime;
          const currentWindow = get(dataWindow);
          if (currentWindow[0] < newDataWindow.min) {
            dataWindow.set([newDataWindow.min, newDataWindow.max]);
          }
        }
      }
    } catch (e) {
      if (e instanceof ClientError && e.code === 'ABORTED') {
         // ignore, this is expected when the stream is canceled
      } else {
        console.error("Telemetry Stream Error", e);
      }
    }
  }

  // -- Reactive Adaptive Sampling --
  // Watch `dataWindow` changes. If the zoom level implies we need higher/lower res, reconnect.

  const ALLOWED_HZ = [0.5, 1, 3, 6, 12, 30, 60];

  $: {
    const [min, max] = $dataWindow;

    // Calculate desired resolution
    // If window is 10 seconds, we want high-res (60hz).
    // If window is 1 hour (3600s), we want low res.
    const duration = max - min;
    const goalHz = 1000 / duration; // Heuristic from your React code

    const newHz = ALLOWED_HZ.reduce((prev, curr) =>
      Math.abs(curr - goalHz) < Math.abs(prev - goalHz) ? curr : prev
    );

    // Only reconnect if Hz changes OR if we panned outside current loaded range significantly
    // (Simplified logic here: Reconnect if Hz changes)
    if (newHz !== sampleRateHz) {
      sampleRateHz = newHz;

      // Debounce the reconnection so we don't thrash while scrolling
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        console.log('Requesting new data [%s, %s], at %sHz', min, max, newHz);
        connect(min, max, newHz);
      }, 500);
    }
  }

  onMount(() => {
    const channel = createChannel(window.location.origin + '/api');
    client = createClient(LiveTelemetryServiceDefinition, channel);
    connect($dataWindow[0], $dataWindow[1], sampleRateHz);
  });

  onDestroy(() => {
    if (abortController) {
      abortController.abort();
    }
  });

</script>

<div class="page-container">
    <div class="controls">
        <span>Window: {$dataWindow[0].toFixed(1)}s - {$dataWindow[1].toFixed(1)}s</span>
        <span>Rate: {sampleRateHz} Hz</span>
    </div>

    {#each QUERIES as queryName}
        <TelemetryChart
                title={queryName}
                scaleStore={dataWindow}
                data={[
        sessionTimes,
        chartsData[queryName] || []
      ]}
        />
    {/each}
</div>

<style>
    .page-container {
        padding: 20px;
        background-color: #000;
        min-height: 100vh;
        color: #fff;
    }
    .controls {
        margin-bottom: 10px;
        font-family: monospace;
        color: #888;
    }
</style>
