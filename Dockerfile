FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system eventix \
    && useradd --system --gid eventix --home-dir /app eventix \
    && mkdir -p /app/data \
    && chown -R eventix:eventix /app

WORKDIR /app

COPY --from=build --chown=eventix:eventix \
    /workspace/target/eventix.jar \
    /app/eventix.jar

USER eventix

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/eventix.jar"]
