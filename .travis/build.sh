#!/usr/bin/env bash

# retrieve submodules
git pull --recurse-submodules && git submodule update --recursive

# build plugin and install it
mvn install

# checkout specific version of commons-math
cd commons-math
git checkout bugs-dot-jar_MATH-286_dbdff075
cd ..

# copy commons-math to have a fixed version
cp -r commons-math commons-math_fixed

# apply the dev patch
cd commons-math_fixed
patch -p1 <.bugs-dot-jar/developer-patch.diff
cd ..

# test the plugin
cd commons-math && mvn clean eu.stamp-project:diff-test-selection:0.1-SNAPSHOT:list -DpathToDiff=".bugs-dot-jar/developer-patch.diff" -DpathToOtherVersion="../commons-math_fixed"
cat testsThatExecuteTheChange.csv