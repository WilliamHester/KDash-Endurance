<script>
  import { formatNumberAsDuration, formatDriverName } from '$lib/utils';
  
  export let entries = [];

  // Reactive calculation for fastest lap to highlight purple
  $: fastestLap = entries.length ? Math.min(...entries.map(e => e.lapTime)) : 0;
</script>

<table class="carLapLog">
  <thead>
    <tr>
      <th>Lap</th>
      <th>Driver</th>
      <th>Pos</th>
      <th>Lap Time</th>
      <th>Gap</th>
    </tr>
  </thead>
  <tbody>
    {#each entries as entry (entry.lapNum)}
      <tr>
        <td class="number">{entry.lapNum}</td>
        <td>{formatDriverName(entry.driverName)}</td>
        <td class="number">{entry.position}</td>
        <td class="number" class:purple={entry.lapTime === fastestLap}>
          {formatNumberAsDuration(entry.lapTime)}
        </td>
        <td class="number">
          {formatNumberAsDuration(entry.gapToLeader, true)}
        </td>
      </tr>
    {/each}
  </tbody>
</table>

<style>
  .carLapLog {
    border-collapse: separate;
    border-spacing: 0;
    font-family: 'Roboto', sans-serif;
    width: 100%;
  }

  th {
    padding: 6px 8px;
    text-align: center;
    border-bottom: 1px solid #333;
  }

  td {
    padding: 6px 8px;
    text-align: center;
  }

  tr:hover {
    background-color: #3a3a3a;
  }

  .number { text-align: right; }
  .purple { color: #ff00ff; font-weight: bold; }
</style>
