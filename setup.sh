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
    nvm install
    nvm use
  fi
}

install_yarn() {
  if ! installed yarn; then
    npm -g install yarn
  fi
}

install_grunt() {
  if ! installed grunt; then
    npm -g install grunt-cli
  fi
}

install_bower() {
  if ! installed bower; then
    npm -g install bower
  fi
}

install_chromedriver() {
  if ! installed chromedriver; then
    if linux; then
      EXTRA_STEPS+=("Download Google Chrome driver for the integration tests from https://code.google.com/p/selenium/wiki/ChromeDriver")
    elif mac; then
      npm install -g chromedriver
    fi
  fi
}

install_npm_globals() {
  install_yarn
  install_grunt
  install_bower
  install_chromedriver
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
  yarn install
  bower_stylesheets
}

install_nginx() {
  if ! installed nginx; then
    if linux; then
      sudo apt-get install -y nginx
    elif mac; then
      brew install nginx
    fi
  fi
}

copy_githooks() {
    cp git-hooks/pre-commit .git/hooks/
}

compile() {
  yarn run compile
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
  install_homebrew
  install_jdk
  install_node
  install_npm_globals
  install_dependencies
  install_nginx
  copy_githooks
  compile
  report
}

main
