# ClinicOS Load Testing

Uses [k6](https://k6.io). Install: `winget install k6 --source winget` (Windows) or `brew install k6`.

## Run

```powershell
# Smoke test (5 VUs, 30s) against local Docker stack
k6 run loadtest/k6-load-test.js

# Full load test: ramps to 200 concurrent users (~10k customer peak)
k6 run -e SCENARIO=load loadtest/k6-load-test.js

# Authenticated flows
k6 run -e SCENARIO=load -e TOKEN=<jwt> -e BASE_URL=http://localhost:8080 loadtest/k6-load-test.js
```

## Pass criteria (thresholds enforced automatically)

- p95 latency < 500 ms, p99 < 1.5 s
- Error rate < 1%

## What to do with results

| Symptom | Likely fix |
|---------|-----------|
| p95 grows with VUs, DB CPU high | Add missing index on `clinic_id` + filter columns; check `pg_stat_statements` |
| Connection timeouts | Raise `DB_POOL_SIZE` (but keep total conns across pods < Postgres `max_connections`) |
| Gateway saturated, services idle | Scale gateway HPA max / raise Netty worker threads |
| Memory OOM kills | Raise pod memory limit; JVM already capped at 75% of container RAM |

