# Multi-stage Docker build for clinic-patient
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Install cloud-sql-proxy for connecting to Cloud SQL
RUN apk add --no-cache curl

COPY target/clinic-patient.jar app.jar

EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8081/health || exit 1

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]

