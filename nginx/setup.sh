#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_HOME=$(nginx -V 2>&1 | grep 'configure arguments:' | sed 's#.*conf-path=\([^ ]*\)/nginx\.conf.*#\1#g')

sudo mkdir -p $NGINX_HOME/sites-enabled
sudo ln -fs $DIR/subscribe.conf $NGINX_HOME/sites-enabled/subscribe.conf
sudo ln -fs $DIR/subscribe.crt $NGINX_HOME/subscribe.crt
sudo ln -fs $DIR/subscribe.key $NGINX_HOME/subscribe.key
sudo nginx -s stop
sudo nginx
