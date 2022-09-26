#!/bin/bash

printf "\n# Force-deleting namespaces Tilt or Skaffold didn't clean up\n"
printf "################################################################################\n\n"

kubectl delete namespace opennms --grace-period=0
kubectl delete namespace kafka --grace-period=0
kubectl delete namespace tilt-instance --grace-period=0
kubectl delete namespace skaffold-instance --grace-period=0
kubectl delete namespace local-instance --grace-period=0
kubectl delete namespace skaffold-operator --grace-period=0
