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
docker-compose up -d

# wait for vault and connector ready
wait-for-url http://127.0.0.1:8200/v1/secret/test 400
wait-for-url http://127.0.0.1:8083/ 200

# setup vault
docker-compose exec vault vault secrets enable transit
docker-compose exec vault vault write -f transit/keys/mykey

# start connect
curl -X POST -H "Content-Type: application/json" --data @connect_vault.json http://localhost:8083/connectors

# subscribe and check the cipher text. It should respond like below if it works:
# 1	{"viewtime":1,"userid":"vault:v1:xAmo45WN6LGjdaSJO/v9+KNI5edZa7pBKnb9ShaaDoiEEQ==","pageid":"vault:v1:LoGTzS0o0XYQ+xQ+O//6PrWD3RdDjlWjsjV/e9AH1HFAJQo="}
# docker-compose exec connect kafka-console-consumer --topic pageviews --bootstrap-server kafka:29092  --property print.key=true --max-messages 1 --from-beginning | grep '"userid":"vault:v1'
docker-compose exec connect kafka-console-consumer --topic pageviews --bootstrap-server kafka:29092  --property print.key=true --max-messages 1 --from-beginning | grep '"userid":"vault:v2'

# clean up
docker-compose down
