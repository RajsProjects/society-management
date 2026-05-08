#!/bin/sh
# Copy config to volume
cp /tmp/prometheus.yml /etc/prometheus/prometheus.yml
# Start prometheus
/bin/prometheus "$@"
