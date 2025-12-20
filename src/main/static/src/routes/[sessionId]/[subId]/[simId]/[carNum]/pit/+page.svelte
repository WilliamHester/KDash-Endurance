<script>
  import {
    connected,
    sessionStore,
    telemetry,
  } from '$lib/stores/session';
  import options from '$lib/stores/options';
  import VariableBox from "$lib/components/VariableBox.svelte";

  const last2FuelUsageQuery = 'DECREASING_SUM(FuelLevel, 2) / 2';
  const last5FuelUsageQuery = 'DECREASING_SUM(FuelLevel, 5) / 5';
  const fuelLevelQuery = 'FuelLevel';
  const lapFuelUsedQuery = 'LapFuelUsed';
  const stintLengthQuery = 'Lap + (LapFuelUsed / (DECREASING_SUM(FuelLevel, 2) / 2)) - LastPitLap';

  const queries = $derived([
    fuelLevelQuery,
    'DECREASING_SUM(FuelLevel, 1)',
    last2FuelUsageQuery,
    last5FuelUsageQuery,
    lapFuelUsedQuery,
    stintLengthQuery,
  ]);

  $effect(() => {
    if ($connected) {
      sessionStore.startTelemetry(queries);
    }
  });

  const lapsAtLine = $derived.by(() => {
    const averageFuelUsage = $telemetry[last2FuelUsageQuery];
    const expectedFuelUsage = averageFuelUsage - $telemetry[lapFuelUsedQuery];
    const tankRemaining = $telemetry[fuelLevelQuery] - $options.fuelMargin;
    const remainingAtEndOfLap = tankRemaining - expectedFuelUsage;
    return remainingAtEndOfLap / averageFuelUsage;
  });

  const fuelTarget = $derived.by(() => {
    const currentStintLength = $telemetry[stintLengthQuery];
    const targetRemainingLaps = $options.targetStintLength - currentStintLength;
    if (targetRemainingLaps > 0) {
      return ($telemetry[fuelLevelQuery] - $options.fuelMargin) / targetRemainingLaps;
    } else {
      return 0;
    }
  });

  const fuelTargetPlus1 = $derived.by(() => {
    const currentStintLength = $telemetry[stintLengthQuery];
    const targetRemainingLaps = $options.targetStintLength + 1 - currentStintLength;
    if (targetRemainingLaps > 0) {
      return ($telemetry[fuelLevelQuery] - $options.fuelMargin) / targetRemainingLaps;
    } else {
      return 0;
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

    <VariableBox title="Laps to go at line">
      { (lapsAtLine || -1).toFixed(2) }
    </VariableBox>

    <VariableBox title="Fuel Target">
      { fuelTarget.toFixed(3) }
    </VariableBox>

    <VariableBox title="Fuel Target +1">
      { fuelTargetPlus1.toFixed(3) }
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
