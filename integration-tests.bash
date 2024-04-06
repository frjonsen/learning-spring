#! /usr/bin/env bash

: "${HOST=localhost}"
: "${PORT=8080}"
: "${PROD_ID_REVS_RECS=1}"
: "${PROD_ID_NOT_FOUND=13}"
: "${PROD_ID_NO_RECS=113}"
: "${PROD_ID_NO_REVS=213}"

function assertCurl() {
  local expectedHttpCode=$1
  local curlCmd
  curlCmd="curl $2 -w \"%{http_code}\""
  echo "Testing $2"
  local result
  result=$(eval "$curlCmd")
  local httpCode="${result:(-3)}"
  RESPONSE='' && (( ${#result} > 3 )) && RESPONSE="${result%???}"

  if [ "$httpCode" = "$expectedHttpCode" ]
  then
    if [ "$httpCode" = "200" ]
    then
      echo "Test OK (HTTP Code: $httpCode)"
    else
      echo "Test OK (HTTP Code: $httpCode, $RESPONSE)"
    fi
  else
    echo  "Test FAILED, EXPECTED HTTP Code: $expectedHttpCode, GOT: $httpCode, WILL ABORT!"
    echo  "- Failing command: $curlCmd"
    echo  "- Response Body: $RESPONSE"
    exit 1
  fi
}

function assertEqual() {

  local expected=$1
  local actual=$2

  if [ "$actual" = "$expected" ]
  then
    echo "Test OK (actual value: $actual)"
  else
    echo "Test FAILED, EXPECTED VALUE: $expected, ACTUAL VALUE: $actual, WILL ABORT"
    exit 1
  fi
}

function testUrl() {
  url=$@
  if curl "$url" -ks -f -o /dev/null
  then
    return 0
  else
    return 1
  fi;
}

function waitForService() {
  url=$@
  echo -n "Wait for: $url"
  n=0
  until testUrl $url; do
    n=$((n+1))
    if [[ $n == 100 ]]; then
      echo "Service did not start in time"
      exit 1
    else
      sleep 3
      echo -n ", retry #$n"
    fi
  done
  echo "DONE, continues..."
}


set -e

echo "Start testing: $(date --iso-8601=seconds)"

echo "HOST=${HOST}"
echo "PORT=${PORT}"

if [[ $@ == *"start"* ]]; then
  echo "Restarting the test environment..."
  echo "$ docker compose down --remove-orphans"
  sudo docker compose down --remove-orphans
  echo "$ docker compose up -d"
  sudo docker compose up -d
fi

waitForService "http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS"

# Verify tha a normal request works, expect three recommendations and three reviews
assertCurl 200 "http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS "$(echo "$RESPONSE" | jq .productId)"
assertEqual 3 "$(echo "$RESPONSE" | jq ".recommendations | length")"
assertEqual 3 "$(echo "$RESPONSE" | jq ".reviews | length")"

# Verify that a 404 (Not Found) error is returned for a non-existing productId ($PROD_ID_NOT_FOUND)
assertCurl 404 "http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo "$RESPONSE" | jq -r .message)"

# Verify that no recommendations are returned for productId $PROD_ID_NO_RECS
assertCurl 200 "http://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS "$(echo "$RESPONSE" | jq .productId)"
assertEqual 0 "$(echo "$RESPONSE" | jq ".recommendations | length")"
assertEqual 3 "$(echo "$RESPONSE" | jq ".reviews | length")"

# Verify that no reviews are returned for productId $PROD_ID_NO_REVS
assertCurl 200 "http://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS "$(echo "$RESPONSE" | jq .productId)"
assertEqual 3 "$(echo "$RESPONSE" | jq ".recommendations | length")"
assertEqual 0 "$(echo "$RESPONSE" | jq ".reviews | length")"

# Verify that a 422 (Unprocessable Entity) error is returned for a productId that is out of range (-1)
assertCurl 422 "http://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo "$RESPONSE" | jq .message)"

# Verify that a 400 (Bad Request) error error is returned for a productId that is not a number, i.e. invalid format
assertCurl 400 "http://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo "$RESPONSE" | jq .message)"

if [[ $@ == *"stop"* ]]
then
    echo "We are done, stopping the test environment..."
    echo "$ docker compose down"
    sudo docker compose down
fi

echo "End, all tests OK: $(date --iso-8601=seconds)"
