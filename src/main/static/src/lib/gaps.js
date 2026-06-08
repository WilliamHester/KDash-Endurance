/**
 * Calculate the gaps between the driver and all other drivers.
 *
 * @param estLapTimes - an array of estimated lap times for each driver
 * @param drivers - an array of drivers
 * @param driverIdx - the target driver's index within the distances array
 * @param lapTime - the estimated lap time for the target driver in seconds
 * @returns an array of pairs of the driver index to the gap (in seconds)
 */
export function calculateRelativeGapToDriver(estLapTimes, drivers, driverIdx, lapTime) {
  if (!estLapTimes || driverIdx == null || !lapTime) return [];

  const driverGap = estLapTimes[driverIdx];
  if (driverGap == null) return [];

  const halfLapTime = lapTime / 2;
  const calculatedGaps = [];

  for (const driverIdx of drivers.keys()) {
    const gap = estLapTimes[driverIdx];
    if (gap == null) continue;

    let diff = gap - driverGap;

    if (diff > halfLapTime) {
      diff -= lapTime;
    } else if (diff < -halfLapTime) {
      diff += lapTime;
    }

    calculatedGaps.push([driverIdx, diff]);
  }

  return calculatedGaps;
}
