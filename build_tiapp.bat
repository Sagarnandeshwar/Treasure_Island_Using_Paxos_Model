:: Adapted from build_tiapp.sh
:: Unlike bash script, must be run from same folder.
@echo off
:: Simply compile all jar files. Assume this script is ran here
javac -cp comp512p2.jar;. -target 11 comp512st/paxos/*.java comp512st/tests/*.java comp512st/tiapp/*.java