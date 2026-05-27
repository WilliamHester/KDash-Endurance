<script>
  import {
    sessionInfo,
    sessionStore,
    staticSessionInfo,
    connected,
    driversList,
    laps,
    lookupTables,
    stints,
    telemetry,
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

  const last2FuelUsageQuery = 'DECREASING_SUM(FuelLevel, 2) / 2';
  const last5FuelUsageQuery = 'DECREASING_SUM(FuelLevel, 5) / 5';
  const fuelLevelQuery = 'FuelLevel';
  const lapFuelUsedQuery = 'LapFuelUsed';
  const stintLengthQuery = 'Lap + (LapFuelUsed / (DECREASING_SUM(FuelLevel, 2) / 2)) - LastPitLap';
  const lapsRemainingQuery = 'SessionTimeRemain / ((0 - 1) * LAP_DELTA(SessionTime))';

  // The list of values we need for this dashboard
  const REQUIRED_QUERIES = [
    'SessionTimeOfDay',
    'SessionTimeRemain',
    'Lap',
    'TrackTempCrew',
    fuelLevelQuery,
    lapFuelUsedQuery,
    'TrackPrecip',
    'PitReqRepairRemaining',
    'PitOptRepairRemaining',
    'PlayerCarTeamIncidentCount',
    'PlayerCarDriverIncidentCount',
    'PlayerCarClassPosition',
    'DECREASING_SUM(FuelLevel, 1)',
    last2FuelUsageQuery,
    last5FuelUsageQuery,
    'CarIdxDriverCarClassEstTime',
    'CarIdxClassPosition',
    'CarIdxEstTime',
    stintLengthQuery,
    lapsRemainingQuery,
  ];

  $effect(() => {
    if ($connected) {
      sessionStore.startTelemetry(REQUIRED_QUERIES);
    }
  })

  const selectedDrivers = $derived($driversList.filter((driver) => selectedCars.has(driver.carId)));
  const carIdxEstTimeToPositionForDriverCarClass = $derived($telemetry['CarIdxDriverCarClassEstTime']);

  const carIdxEstTimesAfterDriverPits = $derived.by(() => {
    const expectedStopTime = 40;
    const stopAndGoSeconds =  $options.stopAndGoSeconds;
    if (carIdxEstTimeToPositionForDriverCarClass === undefined) {
      return [];
    }
    
    const driverMetersToEstTime = $lookupTables.driverCarDistanceMetersToEstTime;
    if ($lookupTables.driverCarDistanceMetersToEstTime == null
      || !driverMetersToEstTime.values
      || driverMetersToEstTime.values.length === 0) {
      return [];
    }

    // Helper for linear interpolation instead of truncating
    const interpolate = (arr, exactIndex) => {
      const floor = Math.max(0, Math.floor(exactIndex));
      const ceil = Math.min(floor + 1, arr.length - 1);
      const fraction = exactIndex - floor;
      return arr[floor] + (arr[ceil] - arr[floor]) * fraction;
    };

    const driverCurrentEstTime = carIdxEstTimeToPositionForDriverCarClass[$staticSessionInfo.driverCarIdx];
    const driverLapRemainingSeconds = $sessionInfo.driverCarEstLapTime - driverCurrentEstTime;
    
    const pitExitMeters = DAYTONA_PIT_EXIT_LAP_DIST_PCT * $staticSessionInfo.lapLengthMeters;
    const pitExitEstTime = interpolate(driverMetersToEstTime.values, pitExitMeters);
    const secondsToPitExitIfPitThisLap = driverLapRemainingSeconds + pitExitEstTime + expectedStopTime + stopAndGoSeconds;

    const carIdxEstTimes = $telemetry['CarIdxEstTime'].map((est, idx) => {
      const lookupTable = $lookupTables.carIdxEstTimeToDistance[idx];
      if (!lookupTable || !lookupTable.values || lookupTable.values.length <= 1) {
        return 0;
      }

      // Buckets are at 60 Hz. Divide the max bucket by 60 to get the lap time.
      // TODO: Consider storing the car's lap time in a proto to avoid looking it up like this.
      // TODO: Also, report a scalar for each driver to calculate their lap time (since it'll differ from iRacing's
      //  predicted lap time).
      const carLapTimeSeconds = (lookupTable.values.length - 1) / 60;
      const wrappedTimeSeconds = (est + secondsToPitExitIfPitThisLap) % carLapTimeSeconds;
      const wrappedTimeSecondsBucket = wrappedTimeSeconds * 60;
      const estDistancePctAfterDriverPitStop = interpolate(lookupTable.values, wrappedTimeSecondsBucket);
      const estDistanceMetersAfterDriverPitStop = estDistancePctAfterDriverPitStop * $staticSessionInfo.lapLengthMeters;
      
      return interpolate(driverMetersToEstTime.values, estDistanceMetersAfterDriverPitStop);
    });
    // The driver will be at the pit exit, not their predicted location if they were lapping normally.
    carIdxEstTimes[$staticSessionInfo.driverCarIdx] = pitExitEstTime;
    return carIdxEstTimes;
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

    <VariableBox title="Laps Remaining">
      { ($telemetry[lapsRemainingQuery] || -1).toFixed(0) }
    </VariableBox>

    <VariableBox title="Position">
      {$telemetry['PlayerCarClassPosition'] || 0}
    </VariableBox>

    <VariableBox title="Track Temp">
      {($telemetry['TrackTempCrew'] || 0).toFixed(1)}°C
    </VariableBox>
    
    <VariableBox title="Track Precip">
      {$telemetry['TrackPrecip'] || 0}%
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

    <VariableBox title="Fuel Target">
      { fuelTarget.toFixed(3) }
    </VariableBox>

    <VariableBox title="Fuel Target +1">
      { fuelTargetPlus1.toFixed(3) }
    </VariableBox>

    <VariableBox title="Laps to go at line">
      { (lapsAtLine || -1).toFixed(2) }
    </VariableBox>
  </div>

  <div class="row logs-row">
    <div class="log-column">
      <CarLapLog entries={$laps} />
    </div>

    <div class="log-column">
      <StintLogTable entries={$stints} />
    </div>

    <GapsTableTable estTimes={carIdxEstTimeToPositionForDriverCarClass} />
    <GapsTableTable estTimes={carIdxEstTimesAfterDriverPits} />
  </div>

  <div class="row mt-1 ms-4">
    <h1>Watched Drivers</h1>
    <TeamSelectionDialog/>
  </div>

  <hr class="separator" />

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

  .separator {
    width: 100%;
    border: 0;
    border-top: 1px solid #404040;
    margin: 8px 0;
  }

  h1 {
    font-size: 1.5rem;
  }
</style>
