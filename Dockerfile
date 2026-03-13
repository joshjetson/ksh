FROM amazoncorretto:11-alpine-jdk AS builder

ENV GRAILS_ENV=production

# Install build dependencies
RUN apk add --no-cache \
    tzdata \
    build-base \
    gcompat \
    curl \
    bash

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew build.gradle gradle.properties /app/

# Download Gradle dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY . /app

# Build the application JAR
RUN ./gradlew assemble -Dgrails.env=production --no-daemon \
    && find . -name "*.tmp" -delete \
    && rm -rf ~/.gradle/caches/*/tmp \
    && rm -rf build/tmp

FROM amazoncorretto:11-alpine AS runtime

ARG COMMIT_HASH

ENV LANG=C.UTF-8 \
    GRAILS_ENV=production \
    JAVA_OPTS="-Xmx512m -Xms256m" \
    COMMIT_HASH=$COMMIT_HASH

LABEL service="korean-school-house"

# Install runtime dependencies
RUN apk add --no-cache \
    tzdata \
    curl \
    bash \
    gcompat

WORKDIR /app

EXPOSE 8080

# Create app user for security
RUN addgroup -g 1000 -S app && adduser -u 1000 -S app -G app

# Copy built application from builder stage
COPY --from=builder --chown=app:app /app/build/libs/*.jar /app/app.jar

# Create directories and set permissions for data persistence
RUN mkdir -p /app/logs /app/storage /app/data \
    && chown -R app:app /app

# Create volume mount points
VOLUME ["/app/storage", "/app/data"]

# Switch to non-root user
USER app

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/ || exit 1

# Start the application
CMD ["java", "-jar", "/app/app.jar"]
