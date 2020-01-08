#!/bin/bash
set -euxo pipefail

rm -rf /target
mkdir /target

docker pull $1

TARGET_JAR=$(docker inspect $1 | jq -r '.[0].Config.Entrypoint[-1]')

CONTAINER=$(docker create -ti $1 sh)
docker cp $CONTAINER:${TARGET_JAR} /target/target.jar
docker rm -f $CONTAINER
