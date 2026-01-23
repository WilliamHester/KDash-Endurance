<script>
  import {
    driversList,
    sessionInfo,
    staticSessionInfo,
    telemetry,
  } from "$lib/stores/session.js";
  import { calculateGaps } from "$lib/gaps.js";
  import DataBox from "$lib/components/DataBox.svelte";

  let { estTimes } = $props();

  const positions = $derived($telemetry['CarIdxClassPosition'] || []);

  const gapsList = $derived.by(() => {
    if (estTimes === undefined) {
      return [];
    }
    const driverIdx = $staticSessionInfo.driverCarIdx;
    const lapTime = $sessionInfo.driverCarEstLapTime;

    const calculatedGaps = calculateGaps(estTimes, $driversList, driverIdx, lapTime);
    if (calculatedGaps.length === 0) return [];

    const sortedGaps = calculatedGaps.sort((a, b) => b[1] - a[1]);
    const driverRank = sortedGaps.findIndex((gap) => gap[0] === driverIdx);

    if (driverRank === -1) return [];

    const rowsAbove = 9;
    const rowsBelow = 9;

    const startSlice = driverRank - rowsAbove;
    const endSlice = driverRank + rowsBelow + 1;

    const topPaddingCount = Math.max(0, -startSlice);
    const bottomPaddingCount = Math.max(0, endSlice - sortedGaps.length);

    const topPadding = Array.from({ length: topPaddingCount }, (_, i) => [-1 - i, null]);
    const bottomPadding = Array.from({ length: bottomPaddingCount }, (_, i) => [-1000 - i, null]);

    const slicedGaps = sortedGaps.slice(Math.max(0, startSlice), Math.min(sortedGaps.length, endSlice));

    return [...topPadding, ...slicedGaps, ...bottomPadding];
  });

  function getCarClassColor(driverIdx) {
    if (driverIdx < 0) return 'transparent';
    const driver = $driversList[driverIdx];
    if (!driver) return 'transparent';
    const carClass = $staticSessionInfo.carClasses.find(c => c.carClassId === driver.carClassId);
    console.log(carClass.carClassColor);
    return carClass ? carClass.carClassColor : 'transparent';
  }
</script>

<DataBox title="Relative" class="min-w-2xs">
  <div class="table-container">
    <table class="table-fixed">
      <thead>
        <tr>
          <th class="w-1"></th>
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
            <td colspan="5" style="visibility: hidden">placeholder</td>
          {:else}
            <td style="background-color: {getCarClassColor(gap[0])}"></td>
            <td class="text-end">
              { positions[gap[0]] }
            </td>
            <td class="text-end">
              #{ $driversList[gap[0]].carNumber }
            </td>
            <td class="text-start px-2">
              { $driversList[gap[0]].driverName }
            </td>
            <td class="text-end">
              { gap[1].toFixed(1) }
            </td>
          {/if}
        </tr>
      {/each}
      </tbody>
    </table>
  </div>
</DataBox>

<style>
  .table-container {
    height: 22rem;
    overflow-y: auto;
    display: flex;
  }
  table {
    flex: 1;
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
