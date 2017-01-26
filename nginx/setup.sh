#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_HOME=$(nginx -V 2>&1 | grep 'configure arguments:' | sed 's#.*conf-path=\([^ ]*\)/nginx\.conf.*#\1#g')

echo this script needs root access to configure nginx, please enter your sudo password if prompted
sudo mkdir -p $NGINX_HOME/sites-enabled
sudo ln -fs $DIR/subscribe.conf $NGINX_HOME/sites-enabled/subscribe.conf

