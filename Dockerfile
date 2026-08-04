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

# JAVA_OPTS only takes effect because the ENTRYPOINT below expands it — `java -jar` does
# NOT read it (only Spring Boot's script launcher does), so the old CMD ran with the JVM's
# ergonomic default: 25% of HOST RAM, ~3.9g on the 15.5GB deploy box. Confirmed live, the
# container's command line was bare `java -jar /app/app.jar`.
#
# Heap 1g, not the never-applied 512m — switching that on for the first time would be an
# untested tightening; measured prod RSS is ~874MB.
#
# DNS: the JDBC pool re-resolves ksh-postgres on every reconnect and maxAge (10 min)
# recycles connections forever even at zero traffic, while the JVM caches a good lookup
# for only 30s. On 2026-07-27 a co-tenant container OOM-starved the host, dockerd's
# embedded resolver started failing, and this app logged UnknownHostException from the
# pool cleaner. preferIPv4Stack matters more here than on the glibc images: musl fires A
# and AAAA in parallel, and asking for A only sidesteps that entirely.
ENV LANG=C.UTF-8 \
    GRAILS_ENV=production \
    JAVA_OPTS="-Xms256m -Xmx1g -Djava.net.preferIPv4Stack=true -Dnetworkaddress.cache.ttl=60 -Dnetworkaddress.cache.negative.ttl=0" \
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

# Health check. '/' renders without touching the database, so an HTTP-only check reports
# healthy while the app cannot reach Postgres at all — that is how the 2026-07-27 DNS
# outage stayed invisible. Also assert the DB host still resolves, parsed out of
# DATABASE_URL so nothing is hardcoded; skipped when DATABASE_URL is unset (dev/H2).
# Detection only — `--restart unless-stopped` does not act on health, so it shows up in
# `docker ps` rather than self-healing.
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8080/ >/dev/null || exit 1; \
        DBH=$(printf '%s' "${DATABASE_URL:-}" | sed -n 's#^jdbc:postgresql://\([^:/?]*\).*#\1#p'); \
        [ -z "$DBH" ] || getent hosts "$DBH" >/dev/null || exit 1

# Start the application. Shell form so $JAVA_OPTS is expanded; `exec` so the JVM still
# becomes PID 1 and receives SIGTERM directly on `docker stop`.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
