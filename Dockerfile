FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system eventix \
    && useradd --system --gid eventix --home-dir /app eventix

WORKDIR /app

COPY --from=build --chown=eventix:eventix \
    /workspace/target/eventix-0.1.0-SNAPSHOT.jar \
    /app/eventix.jar

USER eventix

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/eventix.jar"]
