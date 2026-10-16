// Heavier read load against the (deliberately slow) direct path, to show scale-out helping.
//   docker compose run --rm k6 run /scripts/scale.js
import http from 'k6/http';

const BASE = __ENV.BASE || 'http://edge:8080';

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '15s', target: 150 },
        { duration: '30s', target: 150 },
        { duration: '5s', target: 0 },
      ],
    },
  },
};

export default function () {
  // Pure CPU + DB pressure: random keys across the whole table, bypassing the cache.
  const id = 1 + Math.floor(Math.random() * 400000);
  http.get(`${BASE}/catalog/api/products/${id}/direct`, { tags: { name: 'catalog-direct' } });
}
