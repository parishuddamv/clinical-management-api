// k6 smoke/load test for ClinicOS.
// Usage:
//   k6 run loadtest/k6-load-test.js                          (smoke: 5 VUs, 30s)
//   k6 run -e SCENARIO=load loadtest/k6-load-test.js         (load: ramp to 200 VUs)
//   k6 run -e BASE_URL=https://api.example.com -e TOKEN=eyJ... loadtest/k6-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';

const errorRate = new Rate('errors');
const healthTrend = new Trend('health_duration', true);

const scenarios = {
  smoke: {
    executor: 'constant-vus',
    vus: 5,
    duration: '30s',
  },
  load: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '2m', target: 50 },
      { duration: '5m', target: 200 },  // ~10k customers peak concurrency
      { duration: '2m', target: 200 },
      { duration: '1m', target: 0 },
    ],
  },
};

export const options = {
  scenarios: { main: scenarios[__ENV.SCENARIO || 'smoke'] },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1500'],
    errors: ['rate<0.01'],
  },
};

const authHeaders = TOKEN
  ? { headers: { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' } }
  : { headers: { 'Content-Type': 'application/json' } };

export default function () {
  // 1. Gateway health (always available, no auth)
  const health = http.get(`${BASE_URL}/actuator/health`);
  healthTrend.add(health.timings.duration);
  const healthOk = check(health, { 'gateway health 200': (r) => r.status === 200 });
  errorRate.add(!healthOk);

  // 2. Authenticated flows (only when TOKEN provided)
  if (TOKEN) {
    const patients = http.get(`${BASE_URL}/api/v1/patients/search?q=&page=0&size=20`, authHeaders);
    const ok = check(patients, {
      'patient search 200': (r) => r.status === 200,
      'patient search < 500ms': (r) => r.timings.duration < 500,
    });
    errorRate.add(!ok);

    const appointments = http.get(`${BASE_URL}/api/v1/appointments?page=0&size=20`, authHeaders);
    errorRate.add(!check(appointments, { 'appointments 200': (r) => r.status === 200 }));
  }

  sleep(1);
}

