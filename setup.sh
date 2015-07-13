#!/bin/bash
set -e

readonly SYSTEM=$(uname -s)
EXTRA_STEPS=()
BASEDIR=$(dirname $0)

linux() {
  [[ $SYSTEM == 'Linux' ]]
}

mac() {
  [[ $SYSTEM == 'Darwin' ]]
}

installed() {
  hash "$1" 2>/dev/null
}

create_install_vars() {
  local path="/etc/gu"
  local filename="install_vars"

  if [[ ! -f "$path/$filename" ]]; then
    if [[ ! -d "$path" ]]; then
      sudo mkdir "$path"
    fi

    echo "STAGE=DEV" | sudo tee "$path/$filename" > /dev/null
  fi
}

create_frontend_properties() {
  local path="$HOME/.gu"
  local filename="frontend.properties"

  if [[ ! -f "$path/$filename" ]]; then
    if [[ ! -d "$path" ]]; then
      mkdir "$path"
    fi

    touch "$path/$filename"
    EXTRA_STEPS+=("Ask a colleague for frontend.properties and add the contents to $path/$filename")
  fi
}

create_aws_credentials() {
  local path="$HOME/.aws"
  local filename="credentials"

  if [[ ! -f "$path/$filename" ]]; then
    if [[ ! -d "$path" ]]; then
      mkdir "$path"
    fi

    echo "[nextgen]
    aws_access_key_id=[YOUR_AWS_ACCESS_KEY]
    aws_secret_access_key=[YOUR_AWS_SECRET_ACCESS_KEY]
    region=eu-west-1" > "$path/$filename"
    EXTRA_STEPS+=("Add your AWS keys to $path/$filename")
  fi
}

install_homebrew() {
  if mac && ! installed brew; then
    ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
  fi
}

install_jdk() {
  if ! installed javac; then
    if linux; then
      sudo apt-get install -y openjdk-7-jdk
    elif mac; then
      EXTRA_STEPS+=("Download the JDK from http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html")
    fi
  fi
}

install_node() {
  if ! installed node || ! installed npm; then
    if ! installed curl; then
      sudo apt-get install -y curl
    fi

    if linux; then
      curl -sL https://deb.nodesource.com/setup | sudo bash -
      sudo apt-get install -y nodejs
    elif mac && installed brew; then
      brew install node
    fi
  fi
}

install_grunt() {
  if ! installed grunt; then
    sudo npm -g install grunt-cli
  fi
}

bower_javascripts() {
  if ! installed bower; then
    printf "\nYou need to install bower first:\n"
    printf "\nnpm install -g bower\n"
    exit 1
  else
    pushd assets/javascripts
    rm -rf bower_components
    bower install
  fi
  popd
}

bower_stylesheets() {
  if ! installed bower; then
    printf "\nYou need to install bower first:\n"
    printf "\nnpm install -g bower\n"
    exit 1
  else
    pushd assets/stylesheets
    rm -rf bower_components
    bower install
  fi
  popd
}

install_dependencies() {
  npm install
  bower_javascripts
  bower_stylesheets
}

install_chromedriver() {
  if ! installed chromedriver; then
    if linux; then
      EXTRA_STEPS+=("Download Google Chrome driver for the integration tests from https://code.google.com/p/selenium/wiki/ChromeDriver")
    elif mac; then
      brew install chromedriver
    fi
  fi
}

compile() {
  grunt compile
}

report() {
  if [[ ${#EXTRA_STEPS[@]} -gt 0 ]]; then
    echo -e
    echo "Remaining tasks: "
    for i in "${!EXTRA_STEPS[@]}"; do
      echo "  $((i+1)). ${EXTRA_STEPS[$i]}"
    done
  fi
}

main() {
  create_install_vars
  create_frontend_properties
  create_aws_credentials
  install_homebrew
  install_jdk
  install_node
  install_grunt
  install_dependencies
  install_chromedriver
  compile
  report
}

main
