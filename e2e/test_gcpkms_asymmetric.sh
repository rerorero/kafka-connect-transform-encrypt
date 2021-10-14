#!/bin/bash

# This test doesn't automatically run becase it requries actual Cloud KMS running.
# GCP_PROJECT_ID=<project> GOOGLE_APPLICATION_CREDENTIALS=<path to credential json file> ./test_gcpkms.sh

set -eux

cat "${GOOGLE_APPLICATION_CREDENTIALS}" > ./gcp_sa.json

wait-for-url() {
    echo "Testing $1"
    timeout -s TERM 60 bash -c \
    'while [[ "$(curl -s -o /dev/null -L -w ''%{http_code}'' ${0})" != "${1}" ]];\
    do echo "Waiting for ${0}" && sleep 2;\
    done' ${1} ${2}
    echo "OK!"
    curl -I $1
}

# start services
docker-compose up -d --build connect
trap 'docker-compose down' EXIT

# -------------------------------------------
# wait for vault and connector ready
wait-for-url http://127.0.0.1:8200/v1/secret/test 400
wait-for-url http://127.0.0.1:8083/ 200

# start connect
envsubst '$GCP_PROJECT_ID' < gcpkms_config_asymmetric.json | curl -X POST -H "Content-Type: application/json" --data @- http://localhost:8083/connectors

# subscribe and check the cipher text. It should respond like below if it works:
# 1	{"viewtime":91,"userid":"User_3","pageid":"Page_68"}
docker-compose exec -T connect kafka-console-consumer --topic pageviews-gcpkms --bootstrap-server kafka:29092  --property print.key=true --max-messages 1 --from-beginning --timeout-ms 10000 | grep '"userid":"User_'
docker-compose exec -T connect kafka-console-consumer --topic pageviews-gcpkms --bootstrap-server kafka:29092  --property print.key=true --max-messages 1 --from-beginning --timeout-ms 10000 | grep '"pageid":"Page_'
