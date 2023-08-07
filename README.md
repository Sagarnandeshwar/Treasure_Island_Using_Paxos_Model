# comp512p2 group 50
## Create the following files manually.
They are not in the repo to keep the code in the repo configuration independent.
Also, since `run_tiapp.sh` and `run_tiapp_auto.sh` are in the repo, all changes
to them will be logged by git.

`config_run.sh`
```shell
#!/bin/bash
# Set variables in this script to keep the run scripts computer/ configuration
# independent

group=50

# TODO Optional
# this will always generate the same game island. Change the last digits to any
# number if you want to change it to a different island map. Otherwise leave it
# as it is.
# MAKE SURE that every player has the same gameid.
gameid=game-$group-99

# TODO edit these entries to put the names of the servers and port numbers that
# you are using.
# player1 -> process 1, player 2 -> process 2, etc .. add more depending on how
# many players are playing.
# Remember to start the scripts of corresponding players from the corresponding
# servers.
# Comment out process3 if you are only playing 2 players, etc.
export process1=hostName:401$group
export process2=hostName:402$group
export process3=hostName:403$group
#export process4=hostName:404$group
#export process5=hostName:405$group
#export process6=hostName:406$group
#export process7=hostName:407$group
#export process8=hostName:408$group
#export process9=hostName:409$group

```

`config_test.sh`
```shell
#!/bin/bash
# Set variables in this script to keep the test scripts computer/ configuration
# independent

#TODO update your group number here inpace of XX
group=50

#TODO Optional
# this will always generate the same game island. Change the last digits to any
# number if you want to change it to a different island map. Otherwise leave it
# as it is.
gameid=game-$group-99

#TODO edit these entries to put the name of the server that you are using and
# the associated ports.
# Remember to start the script from this host
export autotesthost=$(hostname)
# player1 -> process 1, player 2 -> process 2, etc .. add more depending on how many players are playing.
# Script automatically counts the variables to figure out the number of players.
export process1=${autotesthost}:401$group
export process2=${autotesthost}:402$group
export process3=${autotesthost}:403$group
#export process4=${autotesthost}:404$group
#export process5=${autotesthost}:405$group
#export process6=${autotesthost}:406$group
#export process7=${autotesthost}:407$group
#export process8=${autotesthost}:408$group
#export process9=${autotesthost}:409$group

# The following variables can be changed if needed.
maxmoves=100
interval=100
randseed=xxxxxxxxx # Seed to generate moves.

# Uncomment these to simulate that type of failure for the N-th player.
#export failmode_N=RECEIVEPROPOSE
#export failmode_N=AFTERSENDVOTE
#export failmode_N=AFTERSENDPROPOSE
#export failmode_N=AFTERBECOMINGLEADER
#export failmode_N=AFTERVALUEACCEPT
#export failmode_N=AFTERBECOMINGLEADER

```