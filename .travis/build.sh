#!/usr/bin/env bash

# retrieve submodules
git pull --recurse-submodules && git submodule update --recursive

# build plugin and install it
mvn install

# setup commons-math project
./src/main/bash/setup-commons-math.sh

# test the plugin
cd commons-math && mvn clean eu.stamp-project:diff-test-selection:0.1-SNAPSHOT:list -DpathToDiff=".bugs-dot-jar/developer-patch.diff" -DpathToOtherVersion="../commons-math_fixed"
cat testsThatExecuteTheChange.csv