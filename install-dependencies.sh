#!/bin/bash

npm install

cd app/assets/javascripts && bower install && cd ../../..
cd app/assets/stylesheets && bower install && cd ../../..
