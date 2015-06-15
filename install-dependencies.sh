#!/bin/bash

npm install
bundle
./node_modules/.bin/jspm install

cd app/assets/javascripts && bower install && cd ../../..
cd app/assets/stylesheets && bower install && cd ../../..
