#! /usr/bin/env bash

[ -z "$SUBSCRIPTIONS_URL" ] && export SUBSCRIPTIONS_URL="https://sub.thegulocal.com"
[ -z "$PASSTHROUGH_COOKIE_VALUE" ] && export PASSTHROUGH_COOKIE_VALUE="qa-passthrough-dev"

curl -k --fail $SUBSCRIPTIONS_URL &> /dev/null

if [ $? -eq 22 ]; then
  echo "Subscription frontend does not seem to be running at $SUBSCRIPTIONS_URL. Please try 'sbt devrun'"
  exit 1
else
  sbt 'acceptance-test'
fi
