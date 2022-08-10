#!/bin/bash

printf "\n# Create Kind cluster\n"
printf "################################################################################\n\n"

kind create cluster --config=./local-sample/config-kind.yaml

printf "\n\n# Confirm connection\n"

kubectl config use-context kind-kind
kubectl config get-contexts

printf "\n# Add Dependencies\n"
printf "################################################################################\n\n"

# Add Dependency - Ingress Nginx # FIXME: Do we need this?
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

until kubectl -n ingress-nginx wait --for=condition=ready pod --timeout=120s -l app.kubernetes.io/component=controller --timeout=90s 2> /dev/null
do
    sleep 5
    echo Waiting for dependencies to start....
done

printf "\n\n# Install OLM\n" # FIXME: Do we need this?
printf "################################################################################\n\n"

operator-sdk olm install

