# ClinicOS Production Deployment Checklist

## Pre-Deployment Phase

### 1. Code & Build
- [ ] All tests passing (unit + integration)
- [ ] Code review completed
- [ ] No security vulnerabilities in dependencies
- [ ] Build artifact created successfully
- [ ] Docker images built and tested locally
- [ ] Images pushed to container registry

### 2. Configuration
- [ ] JWT_SECRET generated (32+ characters)
- [ ] Database password set to secure value (12+ mixed case)
- [ ] Environment variables documented
- [ ] .env file configured for production
- [ ] Database connection string verified
- [ ] CORS origins updated to production domain
- [ ] Application profile set to "prod"

### 3. Security
- [ ] SSL/TLS certificate obtained
- [ ] Certificate installed and tested
- [ ] Security headers configured
- [ ] CORS configuration locked down
- [ ] Input validation on all endpoints verified
- [ ] SQL injection prevention verified
- [ ] Authentication/authorization tested
- [ ] Rate limiting configured
- [ ] Secrets in secure vault (not in code/config)
- [ ] Security scanning completed (no critical issues)

### 4. Database
- [ ] PostgreSQL instance created
- [ ] Database created with correct name
- [ ] Database user created with strong password
- [ ] Encryption at rest enabled
- [ ] SSL connection required
- [ ] Backup configured (automated daily)
- [ ] Backup restoration tested
- [ ] Database indexes optimized
- [ ] Connection pool size configured
- [ ] Flyway migrations validated

### 5. Infrastructure
- [ ] Cloud resources provisioned (Cloud Run, Cloud SQL)
- [ ] VPC/Network configured
- [ ] Firewall rules set
- [ ] Load balancer configured (if needed)
- [ ] CDN configured (if needed)
- [ ] DNS records updated
- [ ] Storage buckets created (if needed)
- [ ] IAM roles and permissions set

### 6. Monitoring & Logging
- [ ] Logging service configured
- [ ] Centralized logging enabled
- [ ] Monitoring dashboards created
- [ ] Alerts configured (errors, latency, availability)
- [ ] APM/tracing service configured
- [ ] Log aggregation verified
- [ ] Metrics endpoints accessible
- [ ] Health check endpoints verified

### 7. Documentation
- [ ] Deployment procedure documented
- [ ] Rollback procedure documented
- [ ] Runbook created
- [ ] Contact information updated
- [ ] Escalation path defined
- [ ] On-call schedule setup
- [ ] Incident response plan created

---

## Deployment Phase

### Pre-Deployment
- [ ] Team aligned on deployment time
- [ ] Backup of current production created
- [ ] Maintenance window scheduled
- [ ] Notification sent to users (if applicable)
- [ ] Final smoke tests completed
- [ ] Deployment checklist printed/reviewed

### Deployment Execution
- [ ] Deploy clinic-patient service
  - [ ] Verify deployment successful
  - [ ] Check health endpoints
  - [ ] Monitor logs for errors
  - [ ] Test core functionality

- [ ] Deploy clinic-appointment service
  - [ ] Verify deployment successful
  - [ ] Check health endpoints
  - [ ] Monitor logs for errors
  - [ ] Test core functionality

- [ ] Deploy clinic-followup service
  - [ ] Verify deployment successful
  - [ ] Check health endpoints
  - [ ] Monitor logs for errors
  - [ ] Test core functionality

- [ ] Deploy clinic-notification service
  - [ ] Verify deployment successful
  - [ ] Check health endpoints
  - [ ] Monitor logs for errors
  - [ ] Test email sending

- [ ] Deploy clinic-billing service
  - [ ] Verify deployment successful
  - [ ] Check health endpoints
  - [ ] Monitor logs for errors
  - [ ] Test invoice generation

- [ ] Deploy clinic-gateway service
  - [ ] Verify deployment successful
  - [ ] Check health endpoints
  - [ ] Monitor logs for errors
  - [ ] Test JWT authentication
  - [ ] Test OAuth2 Google login
  - [ ] Test CORS headers

### Post-Deployment Verification
- [ ] All services running and healthy
- [ ] Database migrations completed successfully
- [ ] API endpoints responding correctly
- [ ] JWT authentication working
- [ ] OAuth2 login working
- [ ] Email notifications sending
- [ ] No error spikes in logs
- [ ] Response times within SLA
- [ ] Database performance normal

---

## Post-Deployment Phase

### Immediate (1 hour)
- [ ] Monitor error rates and logs
- [ ] Check database connection pool
- [ ] Verify all health checks passing
- [ ] Test critical user flows
- [ ] Check API response times
- [ ] Verify backup is running
- [ ] Monitor resource utilization
- [ ] Check for any 500 errors
- [ ] Team stands by for rollback

### Short Term (24 hours)
- [ ] Review all logs for errors
- [ ] Check performance metrics
- [ ] Verify database consistency
- [ ] Test failover/recovery (if applicable)
- [ ] Performance optimization review
- [ ] User feedback collection
- [ ] Incident tickets (if any) reviewed

### Ongoing (1 week)
- [ ] Monitor for degradation
- [ ] Review cost metrics
- [ ] Check security logs
- [ ] Verify backup integrity
- [ ] Performance trending analysis
- [ ] User issue resolution
- [ ] Documentation updates

---

## Rollback Plan

### Quick Rollback Procedure
1. [ ] Decision made to rollback
2. [ ] Previous version docker images verified
3. [ ] All services stopped
4. [ ] Database rolled back (if needed)
5. [ ] Previous services deployed
6. [ ] Health checks verified
7. [ ] Critical tests executed
8. [ ] Users notified
9. [ ] Post-incident review scheduled

### Rollback Testing
- [ ] Rollback procedure tested in staging
- [ ] Database rollback tested
- [ ] Service startup verified
- [ ] Time to rollback measured
- [ ] Rollback runbook created

---

## Critical Services Verification

### clinic-gateway (Port 8080)
- [ ] Service responding to health check
- [ ] JWT validation working
- [ ] OAuth2 callback working
- [ ] CORS headers present
- [ ] Rate limiting active
- [ ] Request routing working

### clinic-patient (Port 8081)
- [ ] Service responding to health check
- [ ] Patient registration endpoint working
- [ ] Patient search working
- [ ] Multi-tenancy enforced
- [ ] Database queries performing well

### clinic-appointment (Port 8082)
- [ ] Service responding to health check
- [ ] Appointment scheduling working
- [ ] Conflict detection working
- [ ] Notifications triggering

### clinic-followup (Port 8083)
- [ ] Service responding to health check
- [ ] Follow-up creation working
- [ ] Reminders triggering

### clinic-notification (Port 8084)
- [ ] Service responding to health check
- [ ] Email sending working
- [ ] Email templates loading
- [ ] SMS integration (if applicable)

### clinic-billing (Port 8085)
- [ ] Service responding to health check
- [ ] Invoice generation working
- [ ] Payment recording working
- [ ] Report generation working

---

## Security Verification Post-Deployment

- [ ] HTTPS working on all endpoints
- [ ] HTTP redirects to HTTPS
- [ ] SSL certificate valid (not expired)
- [ ] Security headers present
- [ ] CORS restricted to allowed origins
- [ ] Secrets not exposed in logs
- [ ] Authentication required for protected endpoints
- [ ] Authorization enforced correctly
- [ ] Multi-tenancy isolation verified
- [ ] Audit logging working
- [ ] No sensitive data in responses

---

## Performance Verification

- [ ] Average response time < 500ms
- [ ] P99 response time < 2s
- [ ] Error rate < 0.1%
- [ ] CPU usage < 70%
- [ ] Memory usage normal
- [ ] Database connection pool healthy
- [ ] No slow queries
- [ ] Cache hit rate acceptable
- [ ] Throughput meeting targets

---

## Compliance & Audit

- [ ] All changes documented
- [ ] Deployment log created
- [ ] Version tags applied
- [ ] Change request closed
- [ ] Audit trail recorded
- [ ] Compliance checklist verified
- [ ] Security review completed

---

## Communication

- [ ] Stakeholders notified of deployment
- [ ] Users notified of any changes
- [ ] Support team trained on new features
- [ ] Documentation updated
- [ ] Runbook distributed
- [ ] Team debriefing scheduled
- [ ] Post-incident review scheduled (if issues)

---

## Sign-Off

- [ ] Project Lead: _______________ Date: _______
- [ ] DevOps Lead: _______________ Date: _______
- [ ] Security Lead: _______________ Date: _______
- [ ] Operations Lead: _______________ Date: _______

---

## Deployment Summary

**Deployment Date & Time**: _______________  
**Deployed Version**: _______________  
**Deployed By**: _______________  
**Duration**: _______________  
**Issues Encountered**: _______________  
**Rollback Required**: [ ] Yes [ ] No  
**Overall Status**: [ ] Success [ ] Partial [ ] Failed  

**Notes**:
```
___________________________________________________________________
___________________________________________________________________
___________________________________________________________________
```

---

**For questions or issues, contact: support@clinicos.health**

