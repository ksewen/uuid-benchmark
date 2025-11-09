#!/bin/bash

pushd $(dirname $0) > /dev/null
SCRIPTPATH=$(pwd -P)
popd > /dev/null

do_package() {
  cd /uuid-benchmark/
  mvn package
}

do_package