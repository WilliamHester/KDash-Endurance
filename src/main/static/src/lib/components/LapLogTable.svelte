<script>
  import { formatNumberAsDuration, formatDriverName } from '$lib/formatters';
  import PitChip from '$lib/components/PitChip.svelte';

  export let entries = [];

  // Calculate the fastest lap in the current set for purple highlighting
  $: fastestLap = entries.length
    ? Math.min(...entries.filter(e => e.lapTime > 0).map(e => e.lapTime))
    : 0;
</script>

<div class="table-container">
    <table>
        <thead>
        <tr>
            <th>Lap</th>
            <th>Driver</th>
            <th>Pos</th>
            <th>Lap Time</th>
            <th>Gap to Leader</th>
            <th>Fuel Level</th>
            <th>Fuel Used</th>
            <th>Track Temp</th>
            <th>Max Speed</th>
            <th>Incidents</th>
            <th>Repairs</th>
            <th>Pit</th>
        </tr>
        </thead>
        <tbody>
        {#each entries as entry (entry.lapNum)}
            <tr>
                <td class="number">{entry.lapNum}</td>
                <td class="center">{formatDriverName(entry.driverName)}</td>
                <td class="number">{entry.position}</td>

                <td class="number" class:purple={entry.lapTime === fastestLap && entry.lapTime > 0}>
                    {formatNumberAsDuration(entry.lapTime)}
                </td>

                <td class="number">
                    {formatNumberAsDuration(entry.gapToLeader, false, true)}
                </td>

                <td class="number">{entry.fuelRemaining.toFixed(2)}</td>
                <td class="number">{entry.fuelUsed.toFixed(3)}</td>
                <td class="number">{entry.trackTemp.toFixed(1)}°C</td>
                <td class="number">{(entry.maxSpeed * 3.6).toFixed(1)} <small>kph</small></td>

                <td class="center">
                    {entry.driverIncidents} <span class="dim">/ {entry.teamIncidents}</span>
                </td>

                <td class="number">
                    {formatNumberAsDuration(entry.repairsRemaining)}
                    <span class="dim">/ {formatNumberAsDuration(entry.optionalRepairsRemaining)}</span>
                </td>

                <td class="center">
                    <PitChip {entry} />
                </td>
            </tr>
        {/each}
        </tbody>
    </table>
</div>

<style>
    .table-container {
        overflow-x: auto;
    }

    table {
        width: 100%;
        border-collapse: separate;
        border-spacing: 0;
        font-family: 'Roboto', sans-serif;
    }

    th {
        text-align: center;
        padding: 8px;
        border-bottom: 1px solid #444;
        background-color: #1a1a1a;
        color: #aaa;
        font-weight: normal;
        white-space: nowrap;
    }

    td {
        padding: 6px 8px;
        border-bottom: 1px solid #222;
    }

    tr:hover {
        background-color: #2a2a2a;
    }

    .number {
        text-align: center;
        font-variant-numeric: tabular-nums;
    }

    .center {
        text-align: center;
    }

    .purple {
        color: #d65dff; /* Bright purple for dark mode */
        font-weight: bold;
    }

    .dim {
        color: #666;
    }

    small {
        color: #666;
        font-size: 0.8em;
    }
</style>
