FROM maven:3.5.3-jdk-8-slim AS builder

COPY uuid-benchmark /uuid-benchmark

WORKDIR /uuid-benchmark

RUN mkdir out && \
    /uuid-benchmark/resources/scripts/build-app.sh

FROM openjdk:8u121-jre-alpine

RUN mkdir /uuid-benchmark

COPY --from=builder /uuid-benchmark/target/uuid-benchmark.jar /uuid-benchmark

WORKDIR /uuid-benchmark

CMD ["tail", "-f", "/dev/null"]