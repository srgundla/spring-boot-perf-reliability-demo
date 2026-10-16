// Steady order traffic while you break the catalog (see runbook, resilience segment).
//   docker compose run --rm k6 run /scripts/orders.js
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://edge:8080';
const degraded = new Counter('degraded_orders');

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: 20, timeUnit: '1s',
      duration: __ENV.DURATION || '120s',
      preAllocatedVUs: 60, maxVUs: 200,
    },
  },
};

export default function () {
  const body = JSON.stringify({
    customerId: 'cust-' + Math.floor(Math.random() * 500),
    productId: 1 + Math.floor(Math.random() * 1000),
    quantity: 1 + Math.floor(Math.random() * 3),
  });
  const res = http.post(`${BASE}/orders/api/orders`, body, { headers: { 'Content-Type': 'application/json' } });
  check(res, { 'order accepted (201)': (r) => r.status === 201 });
  if (res.status === 201 && res.json('degraded') === true) degraded.add(1);
}
