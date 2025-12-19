<script>
  import { formatNumberAsDuration, formatDriverName } from '$lib/formatters';

  export let entries = []; // Array of StintEntry objects

  // Logic for highlighting the overall fastest lap across all stints
  $: overallFastestLap = entries.length
    ? Math.min(...entries.filter(e => e.fastestLapTime > 0).map(e => e.fastestLapTime))
    : 0;
</script>

<div class="table-container">
    <table>
        <thead>
        <tr>
            <th>Out Lap</th>
            <th>In Lap</th>
            <th>Driver</th>
            <th>Total Time</th>
            <th>Avg Lap</th>
            <th>Fastest Lap</th>
            <th>Track Temp</th>
            <th>Incidents</th>
        </tr>
        </thead>
        <tbody>
        {#if entries.length === 0}
            <tr>
                <td colspan="8" class="empty">No completed stints yet.</td>
            </tr>
        {:else}
            {#each entries as stint (stint.outLap)}
                <tr>
                    <td class="number">{stint.outLap}</td>
                    <td class="number">{stint.inLap}</td>
                    <td>{formatDriverName(stint.driverName)}</td>

                    <td class="number">
                        {formatNumberAsDuration(stint.totalTime)}
                    </td>

                    <td class="number">
                        {formatNumberAsDuration(stint.averageLapTime)}
                    </td>

                    <td class="number" class:purple={stint.fastestLapTime === overallFastestLap && stint.fastestLapTime > 0}>
                        {formatNumberAsDuration(stint.fastestLapTime)}
                    </td>

                    <td class="number">{stint.trackTemp.toFixed(1)}°C</td>
                    <td class="center">{stint.incidents}</td>
                </tr>
            {/each}
        {/if}
        </tbody>
    </table>
</div>

<style>
    .table-container {
        overflow-x: auto;
        width: 100%;
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
        position: sticky;
        top: 0;
    }

    td {
        padding: 6px 8px;
        border-bottom: 1px solid #222;
    }

    tr:hover {
        background-color: #2a2a2a;
    }

    .number {
        text-align: right;
        font-variant-numeric: tabular-nums;
    }

    .center {
        text-align: center;
    }

    .purple {
        color: #d65dff;
        font-weight: bold;
    }

    .empty {
        text-align: center;
        padding: 2rem;
        color: #666;
        font-style: italic;
    }
</style>
