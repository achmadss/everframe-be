# Stage 1: Build Application
FROM gradle:8.3.0-jdk17-alpine AS build
WORKDIR /home/gradle/app

# Use alpine-based image and install only essential build dependencies
RUN apk add --no-cache bash

# Copy only the files needed for dependency resolution first
COPY build.gradle.* gradle.properties settings.gradle* gradle/ ./
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true

# Copy source and build
COPY . .
RUN gradle buildFatJar --no-daemon --parallel

# Stage 2: Create a minimal runtime image
FROM alpine:3.19
WORKDIR /app

# Install minimal JRE and SSL certificates
RUN apk add --no-cache openjdk17-jre-headless tzdata

# Copy only the final JAR
COPY --from=build /home/gradle/app/build/libs/*.jar /app/everframe.jar

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Configure Java options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -jar /app/everframe.jar
