#!/bin/bash

#TODO set this to where your code and jar file root dir is
BASEDIR=$HOME/comp512/p2
BASEDIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/../../
BASEDIR=$(realpath "$BASEDIR") # Remove "../..", confirmed installed on lab2
echo "jar located at: $BASEDIR"

# Load user changeable variables from config so that this sh file can be used
# everywhere. See config_test.sh to edit the variables.
source "$BASEDIR"/config_test.sh

# Process variables moved to config_run.sh

# Testing move variables (max, interval, seed) handled in config_test.sh

# Failure modes selection handled in config_test.sh.

# Check if this script is being exectuted on the correct server.
# Add IPv4 representation to test at home, localhost for simpler debugging
if [[ $autotesthost != $(hostname) && $autotesthost != $(hostname -I) && $autotesthost != "localhost" ]]
then
	echo "Error !! This script is configured to run from $autotesthost, but you are trying to run this script on $(hostname)."
	exit 10
fi

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

# set your classpath to include the gcl tar files and the parent path to the comp512st directory.
export CLASSPATH=$BASEDIR/comp512p2.jar:$BASEDIR

# Build the process group string.
export processgroup=$(env | grep '^process[1-9]=' | sort | sed -e 's/.*=//')
processgroup=$(echo $processgroup | sed -e 's/ /,/g')

# Total number of players
numplayers=$(echo $processgroup | awk -F',' '{ print NF}')
echo "There are $numplayers players in the setup"

# We do not need colors to track.
export TIMONOCHROME=true
# We do not want the island display to get constantly refreshed.
export UPDATEDISPLAY=false

# Final pre-start checkup
echo "About to start treasure island auto tester with parameters:"
echo "  processgroup = $processgroup"
echo "  gameid = $gameid"
echo "  numplayers = $numplayers"
echo "  maxmoves = $maxmoves"
echo "  interval = $interval"
echo "  randseed = $randseed"
echo "  failmode = "
echo "$(env | grep '^failmode_' | sed -e 's/.*=//')"
read -n 1 -p $"Start the tester (y/n)? " userInput
echo
if [[ "$userInput" != "y" && "$userInput" != "Y" ]]; then
  echo "Cancelled"
  exit 1
fi


playernum=1
for process in $(echo $processgroup | sed -e 's/,/ /g')
do
	failmode=$(env | grep '^failmode_'${playernum}'=' | sed -e 's/.*=//')
	echo java comp512st.tests.TreasureIslandAppAuto $process $processgroup $gameid $numplayers $playernum $maxmoves $interval $randseed$playernum $failmode '>' $gameid-$playernum-display.log
	java comp512st.tests.TreasureIslandAppAuto $process $processgroup $gameid $numplayers $playernum $maxmoves $interval $randseed$playernum $failmode  > $gameid-$playernum-display.log 2>&1 &
	playernum=$(expr $playernum + 1);
done
echo "Waiting for background processes to finish..."
wait
echo "Done"

# Clean up by putting the log into the logs folder
echo "Moving log files to folder"
cd "$BASEDIR" || exit 1
mostRecentLog=$(ls logs | sort -r | head -1)
tmp=$IFS
IFS='.'
read -a timestamp <<< "$mostRecentLog" # Split on IFS, only care abt 1st el
IFS=$tmp
for logFile in $(find *.log); do
  cp "$logFile" "logs/${timestamp[0]}.$logFile" # In case its still locked
  rm "$logFile"
done