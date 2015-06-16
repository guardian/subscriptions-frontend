#!/bin/bash

npm install
bundle

cd app/assets/javascripts && bower install && cd ../../..
cd app/assets/stylesheets && bower install && cd ../../..
