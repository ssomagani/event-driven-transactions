#!/bin/bash

export PROJ_HOME=/Users/seetasomagani/Projects/event-driven-transactions

echo "--------------------------------------------"
echo "Loading the data model and topic for stations"
echo "--------------------------------------------"

sqlcmd < $PROJ_HOME/sql/stations.sql

echo "--------------------------------------------"
echo "Load initial station data"
echo "--------------------------------------------"

csvloader --file $PROJ_HOME/data/redline.csv --reportdir log stations