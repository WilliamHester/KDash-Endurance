<script>
  import { formatNumberAsDuration, formatDriverName } from '$lib/formatters';
  import DataBox from "$lib/components/DataBox.svelte";

  const { entries, class: className = '' } = $props();

  const fastestLap = $derived(entries.length ? Math.min(...entries.map(e => e.lapTime)) : 0);
</script>

<DataBox title="Laps" class={className}>
  <div class="table-container text-xs">
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
  </div>
</DataBox>

<style>
  .table-container {
    flex: 1;
    overflow: auto;
    width: 100%;
    height: 100%;
  }

  .carLapLog {
    border-collapse: separate;
    border-spacing: 0;
    width: 100%;
  }

  th {
    padding: 6px 8px;
    text-align: center;
    border-bottom: 1px solid #333;
    position: sticky;
    top: 0;
    background-color: #1a1a1a;
    z-index: 1;
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
