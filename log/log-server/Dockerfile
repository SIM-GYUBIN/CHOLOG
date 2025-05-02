FROM gradle:7.6.1-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test --no-daemon

FROM openjdk:17-slim
WORKDIR /app
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
COPY --from=builder /app/build/libs/log-server-0.0.1-SNAPSHOT.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]