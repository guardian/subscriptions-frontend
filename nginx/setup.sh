#!/bin/bash

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

dev-nginx setup-cert sub.thegulocal.com
dev-nginx link-config $DIR/subscribe.conf
dev-nginx restart-nginx
