# syntax=docker/dockerfile:1

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Tải dependency trước, tách layer để sửa code không phải tải lại.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Chạy bằng user thường, không phải root.
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /data && chown app:app /data
WORKDIR /app

COPY --from=build --chown=app:app /build/target/*.jar app.jar

USER app
EXPOSE 8099
VOLUME ["/data"]

ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8099/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
