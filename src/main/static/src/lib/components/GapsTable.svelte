<script>
  import {
    driversList,
    sessionInfo,
    staticSessionInfo,
    telemetry,
  } from "$lib/stores/session.js";

  const gapsList = $derived.by(() => {
    const gaps = $telemetry['CarIdxDriverCarClassEstTime'] || [];
    const indexedGapsAndDistances = gaps.map((gap, index) => [index, gap]).slice(0, $driversList.length);
    console.log($sessionInfo);

    const driverRow = indexedGapsAndDistances[$staticSessionInfo.driverCarIdx];
    if (driverRow == null) {
      return [];
    }
    const halfLapTime = $sessionInfo.driverCarEstLapTime / 2;
    // TODO: I think this can be simplified a bit. Sorting is all the same, and the two middle cases are identical.
    const gapsAndDistancesLapAhead =
      // Other car behind the driver car but more than half a lap behind
      indexedGapsAndDistances.filter((row) => row[1] < driverRow[1] && driverRow[1] - row[1] >= halfLapTime)
        .map((row) => [row[0], row[1] + $sessionInfo.driverCarEstLapTime - driverRow[1]])
        .sort((a, b) => b[1] - a[1]);
    const gapsAndDistancesAhead =
      indexedGapsAndDistances.filter(
        (row) =>
          // Other car is ahead of driver car within the same lap, and the other car is less than half a lap ahead
          (row[1] > driverRow[1] && (row[1] - driverRow[1]) < halfLapTime))
        .map((row) => [row[0], row[1] - driverRow[1]])
        .sort((a, b) => b[1] - a[1]);
    const gapsAndDistancesBehind =
      indexedGapsAndDistances.filter(
        (row) =>
          // Other car is behind the driver within the same lap, and it's less than half a lap behind
          (row[1] < driverRow[1] && (driverRow[1] - row[1]) < halfLapTime))
        .map((row) => [row[0], row[1] - driverRow[1]])
        .sort((a, b) => b[1] - a[1]);
    const gapsAndDistancesLapBehind =
      indexedGapsAndDistances.filter(
        (row) =>
          // Other car is ahead of the current driver, and it's greater than half a lap ahead
          (row[1] > driverRow[1] && row[1] - driverRow[1] >= halfLapTime))
        .map((row) => [row[0], row[1] - $sessionInfo.driverCarEstLapTime - driverRow[1]])
        .sort((a, b) => b[1] - a[1]);
    driverRow[1] = 0; // The relative to yourself is always 0
    return [
      ...gapsAndDistancesLapAhead,
      ...gapsAndDistancesAhead,
      driverRow,
      ...gapsAndDistancesBehind,
      ...gapsAndDistancesLapBehind,
    ];
  });
</script>

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
    <tr>
      <td>
        idk
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
    </tr>
  {/each}
  </tbody>
</table>
