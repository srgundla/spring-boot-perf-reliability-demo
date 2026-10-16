// Cache effect. MODE=direct -> PostgreSQL every time, MODE=cached -> Redis (default)
//   docker compose run --rm k6 run -e MODE=direct /scripts/cache.js
//   docker compose run --rm k6 run -e MODE=cached /scripts/cache.js
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'http://edge:8080';
const MODE = __ENV.MODE || 'cached';
const HOT_KEYS = 500; // a realistic "hot set": a few products get most of the traffic

export const options = {
  scenarios: {
    steady: { executor: 'constant-vus', vus: 50, duration: __ENV.DURATION || '40s' },
  },
};

export default function () {
  const id = 1 + Math.floor(Math.random() * HOT_KEYS);
  const suffix = MODE === 'direct' ? '/direct' : '';
  const res = http.get(`${BASE}/catalog/api/products/${id}${suffix}`);
  check(res, { 'status 200': (r) => r.status === 200 });
}
