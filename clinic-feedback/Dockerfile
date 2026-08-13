FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S clinicos && adduser -S clinicos -G clinicos
RUN mkdir -p /app/logs && chown -R clinicos:clinicos /app

COPY target/clinic-feedback.jar app.jar
RUN chown clinicos:clinicos app.jar

USER clinicos
EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q --spider http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

