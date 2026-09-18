#!/bin/sh

set -eu

for topic in \
    plantos.machine.telemetry.v1 \
    plantos.machine.health.v1 \
    plantos.machine.anomaly.v1
do
    docker compose exec -T kafka \
        /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --create \
        --if-not-exists \
        --topic "$topic" \
        --partitions 3 \
        --replication-factor 1
done
