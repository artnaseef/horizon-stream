#!/bin/bash

docker pull docker.io/bitnami/kafka:3
docker pull docker.io/bitnami/zookeeper:3.7
docker pull mailhog/mailhog:latest
docker pull nginx:1.21.6-alpine
docker pull opennms/minion:29.0.10
docker pull postgres:13.3-alpine
docker pull prom/prometheus
docker pull prom/pushgateway
docker pull busybox

kind load docker-image docker.io/bitnami/kafka:3
kind load docker-image docker.io/bitnami/zookeeper:3.7
kind load docker-image mailhog/mailhog:latest
kind load docker-image nginx:1.21.6-alpine
kind load docker-image opennms/minion:29.0.10
kind load docker-image postgres:13.3-alpine
kind load docker-image prom/prometheus
kind load docker-image prom/pushgateway
kind load docker-image busybox

kind load docker-image "opennms/grafana-dev:local-basic"
kind load docker-image "opennms/horizon-stream-core:local-basic"
kind load docker-image "opennms/horizon-stream-minion:local-basic"
kind load docker-image "opennms/horizon-stream-ui:local-basic"
kind load docker-image "opennms/horizon-stream-keycloak-dev:local-basic"
kind load docker-image "opennms/horizon-stream-inventory:local-basic"
kind load docker-image "opennms/horizon-stream-notification:local-basic"
kind load docker-image "opennms/horizon-stream-api:local-basic"
kind load docker-image "opennms/horizon-stream-minion-gateway:local-basic"

