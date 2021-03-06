#!/bin/bash
set -euxo pipefail

mvn clean install -DskipTests

pushd application
docker build -t diptadas/rad-analysis .
docker push diptadas/rad-analysis
popd

pushd sample/sample-one
docker build -t diptadas/rad-sample-one .
docker push diptadas/rad-sample-one
popd

pushd sample/sample-two
docker build -t diptadas/rad-sample-two .
docker push diptadas/rad-sample-two
popd

minikube start

kubectl delete -f application/deploy.yaml || true
kubectl delete -f sample/sample-one/deploy.yaml || true
kubectl delete -f sample/sample-two/deploy.yaml || true

kubectl create rolebinding default-view --clusterrole=view --serviceaccount=default:default || true

kubectl apply -f application/deploy.yaml
kubectl apply -f sample/sample-one/deploy.yaml
kubectl apply -f sample/sample-two/deploy.yaml
