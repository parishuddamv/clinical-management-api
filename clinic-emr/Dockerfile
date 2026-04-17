FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add a non-root user
RUN addgroup -S clinicos && adduser -S clinicos -G clinicos

# Create directories for logs and uploads
RUN mkdir -p /app/logs /app/uploads && chown -R clinicos:clinicos /app

# Copy the JAR file
COPY target/clinic-emr.jar app.jar

# Set ownership
RUN chown clinicos:clinicos app.jar

USER clinicos

# Expose port
EXPOSE 8086

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q --spider http://localhost:8086/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

