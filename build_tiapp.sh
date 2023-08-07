#!/bin/bash
# Script modified so we don't create environment variables.

# Directory in which this script file is located, don't move pls.
BASEDIR=$HOME/comp512/p2
BASEDIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

if [[ ! -d $BASEDIR ]]
then
	echo "Error $BASEDIR is not a valid dir."
	exit 1
fi

if [[ ! -f $BASEDIR/comp512p2.jar ]]
then
	echo "Error cannot locate $BASEDIR/comp512p2.jar . Make sure it is present."
	exit 1
fi

if [[ ! -d $BASEDIR/comp512st ]]
then
	echo "Error cannot locate $BASEDIR/comp512st directory . Make sure it is present."
	exit 1
fi

# Add jar and root directory into the classpath
cpPaths=$BASEDIR/comp512p2.jar:$BASEDIR

cd $BASEDIR
javac -cp "$cpPaths" $(find -L comp512st -name '*.java')

