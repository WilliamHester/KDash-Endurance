<script>
  import { drivers, otherCarLaps } from '$lib/stores/session';
  import { formatNumberAsDuration, formatDriverName } from '$lib/formatters';
  import PitChip from '$lib/components/PitChip.svelte';

  // State for the dropdown
  let selectedCarId = -1;

  // 1. Convert Drivers Map to Array for the dropdown
  // We sort by car number for easier finding
  $: driverOptions = Array.from($drivers.values())
    .sort((a, b) => a.carNumber - b.carNumber);

  // 2. Default Selection Logic
  // If no car is selected, pick the first one available
  $: if (selectedCarId === -1 && driverOptions.length > 0) {
    selectedCarId = driverOptions[0].carId;
  }

  // 3. Filter the Laps based on selection
  // This runs automatically whenever selectedCarId OR $otherCarLaps changes
  $: filteredLaps = $otherCarLaps.filter(l => l.carId === selectedCarId);

  // 4. Calculate fastest lap for the selected car (Purple Sector logic)
  $: fastestLap = filteredLaps.length
    ? Math.min(...filteredLaps.filter(e => e.lapTime > 0).map(e => e.lapTime))
    : 0;
</script>

<div class="page-layout">
    <div class="controls">
        <label for="driver-select">Select Driver:</label>
        <select id="driver-select" bind:value={selectedCarId}>
            {#each driverOptions as driver}
                <option value={driver.carId}>
                    #{driver.carNumber} - {driver.teamName} ({formatDriverName(driver.driverName)})
                </option>
            {/each}
        </select>

        <span class="count">Laps Logged: {filteredLaps.length}</span>
    </div>

    <div class="table-container">
        <table>
            <thead>
            <tr>
                <th>Lap</th>
                <th>Driver</th>
                <th>Pos</th>
                <th>Lap Time</th>
                <th>Gap to Leader</th>
                <th>Track Temp</th>
                <th>Pit</th>
            </tr>
            </thead>
            <tbody>
            {#if filteredLaps.length === 0}
                <tr>
                    <td colspan="7" class="empty">No laps recorded for this car.</td>
                </tr>
            {:else}
                {#each filteredLaps as entry (entry.lapNum)}
                    <tr>
                        <td class="number">{entry.lapNum}</td>
                        <td>{formatDriverName(entry.driverName)}</td>
                        <td class="number">{entry.position}</td>

                        <td class="number" class:purple={entry.lapTime === fastestLap && entry.lapTime > 0}>
                            {formatNumberAsDuration(entry.lapTime)}
                        </td>

                        <td class="number">
                            {formatNumberAsDuration(entry.gapToLeader, false, true)}
                        </td>

                        <td class="number">{entry.trackTemp.toFixed(1)}°C</td>

                        <td class="center">
                            <PitChip {entry} />
                        </td>
                    </tr>
                {/each}
            {/if}
            </tbody>
        </table>
    </div>
</div>

<style>
    .page-layout {
        padding: 0 10px;
        height: 100%;
        display: flex;
        flex-direction: column;
    }

    .controls {
        display: flex;
        align-items: center;
        gap: 15px;
        margin-bottom: 1rem;
        padding: 10px;
        background-color: #1a1a1a;
        border-bottom: 1px solid #333;
        border-radius: 4px;
    }

    select {
        background-color: #333;
        color: white;
        border: 1px solid #555;
        padding: 8px;
        border-radius: 4px;
        font-size: 14px;
        min-width: 300px;
    }

    .count {
        color: #888;
        font-family: monospace;
        font-size: 0.9em;
        margin-left: auto;
    }

    /* Table Styles (Similar to LapLogTable but simplified) */
    .table-container {
        overflow-y: auto;
        flex: 1;
    }

    table {
        width: 100%;
        border-collapse: separate;
        border-spacing: 0;
        font-size: 12px;
        font-family: 'Roboto', sans-serif;
    }

    th {
        text-align: center;
        padding: 8px;
        border-bottom: 1px solid #444;
        background-color: #1a1a1a;
        color: #aaa;
        font-weight: normal;
        position: sticky;
        top: 0;
        z-index: 1;
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

    .center { text-align: center; }

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
