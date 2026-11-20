# Build stage
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

# Cache dependencies separately
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Build and test the application
COPY src ./src
RUN mvn -B -ntp clean package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run the application as a non-root user
RUN addgroup -S app && adduser -S app -G app

COPY --from=build --chown=app:app \
    /workspace/target/*.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]