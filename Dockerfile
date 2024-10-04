FROM gradle:8.10.0-jdk21 AS builder

FROM eclipse-temurin:21.0.2_13-jre AS runtime

FROM builder AS build
WORKDIR /app
ADD . .
RUN gradle --no-daemon assemble

FROM builder AS development
WORKDIR /app

FROM runtime AS production
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/London
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

RUN addgroup --gid 2000 --system appgroup && \
    adduser --uid 2000 --system appuser --gid 2000

WORKDIR /app
COPY --from=build --chown=appuser:appgroup /app/build/libs/hmpps-assess-risks-and-needs-handover-service*.jar /app/app.jar
COPY --from=build --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=build --chown=appuser:appgroup /app/applicationinsights.json /app
COPY --from=build --chown=appuser:appgroup /app/applicationinsights.dev.json /app

USER 2000

ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
