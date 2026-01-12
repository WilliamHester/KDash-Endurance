<script>
  import {
    sessionStore,
    sessionInfo,
    staticSessionInfo,
    connected,
    driversList,
    laps,
    stints,
    telemetry, lookupTables,
  } from '$lib/stores/session';
  import {
    selectedCars,
  } from "$lib/stores/drivers.svelte.js";
  import options from '$lib/stores/options';
  import VariableBox from '$lib/components/VariableBox.svelte';
  import CarLapLog from '$lib/components/CarLapLog.svelte';
  import { timeOfDayFormatter, hourMinuteSecondFormatter, minuteSecondFormatter } from '$lib/formatters';
  import StintLogTable from "$lib/components/StintLogTable.svelte";
  import TeamSelectionDialog from "$lib/components/TeamSelectionDialog.svelte";
  import GapsTableTable from "$lib/components/GapsTable.svelte";
  import DriverDataBox from "$lib/components/DriverDataBox.svelte";

  const DAYTONA_PIT_EXIT_LAP_DIST_PCT = 0.12898552;

  // The list of values we need for this dashboard
  const REQUIRED_QUERIES = [
    'SessionTimeOfDay',
    'SessionTimeRemain',
    'Lap',
    'TrackTempCrew',
    'FuelLevel',
    'TrackPrecip',
    'PitReqRepairRemaining',
    'PitOptRepairRemaining',
    'PlayerCarTeamIncidentCount',
    'PlayerCarDriverIncidentCount',
    'DECREASING_SUM(FuelLevel, 1)',
    'DECREASING_SUM(FuelLevel, 5) / 5',
    'CarIdxDriverCarClassEstTime',
    'CarIdxClassPosition',
    'CarIdxEstTime',
  ];

  $effect(() => {
    if ($connected) {
      sessionStore.startTelemetry(REQUIRED_QUERIES);
    }
  })

  let fuelLevel = $derived($telemetry['FuelLevel'] || 0);
  let lapFuel = $derived($telemetry['DECREASING_SUM(FuelLevel, 1)'] || 0);
  let avg5LapFuel = $derived($telemetry['DECREASING_SUM(FuelLevel, 5) / 5'] || 0);

  // Logic: (Fuel - 1) / Ceil( (Fuel - 1) / LapFuel )
  let fuelTargetPlus1 = $derived.by(() => {
      if (lapFuel === 0) return 0;
      const fuelMinus1 = fuelLevel - 1;
      const lapsRemainingCurrentFuel = fuelMinus1 / lapFuel;
      return fuelMinus1 / Math.ceil(lapsRemainingCurrentFuel);
  });

  const selectedDrivers = $derived($driversList.filter((driver) => selectedCars.has(driver.carId)));
  const carIdxEstTimeToPositionForDriverCarClass = $derived($telemetry['CarIdxDriverCarClassEstTime']);

  const carIdxEstTimesAfterDriverPits = $derived.by(() => {
    const expectedStopTime = 40;
    const stopAndGoSeconds =  $options.stopAndGoSeconds;
    if (carIdxEstTimeToPositionForDriverCarClass === undefined) {
      return [];
    }
    if ($lookupTables.driverCarDistanceMetersToEstTime.length === 0) {
      return [];
    }

    const driverCurrentEstTime = carIdxEstTimeToPositionForDriverCarClass[$staticSessionInfo.driverCarIdx];
    const driverMetersToEstTime = $lookupTables.driverCarDistanceMetersToEstTime;
    const driverLapRemaining = $sessionInfo.driverCarEstLapTime - driverCurrentEstTime;
    // TODO: interpolate instead of truncating only
    const pitExitMeters = Math.trunc(DAYTONA_PIT_EXIT_LAP_DIST_PCT * $staticSessionInfo.lapLengthMeters);
    const pitExitEstTime = driverMetersToEstTime.values[pitExitMeters];
    const secondsToPitExitIfPitThisLap = driverLapRemaining + pitExitEstTime + expectedStopTime + stopAndGoSeconds;

    const carIdxEstTimes = $telemetry['CarIdxEstTime'].map((est, idx) => {
      const lookupTable = $lookupTables.carIdxEstTimeToDistance[idx];
      if (!lookupTable) {
        return 0;
      }
      const estDistanceAfterDriverPitStop =
        (est + secondsToPitExitIfPitThisLap) % lookupTable.values[lookupTable.values.length - 1];
      const estDistanceMetersAfterDriverPitStop = estDistanceAfterDriverPitStop * $staticSessionInfo.lapLengthMeters;
      // TODO: interpolate instead of truncating only
      return $lookupTables.driverCarDistanceMetersToEstTime.values[Math.trunc(estDistanceMetersAfterDriverPitStop)];
    });
    // The driver will be at the pit exit, not their predicted location if they were lapping normally.
    carIdxEstTimes[$staticSessionInfo.driverCarIdx] = pitExitEstTime;
    return carIdxEstTimes;
  });
</script>

<div class="dashboard-container">
  <div class="row">
    <VariableBox title="Sim Time">
      {timeOfDayFormatter($telemetry['SessionTimeOfDay'])}
    </VariableBox>
    
    <VariableBox title="Time Remaining">
      {hourMinuteSecondFormatter($telemetry['SessionTimeRemain'])}
    </VariableBox>
    
    <VariableBox title="Lap">
      {$telemetry['Lap'] || '--'}
    </VariableBox>
    
    <VariableBox title="Track Temp">
      {($telemetry['TrackTempCrew'] || 0).toFixed(1)}°C
    </VariableBox>
    
    <VariableBox title="Lap Fuel">
      {lapFuel.toFixed(3)}
    </VariableBox>
    
    <VariableBox title="Avg 5 Lap Fuel">
      {avg5LapFuel.toFixed(3)}
    </VariableBox>
    
    <VariableBox title="Track Precip">
      {$telemetry['TrackPrecip'] || 0}%
    </VariableBox>
    
    <VariableBox title="Fuel Target (+1 Lap)">
      {fuelTargetPlus1.toFixed(3)}
    </VariableBox>
    
    <VariableBox title="Repairs (Opt)">
      {minuteSecondFormatter($telemetry['PitReqRepairRemaining'])}
      <span class="sub-text">({minuteSecondFormatter($telemetry['PitOptRepairRemaining'])})</span>
    </VariableBox>
    
    <VariableBox title="Incidents">
      {$telemetry['PlayerCarTeamIncidentCount'] || 0}
      <span class="sub-text">({$telemetry['PlayerCarDriverIncidentCount'] || 0})</span>
    </VariableBox>
  </div>

  <div class="row logs-row">
    <div class="log-column">
      <CarLapLog entries={$laps} />
    </div>

    <div class="log-column">
      <StintLogTable entries={$stints} />
    </div>

    <TeamSelectionDialog/>

    <GapsTableTable estTimes={carIdxEstTimeToPositionForDriverCarClass} />
    <GapsTableTable estTimes={carIdxEstTimesAfterDriverPits} />
  </div>

  <div class="row">
    {#each selectedDrivers as selectedDriver}
      <DriverDataBox driver={selectedDriver}/>
    {/each}
  </div>
</div>

<style>
  .dashboard-container {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .row {
    display: flex;
    flex-wrap: wrap;
    width: 100%;
    gap: 8px;
  }

  .logs-row {
    height: 500px; /* Fixed height for scrollable tables */
    gap: 10px;
  }

  .log-column {
    /*border: 1px solid #404040;*/
    display: flex;
    flex-direction: column;
    min-width: 300px;
    height: 100%;
  }

  /* Target the DataBox component inside log-column to make it fill the height */
  .log-column :global(.data-box-container) {
    height: 100%;
    width: 100%;
    display: flex;
    flex-direction: column;
  }

  /* Sub-text smaller font for (Optional Repairs) etc */
  .sub-text {
    font-size: 0.6em;
    color: #888;
    margin-left: 4px;
  }
</style>
