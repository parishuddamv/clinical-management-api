FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN apk add --no-cache curl

COPY target/clinic-appointment.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8082/health || exit 1

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]

