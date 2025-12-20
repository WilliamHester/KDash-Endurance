<script>
  import {
    connected,
    sessionStore,
    staticSessionInfo,
    telemetry,
  } from '$lib/stores/session';
  import options from '$lib/stores/options';
  import VariableBox from "$lib/components/VariableBox.svelte";

  const last2FuelUsageQuery = 'DECREASING_SUM(FuelLevel, 2) / 2'
  const last5FuelUsageQuery = 'DECREASING_SUM(FuelLevel, 5) / 5'
  const fuelLevelQuery = 'FuelLevel';
  const lapFuelUsedQuery = 'LapFuelUsed';

  const queries = $derived([
    fuelLevelQuery,
    'DECREASING_SUM(FuelLevel, 1)',
    last2FuelUsageQuery,
    last5FuelUsageQuery,
    lapFuelUsedQuery,
  ]);

  $effect(() => {
    if ($connected) {
      sessionStore.startTelemetry(queries);
    }
  });

  const lapsAtLine = $derived.by(() => {
    if ($staticSessionInfo) {
      const averageFuelUsage = $telemetry[last2FuelUsageQuery];
      const expectedFuelUsage = averageFuelUsage - $telemetry[lapFuelUsedQuery];
      const tankRemaining = $telemetry[fuelLevelQuery] - $options.fuelMargin;
      const remainingAtEndOfLap = tankRemaining - expectedFuelUsage;
      return remainingAtEndOfLap / averageFuelUsage;
    } else {
      return null;
    }
  });
</script>

<div class="column">
  <div class="row">
    <VariableBox title="Fuel Level">
      { ($telemetry[fuelLevelQuery] || 0).toFixed(3) }
    </VariableBox>

    <VariableBox title="Lap over lap fuel">
      { ($telemetry['DECREASING_SUM(FuelLevel, 1)'] || -1).toFixed(3) }
    </VariableBox>

    <VariableBox title="Avg 5 lap over lap fuel">
      { ($telemetry[last5FuelUsageQuery] || -1).toFixed(3) }
    </VariableBox>

    <VariableBox title="Laps at line">
      { (lapsAtLine || -1).toFixed(2) }
    </VariableBox>
  </div>
</div>

<style>
  .column {
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  .row {
    width: 100%;
    display: flex;
    flex-direction: row;
    gap: 4px;
  }
</style>
