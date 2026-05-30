# Reporting Shadow Deployment (Read Replica)

Purpose: Serve dashboard/reporting endpoints from a lightweight instance that reads ONLY from the PostgreSQL replica.

## 1. Database Role (Run on Primary)
```sql
CREATE ROLE report_reader LOGIN PASSWORD 'CHANGE_ME_STRONG_PWD';
GRANT CONNECT ON DATABASE postgres TO report_reader;
GRANT USAGE ON SCHEMA public TO report_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO report_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO report_reader;
```
(Optional) If materialized views are created on replica only:
```sql
GRANT SELECT, REFRESH ON MATERIALIZED VIEW mv_name TO report_reader;
```

## 2. Required Environment Variables
```
REPORTING_DB_URL=jdbc:postgresql://<replica-host>:5431/postgres?sslmode=require
REPORTING_DB_USERNAME=report_reader
REPORTING_DB_PASSWORD=********
REPORTING_DB_POOL_SIZE=10
REPORTING_SERVER_PORT=9091
REPLICA_LAG_ALERT_SECONDS=30
```

## 3. Launch (Local)
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=reporting -Dspring-boot.run.mainClass=com.thedal.thedal_app.reporting.ReportingApplication
```

## 4. Docker (Example Snippet)
```dockerfile
# Existing build stage assumed
FROM eclipse-temurin:21-jre as runtime
WORKDIR /app
COPY target/Thedalapp.jar app.jar
ENV SPRING_PROFILES_ACTIVE=reporting \
    REPORTING_DB_URL=jdbc:postgresql://replica:5431/postgres?sslmode=require
EXPOSE 9091
ENTRYPOINT ["java","-Dspring.main.sources=com.thedal.thedal_app.reporting.ReportingApplication","-jar","/app/app.jar"]
```

## 5. Health & Lag
- Lag endpoint: GET /reporting/internal/replica/lag
Response example:
```json
{ "lagSeconds": 2, "state": "HEALTHY", "alertThresholdSeconds": 30 }
```
If lagSeconds = -1 => UNKNOWN (function not available or not replica).

## 6. Cutover Plan
1. Deploy reporting instance.
2. Verify endpoints (compare JSON with existing app responses for sample electionIds).
3. Monitor replica lag (target < 5s, alert if > threshold).
4. Frontend feature flag: switch dashboard API base URL to /reporting.
5. Observe error rates & latency for 24h.
6. Decommission old dashboard calls (or keep fallback for one release).

## 7. Logging & Metrics
Recommended additions (future):
- Add Micrometer timer for each report endpoint.
- Log slow queries (>300ms) via datasource proxy or p6spy (optional).

## 8. Security Notes
- Do NOT reuse superuser credentials.
- Enforce network ACL: only app subnets can reach replica port.
- Consider secret manager integration (Vault / AWS Secrets Manager) in production.

## 9. Materialized Views (Later)
Example turnout aggregation skeleton:
```sql
CREATE MATERIALIZED VIEW mv_cadre_overview AS
SELECT count(*) FILTER (WHERE active)      AS active_cadre,
       count(*) FILTER (WHERE NOT active)  AS inactive_cadre,
       count(*) FILTER (WHERE gender='M')  AS male_cadre,
       count(*) FILTER (WHERE gender='F')  AS female_cadre
FROM cadre; -- adapt to real table names
```
Refresh job (cron or external):
```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_cadre_overview;
```

## 10. Rollback
- Frontend revert feature flag to original endpoints.
- Stop reporting container.
- No schema changes made to primary (nothing to undo).

## 11. Future Extraction to Dedicated Service
- Copy `report` + `reporting` packages to new repo.
- Replace JPA-heavy logic with native SQL where beneficial.
- Introduce caching (Redis) for hot aggregates.

---
Maintainer Checklist Before Go-Live:
- [ ] Read-only credentials validated
- [ ] Lag endpoint returns HEALTHY
- [ ] Sample responses validated
- [ ] Security scanning passed
- [ ] Observability dashboards ready (CPU, memory, DB connections, lag)

## 12. Security Hardening Roadmap
Phase A (current): All endpoints permitted (shadow validation). NOT for public internet.
Phase B: Add JWT filter (reuse existing token validator) – restrict to roles with dashboard scope.
Phase C: Rate limit poll-day high-frequency endpoints (Redis token bucket).
Phase D: Add response metadata: { "dataLagSeconds": n } for UI freshness banner.

Example JWT Integration Sketch (future):
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/internal/replica/lag").hasRole("OPS")
    .anyRequest().hasAuthority("SCOPE_DASHBOARD_READ"));
```

## 13. Staging Deployment
Profiles: `reporting,staging`

Run (Maven):
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=reporting,staging -Dspring-boot.run.mainClass=com.thedal.thedal_app.reporting.ReportingApplication
```

Container (env excerpt):
```
SPRING_PROFILES_ACTIVE=reporting,staging
REPORTING_DB_URL=jdbc:postgresql://<replica-host>:5431/postgres?sslmode=require
REPORTING_DB_USERNAME=report_reader
REPORTING_DB_PASSWORD=********
```

Fallback behavior: If REPORTING_DB_* not set, staging overlay will fall back to DB_URL / DB_USERNAME / DB_PASSWORD (ensure those point to REPLICA, not primary, when using reporting service).

Validation checklist staging:
- [ ] /reporting/internal/replica/lag shows state=HEALTHY
- [ ] GET /reporting/cadre/overview/{electionId} returns data
- [ ] Load test (50 rps) stable latency
- [ ] No write queries detected (check DB logs)



