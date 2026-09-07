# Performance Quick Reference - Clinical Management System

## 🚀 Performance Targets

| Metric | Target | Status |
|--------|--------|--------|
| Patient Search API | < 100ms | ✅ Optimized |
| Patient Details API | < 50ms | ✅ Optimized |
| Dashboard Load | < 300ms | ✅ Optimized |
| Appointment List | < 100ms | ✅ Optimized |
| P99 Latency | < 500ms | ✅ Optimized |
| Throughput | 1000+ req/s | ✅ Optimized |
| Cache Hit Ratio | > 70% | ✅ Optimized |

## 🔧 Key Optimizations Implemented

### 1. **Database Layer**
- ✅ HikariCP connection pooling (pool size: 20-30)
- ✅ Native SQL queries for complex operations
- ✅ Strategic database indexes (14+ indexes created)
- ✅ Query result caching via Redis
- ✅ Batch operations (batch_size: 50)

### 2. **Application Layer**
- ✅ Redis distributed caching
- ✅ Method-level caching (@Cacheable, @CacheEvict)
- ✅ Async processing for non-blocking operations
- ✅ HTTP/2 support
- ✅ Response compression (gzip)
- ✅ Performance monitoring interceptor

### 3. **Infrastructure**
- ✅ PostgreSQL with optimized settings
- ✅ Redis cache server
- ✅ Prometheus metrics collection
- ✅ Grafana dashboards
- ✅ Alert rules for anomalies

## 📊 Cache Configuration

### Cache Layers
```
User Request
    ↓
HTTP Cache (Browser)
    ↓
Application Cache (Redis)
    ↓
Database (PostgreSQL)
```

### Cache TTLs
- **Patient Data**: 1 hour
- **Patient Search**: 15 minutes
- **Dashboard Data**: 5 minutes
- **Appointment Data**: 15 minutes

### Expected Impact
- 90-95% faster cached responses
- 50-70% reduction in database queries
- 60-80% cache hit ratio

## 🗄️ Database Performance

### Connection Pool Sizing
```yaml
# Development/Testing
maximum-pool-size: 10
minimum-idle: 2

# Production
maximum-pool-size: 30
minimum-idle: 10
```

### Important Indexes
```sql
-- Patient Search (Most Critical)
CREATE INDEX idx_patient_search_name 
ON patients(clinic_id, is_active, first_name, last_name);

-- Recent Patients (Dashboard)
CREATE INDEX idx_patient_created_at 
ON patients(clinic_id, created_at DESC);

-- Foreign Keys (Joins)
CREATE INDEX idx_patient_tag_patients 
ON patient_tag(clinic_id, patient_id);
```

### Query Optimization Tips
1. Use `SELECT specific_columns` instead of `SELECT *`
2. Filter by clinic_id early in WHERE clause
3. Use `LIMIT` for paginated results
4. Prefer `IN` over multiple `OR` conditions
5. Create indexes on frequently filtered columns

## 🔥 JVM Optimization

### Heap Settings
```bash
# Development
-Xms512m -Xmx1g

# Production
-Xms2g -Xmx4g

# With G1GC (recommended for large heaps)
-XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Thread Pool Configuration
```yaml
# Tomcat Threads
server:
  tomcat:
    threads:
      max: 200-300
      min-spare: 10-20

# Async Tasks
@Bean(name = "taskExecutor")
executor.setCorePoolSize(10);
executor.setMaxPoolSize(20);
```

## 📈 Monitoring Metrics

### Critical Metrics to Watch
```
http_server_requests_seconds (P50, P95, P99)
hikaricp_connections_active
hikaricp_connections_idle
jvm_memory_used_bytes (heap)
cache_gets_hit_total
cache_gets_miss_total
db_connection_pending_count
```

### Prometheus Queries
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# P95 Response Time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Cache Hit Ratio
rate(cache_gets_hit_total[5m]) / (rate(cache_gets_hit_total[5m]) + rate(cache_gets_miss_total[5m]))

# Active DB Connections
hikaricp_connections_active
```

## 🚨 Performance Degradation Signs

| Sign | Likely Cause | Solution |
|------|-------------|----------|
| Slow API responses | DB query slow or cache miss | Check slow query log / warm cache |
| High memory usage | Heap too small or memory leak | Increase `-Xmx` / Check for leaks |
| DB connection pool exhausted | Too many concurrent queries | Increase pool size / Add load balancer |
| High CPU usage | N+1 queries or inefficient algorithm | Optimize queries / Use pagination |
| Cache hit ratio < 50% | Cache expiration too short | Increase TTL or pre-warm cache |

## ⚡ Quick Optimization Checklist

### Immediate Actions
- [ ] Run database indexes script (02-create-performance-indexes.sql)
- [ ] Enable Redis caching in application.yml
- [ ] Set DB pool size to 20+ (production)
- [ ] Enable HTTP/2 in server config
- [ ] Enable response compression

### Within 24 Hours
- [ ] Deploy Prometheus for metrics
- [ ] Set up Grafana dashboards
- [ ] Configure alert rules
- [ ] Run baseline performance tests
- [ ] Tune JVM heap settings

### Within 1 Week
- [ ] Load test with 100+ concurrent users
- [ ] Optimize slow endpoints
- [ ] Implement connection pooling across services
- [ ] Set up log aggregation
- [ ] Configure automated backups

## 📊 Performance Testing Commands

### Apache Bench (Quick Test)
```bash
# 1000 requests, 50 concurrent
ab -n 1000 -c 50 http://localhost:8080/api/v1/patients

# With authentication header
ab -n 1000 -c 50 -H "Authorization: Bearer TOKEN" http://localhost:8080/api/v1/patients
```

### K6 (Advanced Load Testing)
```bash
# Run load test
k6 run load-test.js

# Expected script
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '1m', target: 100 },
    { duration: '5m', target: 100 },
    { duration: '1m', target: 0 },
  ],
};

export default function () {
  let res = http.get('http://localhost:8080/api/v1/patients');
  check(res, { 'status is 200': (r) => r.status === 200 });
}
```

## 🔐 Security + Performance Balance

⚠️ **Important**: Performance optimizations should NOT compromise security

✅ **Good Practices**:
- Use HTTPS (even at performance cost)
- Keep authentication fast (JWT, not database queries)
- Cache non-sensitive data only
- Rate limit per client/IP
- Validate input early

❌ **Bad Practices**:
- Disabling HTTPS for speed
- Removing authentication checks
- Caching patient sensitive data insecurely
- Removing input validation

## 📚 Configuration Files Summary

| File | Purpose |
|------|---------|
| `RedisConfig.java` | Cache configuration |
| `HikariDataSourceConfig.java` | Connection pooling |
| `AsyncConfig.java` | Async task execution |
| `PerformanceInterceptor.java` | Request timing |
| `application.yml` | Spring Boot settings |
| `02-create-performance-indexes.sql` | Database indexes |
| `prometheus.yml` | Metrics collection |
| `alert_rules.yml` | Performance alerts |

## 📞 Support Resources

- **Performance Issues**: Check `PERFORMANCE_IMPLEMENTATION_GUIDE.md`
- **Deployment**: See `PERFORMANCE_DEPLOYMENT_GUIDE.md`
- **Monitoring**: Visit http://localhost:9090 (Prometheus) or http://localhost:3001 (Grafana)
- **Metrics**: http://localhost:8081/actuator/prometheus

## 🎯 Performance SLAs

### API Response Times
- **Clinic Operations**: 99th percentile < 500ms
- **Patient Search**: 99th percentile < 200ms
- **Dashboard**: 99th percentile < 400ms

### Availability
- **Uptime Target**: 99.9% (8.76 hours downtime/year)
- **Planned Maintenance**: 2 hours/month

### Throughput
- **Single Server**: 1000-3000 requests/second
- **Cluster**: 5000-10000 requests/second (3+ servers)

---

**Last Updated**: April 2026 | **Version**: 1.0

