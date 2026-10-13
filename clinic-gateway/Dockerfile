FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN apk add --no-cache curl

COPY target/clinic-gateway.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/ || exit 1

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]

