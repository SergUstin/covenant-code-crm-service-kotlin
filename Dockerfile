FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

# Cache dependency resolution separately from source compilation
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon -q 2>/dev/null || true

COPY src ./src
RUN gradle bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/build/libs/*.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh", "java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
