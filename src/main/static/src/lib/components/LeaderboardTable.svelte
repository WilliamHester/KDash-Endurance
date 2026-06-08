<script>
  import {
    drivers,
    laps,
    otherCarLaps,
    sessionInfo,
    staticSessionInfo,
    telemetry,
  } from "$lib/stores/session.js";
  import { formatNumberAsDuration } from "$lib/formatters.js";
  import HeaderSelect from "$lib/components/HeaderSelect.svelte";

  function formatGap(lapDelta, gap) {
    let lapsString;
    if (lapDelta <= 0) {
      lapsString = "";
    } else if (lapDelta === 1) {
      lapsString = "1 lap + ";
    } else {
      lapsString = `${lapDelta} laps + `;
    }
    return lapsString + formatNumberAsDuration(gap, true);
  }

  let driverOrTeamName = $state("Driver");
  let bestLapType = $state("Driver Best Lap");

  /*
  Receive drivers as an object like
  {
    driver: driver,
    lapDistPct: lapDistPct,
    estTimeAtPos: estTimeAtPos,
    lapsCompleted: ,
  }
   */
  function calculateGapsBetweenDrivers(driverInfo, lapTime) {
    if (driverInfo.length === 0) return;

    driverInfo.sort((left, right) => {
      // Sort in reverse order so we get the largest distances at the front.
      right.lapsCompleted +
        right.lapDistPct -
        (left.lapsCompleted + left.lapDistPct);
    });

    const leader = driverInfo[0];
    let lastDriver = leader;
    for (let i = 1; i < driverInfo.length; i++) {
      // TODO: Account for lap differences in the gap. If the leader is on lap 100 but the driver
      //  is only on lap 90, then the gap is 10 laps, even if they're at the same position.
      //  A lazy way to do this might be to just multiply the laps completed by the lapTime then add that to
      //  the estimated time to reach that position for both drivers. However, that doesn't really allow for
      //  returning something different than the time. Large deltas (e.g. 8 minutes) may look funny and be
      //  inaccurate.
      const thisDriver = driverInfo[i];
      let gap = lastDriver.estTimeAtPos - thisDriver.estTimeAtPos;
      let gapLaps = lastDriver.lapsCompleted - thisDriver.lapsCompleted;
      if (gap < 0) {
        gap += lapTime;
        gapLaps--;
      }

      let gapToLeader = leader.estTimeAtPos - thisDriver.estTimeAtPos;
      let gapToLeaderLaps = leader.lapsCompleted - thisDriver.lapsCompleted;
      if (gapToLeader < 0) {
        gapToLeader += lapTime;
        gapToLeaderLaps--;
      }

      thisDriver.gapAhead = formatGap(gapLaps, gap);
      thisDriver.gapToLeader = formatGap(gapToLeaderLaps, gapToLeader);

      lastDriver = thisDriver;
    }
  }

  function calculatePace(driverLaps) {
    const lastFive = driverLaps.slice(-5).map((item) => item.lapTime);
    const topTwo = lastFive
      .toSorted((a, b) => a.lapTime - b.lapTime)
      .slice(0, 2);
    if (topTwo.length === 0) {
      return -1;
    }
    return (
      topTwo.reduce((sum, item) => sum + item, 0) / Math.max(topTwo.length, 1)
    );
  }

  const driverPace = $derived(calculatePace($laps));

  function getTelemetryValueOrDefault(query, idx, defaultValue) {
    const telemetryArray = $telemetry[query];
    if (telemetryArray === undefined) {
      return defaultValue;
    }
    if (telemetryArray.length <= idx) {
      return defaultValue;
    }
    return telemetryArray[idx];
  }

  const carClasses = $derived.by(() => {
    const carClasses = $staticSessionInfo.carClasses
      .map((carClass) => {
        const classDrivers = $drivers
          .entries()
          .filter((d) => d[1].carClassId === carClass.carClassId);
        const classDriversOutput = [];

        for (const [carIdx, driver] of classDrivers) {
          const carLaps = $otherCarLaps.filter((lap) => lap.carId === carIdx);
          let lastLap = -1;
          let bestLap = Number.MAX_VALUE;
          let bestLapDriver = Number.MAX_VALUE;
          let lastPitLap = 0;
          for (let i = 0; i < carLaps.length; i += 1) {
            const carLap = carLaps[i];
            lastLap = carLap.lapTime;
            if (lastLap < bestLap) {
              bestLap = lastLap;
            }
            if (
              lastLap < bestLapDriver &&
              carLap.driverName === driver.driverName
            ) {
              bestLapDriver = lastLap;
            }
            if (lastLap.pitOut) {
              lastPitLap = lastLap.lapNum;
            }
          }
          const pace = calculatePace(carLaps);

          classDriversOutput.push({
            driver: driver,
            lapDistPct: getTelemetryValueOrDefault(
              "CarIdxLapDistPct",
              carIdx,
              0,
            ),
            estTimeAtPos: getTelemetryValueOrDefault(
              "CarIdxDriverCarClassEstTime",
              carIdx,
              0,
            ),
            lapsCompleted: getTelemetryValueOrDefault(
              "CarIdxLapCompleted",
              carIdx,
              0,
            ),
            lastLap: lastLap,
            bestLap: bestLap,
            bestLapDriver: bestLapDriver,
            pace: pace,
            relativePace: pace - driverPace,
            lastPitLap: lastPitLap,
            nextPitLap: lastPitLap + 30, // TODO: Actually compute this dynamically
            tires: "Dry", // TODO: Actually find this in the data
          });
        }

        classDriversOutput.sort(
          (a, b) =>
            b.lapsCompleted + b.lapDistPct - (a.lapsCompleted + a.lapDistPct),
        );

        calculateGapsBetweenDrivers(
          classDriversOutput,
          $sessionInfo.driverCarEstLapTime,
        );

        return {
          carClass: carClass,
          drivers: classDriversOutput,
        };
      })
      // Remove any car classes that don't have any drivers.
      .filter((c) => c.drivers.length > 0);
    // Sort by fastest estimated lap time (per iRacing session string YAML).
    carClasses.sort(
      (a, b) =>
        a.drivers[0].driver.estimatedLapTime -
        b.drivers[0].driver.estimatedLapTime,
    );
    return carClasses;
  });

  function getCarClassColor(carClassId) {
    const carClass = $staticSessionInfo.carClasses.find(
      (c) => c.carClassId === carClassId,
    );
    return carClass ? carClass.carClassColor : "transparent";
  }
</script>

<div class="min-w-2xs">
  <!-- TODO: Add dropdown/toggle to select classes to display. -->
  <div class="table-container">
    <table class="table-fixed w-full">
      <thead>
        <tr class="bg-neutral-800 text-white">
          <th class="px-2 py-2 w-1 text-left"></th>
          <th class="px-2 py-2 text-right">Pos</th>
          <th class="px-2 py-2 text-right">Num</th>
          <th class="px-2 text-left">
            <HeaderSelect
              bind:value={driverOrTeamName}
              options={["Driver", "Team"]}
            />
          </th>
          <th class="px-2 py-2 text-right">Gap</th>
          <th class="px-2 py-2 text-right">Leader</th>
          <!-- The gap to the leader (in class) -->
          <th class="px-2 py-2 text-right">Lap</th>
          <th class="px-2 py-2 text-right">Last Lap</th>
          <!-- The last lap time -->
          <th class="px-2 text-right">
            <HeaderSelect
              bind:value={bestLapType}
              options={["Driver Best Lap", "Team Best Lap"]}
              class="ml-auto"
            />
          </th>
          <th class="px-2 py-2 text-right">Pace</th>
          <!-- The best 2 of the last 5 laps, averaged -->
          <th class="px-2 py-2 text-right">Relative Pace</th>
          <!-- The driver's pace minus this car's pace, two decimal points -->
          <th class="px-2 py-2 text-right">Last Pit</th>
          <th class="px-2 py-2 text-right">Next Pit (laps)</th>
          <!-- The next lap number this driver will pit (laps until it happens)  -->
          <th class="px-2 py-2 text-left">Tires</th>
        </tr>
      </thead>
      <tbody>
        {#each carClasses as carClass}
          <tr
            style="background-color: {getCarClassColor(
              carClass.carClass.carClassId,
            )}"
            class="border-b-white"
          >
            <td colspan="14" class="text-center py-2 font-bold"
              >{carClass.carClass.carClassShortName}</td
            >
          </tr>
          {#each carClass.drivers as driver, index}
            <tr
              class="odd:bg-neutral-900"
              class:driver-row={driver.driver.carId ===
                $staticSessionInfo.driverCarIdx}
            >
              <td class="px-2 py-1"></td>
              <td class="px-2 py-1 text-right">{index + 1}</td>
              <td class="px-2 py-1 text-right">#{driver.driver.carNumber}</td>
              <td class="px-2 py-1 text-left">
                {driverOrTeamName === "Driver"
                  ? driver.driver.driverName
                  : driver.driver.teamName}
              </td>
              <td class="px-2 py-1 text-right">{driver.gapAhead}</td>
              <td class="px-2 py-1 text-right">{driver.gapToLeader}</td>
              <td class="px-2 py-1 text-right">{driver.lapsCompleted}</td>
              <td class="px-2 py-1 text-right"
                >{formatNumberAsDuration(driver.lastLap)}</td
              >
              <td class="px-2 py-1 text-right">
                {formatNumberAsDuration(
                  bestLapType === "Driver Best Lap"
                    ? driver.bestLapDriver
                    : driver.bestLap,
                )}
              </td>
              <td class="px-2 py-1 text-right"
                >{formatNumberAsDuration(driver.pace)}</td
              >
              <td class="px-2 py-1 text-right"
                >{formatNumberAsDuration(driver.relativePace, true, true)}</td
              >
              <td class="px-2 py-1 text-right">{driver.lastPitLap}</td>
              <td class="px-2 py-1 text-right"
                >{driver.nextPitLap} ({driver.nextPitLap -
                  driver.lapsCompleted})</td
              >
              <td class="px-2 py-1 text-left">{driver.tires}</td>
            </tr>
          {/each}
        {/each}
      </tbody>
    </table>
  </div>
</div>

<style>
  .table-container {
    overflow-y: auto;
    display: flex;
  }

  .driver-row {
    color: #f79200;
  }
</style>
