#!/bin/bash

if [[ $# -ne 1 ]]
then
	echo "Please pass the player number as the argument"
	echo "  $0 <playernum>"
	echo "  $0 3"
	exit 2
fi

# The player number is passed as the argument.
playernum=$1

# Directory of jar = dir of this script file, up twice.
BASEDIR=$HOME/comp512/p2
BASEDIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/../../
BASEDIR=$(realpath "$BASEDIR") # Remove "../..", confirmed installed on lab2
echo "jar located at: $BASEDIR"

# Load user changeable variables from config so that this sh file can be used
# everywhere. See config_run.sh to edit the variables.
source "$BASEDIR"/config_run.sh

# Group number moved to config_run.sh

# Island seed moved to config_run.sh

# Process variables moved to config_run.sh

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

# set the classpath to include the gcl tar files and the parent path to the comp512st directory.
export CLASSPATH=$BASEDIR/comp512p2.jar:$BASEDIR

# Build the process group string.
export processgroup=$(env | grep '^process[1-9]=' | sort | sed -e 's/.*=//')
processgroup=$(echo $processgroup | sed -e 's/ /,/g')

# Total number of players
numplayers=$(echo $processgroup | awk -F',' '{ print NF}')

if [[ $playernum -gt $numplayers ]]
then
	echo "Error, you have only $numplayers processes configured for your group. Cannot allocate a process from $processgroup to player number $playernum"
	exit 3
fi

# Find out the process mapped to THIS player.
myprocess=$(echo $processgroup | awk -F',' -v playernum=$playernum '{ print $playernum }')

if [[ -z $myprocess ]]
then
	echo "Error, unable to allocate a process to this player from the group $processgroup"
	exit 4
fi

# Check if this script is being exectuted on the correct server.
# Add IPv4 representation to test at home, localhost for simpler debugging
myhost=${myprocess%:*}
if [[ $myhost != $(hostname) && $(hostname -I) == *"$myhost"* && $myhost != "localhost" ]]
then
	echo "Error !! your player's process [$myprocess] is set to run from $myhost, but you are trying to run this script on $(hostname)."
	exit 10
fi

# Start the game instance for this player.

# Final pre-start checkup
echo "About to start treasure island app with parameters:"
echo "  myprocess = $myprocess"
echo "  processgroup = $processgroup"
echo "  gameid = $gameid"
echo "  numplayers = $numplayers"
echo "  playernum = $playernum"
read -n 1 -p $"Start the app (y/n)? " userInput
echo
if [[ "$userInput" != "y" && "$userInput" != "Y" ]]; then
  echo "Cancelled"
  exit 1
fi

set -x
java comp512st.tiapp.TreasureIslandApp $myprocess $processgroup $gameid $numplayers $playernum

set +x
echo "Waiting for background processes to finish..."
wait
echo "Done" # At this point lock files should be gone.

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

