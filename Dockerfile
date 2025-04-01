# Stage 1: Build Application
FROM gradle:8.4.0-jdk17-alpine AS build
WORKDIR /home/gradle/app

# Only install essential build dependencies
RUN apk add --no-cache bash

# Implement better layer caching for dependencies
COPY gradle/ ./gradle/
COPY build.gradle.* settings.gradle* gradle.properties ./
RUN gradle --no-daemon dependencies

# Copy source and build with specific target
COPY . .
RUN gradle buildFatJar --no-daemon --parallel -x test

# Stage 2: Runtime with minimal image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add only necessary packages
RUN apk add --no-cache tzdata

# Copy only the final JAR
COPY --from=build /home/gradle/app/build/libs/*.jar /app/everframe.jar

# Create a dedicated user for better security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app
USER appuser

# Configure optimized JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Add healthcheck
HEALTHCHECK --interval=30s --timeout=5s CMD wget -q --spider http://localhost:8080/health || exit 1

EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -jar /app/everframe.jar