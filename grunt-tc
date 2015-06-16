#!/bin/bash

set -o xtrace
set -o nounset
set -o errexit

################################################################################
#
# Build static assets for the project, files will now be found in public/
#
################################################################################

JS_DIR=app/assets/javascripts
SCSS_DIR=app/assets/stylesheets

npm install

pushd $JS_DIR
bower install
popd

pushd $SCSS_DIR
bower install
popd

grunt "$@"
