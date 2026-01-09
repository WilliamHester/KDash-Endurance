export function calculateGaps(distances, driversList, driverIdx, lapTime) {
  if (!distances || driverIdx == null || !lapTime) return [];

  const driverGap = distances[driverIdx];
  if (driverGap == null) return [];

  const halfLapTime = lapTime / 2;
  const calculatedGaps = [];

  for (let i = 0; i < driversList.length; i++) {
    const gap = distances[i];
    if (gap == null) continue;

    let diff = gap - driverGap;

    if (diff > halfLapTime) {
      diff -= lapTime;
    } else if (diff < -halfLapTime) {
      diff += lapTime;
    }

    calculatedGaps.push([i, diff]);
  }

  console.log(calculatedGaps);

  return calculatedGaps;
}
