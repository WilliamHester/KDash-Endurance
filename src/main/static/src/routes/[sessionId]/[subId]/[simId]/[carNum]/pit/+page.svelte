<script>
  import {
    connected,
    sessionStore,
    telemetry,
  } from '$lib/stores/session';
  import VariableBox from "$lib/components/VariableBox.svelte";

  const REQUIRED_QUERIES = [
    'FuelLevel',
    'DECREASING_SUM(FuelLevel, 1)',
    'DECREASING_SUM(FuelLevel, 5) / 5',
  ];

  $effect(() => {
    if ($connected) {
      sessionStore.startTelemetry(REQUIRED_QUERIES);
    }
  });
</script>

<div class="column">
  <div class="row">
    <VariableBox title="Fuel Level">
      { ($telemetry['FuelLevel'] || 0).toFixed(3) }
    </VariableBox>

    <VariableBox title="Lap over lap fuel">
      { ($telemetry['DECREASING_SUM(FuelLevel, 1)'] || -1).toFixed(3) }
    </VariableBox>

    <VariableBox title="Avg 5 lap over lap fuel">
      { ($telemetry['DECREASING_SUM(FuelLevel, 5) / 5'] || -1).toFixed(3) }
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
