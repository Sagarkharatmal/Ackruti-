// Small geographic helpers. Distances are in metres.

const EARTH_RADIUS_M = 6371000;

const toRad = (deg) => (deg * Math.PI) / 180;

/** Great-circle distance between two lat/lng points (haversine). */
export function distanceM(lat1, lng1, lat2, lng2) {
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(a)));
}

export function isValidLatLng(lat, lng) {
  return (
    Number.isFinite(lat) &&
    Number.isFinite(lng) &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180
  );
}

/**
 * Keeps every point that is at least `minStepM` from the previously kept one,
 * then thins the result to at most `maxPoints` (always keeping the last point).
 */
export function simplifyTrail(points, minStepM = 10, maxPoints = 400) {
  if (points.length === 0) return [];
  const kept = [points[0]];
  for (let i = 1; i < points.length; i++) {
    const last = kept[kept.length - 1];
    if (distanceM(last[0], last[1], points[i][0], points[i][1]) >= minStepM) {
      kept.push(points[i]);
    }
  }
  if (kept.length <= maxPoints) return kept;
  const step = kept.length / maxPoints;
  const thinned = [];
  for (let i = 0; i < maxPoints - 1; i++) thinned.push(kept[Math.floor(i * step)]);
  thinned.push(kept[kept.length - 1]);
  return thinned;
}
