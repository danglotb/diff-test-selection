#!/usr/bin/env bash

# retrieve submodules
git pull --recurse-submodules && git submodule update --recursive

# build plugin and install it
mvn install

# setup commons-math project
./src/main/bash/setup-commons-math.sh

# execute the plugin
cd commons-math && mvn clean eu.stamp-project:diff-test-selection:0.1-SNAPSHOT:list -DpathToDiff=".bugs-dot-jar/developer-patch.diff" -DpathToOtherVersion="../commons-math_fixed"

# test the output
expected="org.apache.commons.math.optimization.linear.SimplexSolverTest;testRestrictVariablesToNonNegative;testInfeasibleSolution;testSimplexSolver;testMath272;testModelWithNoArtificialVars;testEpsilon;testSolutionWithNegativeDecisionVariable;testLargeModel;testMath286;testMinimization;testSingleVariableAndConstraint;testTrivialModel;testUnboundedSolution"
actual=$(head -n1 testsThatExecuteTheChange.csv)
echo $expected
echo $actual
if [ "$expected" = "$actual" ]
then
        echo "OK"
        return 0
else
        echo "KO"
        return 1
fi