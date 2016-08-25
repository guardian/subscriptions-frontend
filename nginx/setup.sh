#!/bin/bash

GU_KEYS="${HOME}/.gu/keys"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_HOME=$(nginx -V 2>&1 | grep 'configure arguments:' | sed 's#.*conf-path=\([^ ]*\)/nginx\.conf.*#\1#g')

echo this script needs root access and membership AWS credentials
echo configuring nginx, please enter your sudo password if prompted
sudo mkdir -p $NGINX_HOME/sites-enabled
sudo ln -fs $DIR/subscribe.conf $NGINX_HOME/sites-enabled/subscribe.conf

aws s3 cp s3://identity-local-ssl/sub-thegulocal-com-exp2017-03-31-bundle.crt ${GU_KEYS}/ --profile membership
aws s3 cp s3://identity-local-ssl/sub-thegulocal-com-exp2017-03-31.key ${GU_KEYS}/ --profile membership
sudo ln -fs ${GU_KEYS}/ $NGINX_HOME/keys

sudo nginx -s stop
sleep 1
sudo nginx
