FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace
COPY gradlew settings.gradle ./
COPY gradle ./gradle
COPY backend ./backend
RUN chmod +x gradlew \
    && ./gradlew :backend:ainews-server:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S ainews \
    && adduser -S -G ainews -h /app ainews

WORKDIR /app
COPY --from=builder --chown=ainews:ainews /workspace/backend/ainews-server/build/libs/*.jar app.jar

USER ainews
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
