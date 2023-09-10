#!/bin/bash
source setenv-kafka.sh


function verifyDirectory() {
  deployDirectory="/home/$LXC_USER/deploy"
  if [ "$PWD" != "$deployDirectory" ]; then
    echo "Aborted: File has to be run from $deployDirectory but was run from $PWD"
    exit
  fi
}

function setNohupFileName() {
  nohupFile="/home/$LXC_USER/deploy/nohup_kafka.out"
  if test -f "$nohupFile"; then
      echo "Rename previous nohup_kafka"
      mkdir -p old_nohup
      mv nohup_kafka.out old_nohup/nohup_kafka_"$(date +'%Y-%m-%d_%H:%M:%S')".out
  fi
}

function checkIfRunning() {
  versionFile="/home/$LXC_USER/deploy/$COMMON_VERSION_FILE"
  if test -f "$versionFile"; then
      echo "Application running aborting..."
      exit
  fi
}

# Set the version
version=Kafka
if [[ ${1+x} ]]
then
    version=$1
fi

# Persist version running
verifyDirectory
checkIfRunning
versionFile="$COMMON_VERSION_FILE"
touch "$versionFile"
echo "$version" > "$versionFile"

# Start the application with specified version
setNohupFileName
echo "Starting Application: $version"

KAFKA_VERSION="kafka_2.13-3.3.1"
KAFKA_CLUSTER_ID="$($KAFKA_VERSION/bin/kafka-storage.sh random-uuid)"
$KAFKA_VERSION/bin/kafka-storage.sh format -t "$KAFKA_CLUSTER_ID" -c $KAFKA_VERSION/config/kraft/server.properties
nohup $KAFKA_VERSION/bin/kafka-server-start.sh $KAFKA_VERSION/config/kraft/server.properties &> nohup_kafka.out &

