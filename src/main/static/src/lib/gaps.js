export function calculateGaps(distances, drivers, driverIdx, lapTime) {
  if (!distances || driverIdx == null || !lapTime) return [];

  const driverGap = distances[driverIdx];
  if (driverGap == null) return [];

  const halfLapTime = lapTime / 2;
  const calculatedGaps = [];

  for (const driverIdx of drivers.keys()) {
    const gap = distances[driverIdx];
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
