set -x

mkdir /target

TARGET_JAR=$(docker inspect $1 | jq -r '.[0].Config.Entrypoint[-1]')

docker create -ti --name dummy $1 bash
docker cp dummy:${TARGET_JAR} /target/target.jar
docker rm -f dummy

