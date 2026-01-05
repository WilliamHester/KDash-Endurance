<script>
  import {
    driversList,
    sessionInfo,
    staticSessionInfo,
    telemetry,
  } from "$lib/stores/session.js";

  const positions = $derived($telemetry['CarIdxClassPosition'] || []);

  const gapsList = $derived.by(() => {
    const gaps = $telemetry['CarIdxDriverCarClassEstTime'] || [];
    const driverIdx = $staticSessionInfo.driverCarIdx;
    const driverGap = gaps[driverIdx];

    if (driverGap == null) {
      return [];
    }

    const lapTime = $sessionInfo.driverCarEstLapTime;
    const halfLapTime = lapTime / 2;
    const numDrivers = $driversList.length;
    const calculatedGaps = [];

    for (let i = 0; i < numDrivers; i++) {
      const gap = gaps[i];
      if (gap == null) continue;

      let diff = gap - driverGap;

      if (diff > halfLapTime) {
        diff -= lapTime;
      } else if (diff < -halfLapTime) {
        diff += lapTime;
      }

      calculatedGaps.push([i, diff]);
    }

    const sortedGaps = calculatedGaps.sort((a, b) => b[1] - a[1]);
    const driverRank = sortedGaps.findIndex((gap) => gap[0] === driverIdx);
    const padding = $driversList.length;
    const topPaddingCount = Math.max(0, padding - driverRank);
    const bottomPaddingCount = Math.max(0, padding - (sortedGaps.length - 1 - driverRank));

    const topPadding = Array.from({ length: topPaddingCount }, (_, i) => [-1 - i, null]);
    const bottomPadding = Array.from({ length: bottomPaddingCount }, (_, i) => [-1000 - i, null]);

    return [...topPadding, ...sortedGaps, ...bottomPadding];
  });
</script>

<div class="table-container">
  <table class="table-fixed">
    <thead>
      <tr>
        <th>Pos</th>
        <th>Num</th>
        <th>Driver</th>
        <th>Gap</th>
      </tr>
    </thead>
    <tbody>
    {#each gapsList as gap (gap[0])}
      <tr id={gap[0] === $staticSessionInfo.driverCarIdx ? 'driver-row' : ''}>
        {#if gap[1] === null}
          <td colspan="4" style="height: 2rem;"></td>
        {:else}
          <td>
            { positions[gap[0]] }
          </td>
          <td>
            #{ $driversList[gap[0]].carNumber }
          </td>
          <td>
            { ($driversList[gap[0]] || {driverName: 'unknown'}).driverName || 'unknown' }
          </td>
          <td>
            { gap[1].toFixed(2) }
          </td>
        {/if}
      </tr>
    {/each}
    </tbody>
  </table>
</div>

<style>
  .table-container {
    height: 22rem;
    overflow-y: auto;
  }
  thead {
    position: sticky;
    top: 0;
    background-color: #000;
    z-index: 1;
  }

  #driver-row {
    background-color: #404040;
  }
</style>
