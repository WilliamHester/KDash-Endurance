<script>
  import VariableBox from "$lib/components/VariableBox.svelte";
  import {
    drivers,
    otherCarLaps,
    otherCarStints,
    sessionInfo,
    staticSessionInfo,
    telemetry,
  } from "$lib/stores/session.js";
  import {OtherCarLapEntry, OtherCarStintEntry} from "$lib/grpc/live_telemetry_service.ts";
  import { formatNumberAsDuration } from '$lib/formatters';
  import {calculateRelativeGapToDriver} from "$lib/gaps.js";
  import DataBox from "$lib/components/DataBox.svelte";

  const { driver } = $props();

  const lastLap = $derived.by(() => {
    let lap = OtherCarLapEntry.create();
    const driverLaps = $otherCarLaps.filter((lap) => lap.carId === driver.carId)
    for (let i = 0; i < driverLaps.length; i++) {
      if (driverLaps[i].lapNum > lap.lapNum) {
        lap = driverLaps[i];
      }
    }
    return lap;
  });

  const lastStint = $derived.by(() => {
    let stint = OtherCarStintEntry.create();
    const driverStints = $otherCarStints.filter((stint) => stint.carIdx === driver.carId);
    for (let i = 0; i < driverStints.length; i++) {
      // Use inLap here instead of outLap, because the first outLap will be 0 and the default will be 0
      if (driverStints[i].inLap > stint.inLap) {
        stint = driverStints[i];
      }
    }
    return stint;
  });

  const gap = $derived.by(() => {
    const distances = $telemetry['CarIdxDriverCarClassEstTime'];
    console.log(distances);
    const gaps =
      calculateRelativeGapToDriver(
        $telemetry['CarIdxDriverCarClassEstTime'],
        $drivers,
        $staticSessionInfo.driverCarIdx,
        $sessionInfo.driverCarEstLapTime);
    if (gaps.length === 0) {
      return 0;
    }
    return gaps.find((gap) => gap[0] === driver.carId)[1];
  });
</script>

<DataBox title={`#${driver.carNumber} ${driver.teamName}`}>
  <div class="px-4 py-1">
    <table class="w-full text-sm">
      <tbody class="border-separate border-spacing-x-2">
        <tr>
          <td class="text-left">Gap</td>
          <td class="text-right">{ gap.toFixed(1) }</td>
        </tr>
        <tr>
          <td class="text-left">Current lap</td>
          <td class="text-right">{ lastLap.lapNum }</td>
        </tr>
        <tr>
          <td class="text-left">Stint current lap</td>
          <td class="text-right">{ lastLap.lapNum - (lastStint.inLap) }</td>
        </tr>
        <tr>
          <td class="text-left">Last pit out lap</td>
          <td class="text-right">{ lastStint.inLap + 1 }</td>
        </tr>
        <tr>
          <td class="text-left">Last lap time</td>
          <td class="text-right">{ formatNumberAsDuration(lastLap.lapTime) }</td>
        </tr>
    <!--      <tr>-->
    <!--        <td class="text-left">Stint remaining laps</td>-->
    <!--        <td class="text-right">3</td>-->
    <!--      </tr>-->
      </tbody>
    </table>
  </div>
</DataBox>
