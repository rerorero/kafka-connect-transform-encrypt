#!/bin/bash

set -eux

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

# setup vault
docker-compose exec -T vault vault secrets enable transit
docker-compose exec -T vault vault write -f transit/keys/mykey

# start connect
curl -X POST -H "Content-Type: application/json" --data @vault_config.json http://localhost:8083/connectors

# subscribe and check the cipher text. userid field is encrypted then decrypted, and pageid field is just encrypted.
# It should respond like below if it works:
# 1	{"viewtime":91,"userid":"User_3","pageid":"vault:v1:E18m0K2uZZ7rGOm5YZDIMP3t5H/53i7v9OBnuyFQ5f+VFKU="}
docker-compose exec -T connect kafka-console-consumer --topic pageviews-vault --bootstrap-server kafka:29092  --property print.key=true --max-messages 1 --from-beginning --timeout-ms 10000 | grep '"userid":"User_'
docker-compose exec -T connect kafka-console-consumer --topic pageviews-vault --bootstrap-server kafka:29092  --property print.key=true --max-messages 1 --from-beginning --timeout-ms 10000 | grep '"pageid":"vault:v1'

# -------------------------------------------
# wait for AWS KMS
wait-for-url http://127.0.0.1:8080/ 405

# create a key
export KEYARN=$(curl -s -X POST -H "X-Amz-Target: TrentService.CreateKey" -H "Content-Type:  application/x-amz-json-1.1" --data '' http://localhost:8080/ | jq -r .KeyMetadata.Arn)

# start connect
envsubst '$KEYARN' < awskms_config.json | curl -X POST -H "Content-Type: application/json" --data @- http://localhost:8083/connectors

# subscribe and check the cipher text. It should respond like below if it works:
# 1	{"viewtime":91,"userid":"User_3","pageid":"Page_68"}
docker-compose exec -T connect kafka-console-consumer --topic pageviews-awskms --bootstrap-server kafka:29092  --property print.key=true --max-messages 1 --from-beginning --timeout-ms 10000 | grep '"userid":"User_'
docker-compose exec -T connect kafka-console-consumer --topic pageviews-awskms --bootstrap-server kafka:29092  --property print.key=true --max-messages 1 --from-beginning --timeout-ms 10000 | grep '"pageid":"Page_'
