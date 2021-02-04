#!/bin/bash

export PROJ_HOME=/Users/seetasomagani/Projects/event-driven-transactions

echo "--------------------------------------------"
echo "Loading the data model and topic for trains"
echo "--------------------------------------------"

sqlcmd < $PROJ_HOME/sql/trains.sql

echo "--------------------------------------------"
echo "Load initial train data"
echo "--------------------------------------------"

csvloader --file $PROJ_HOME/data/trains.csv --reportdir log trains

echo "--------------------------------------------"
echo "Generate train events"
echo "--------------------------------------------"

java -cp $PROJ_HOME/dist/events.jar:$PROJ_HOME/lib/kafka-clients-2.3.0.jar:$PROJ_HOME/lib/log4j-1.2.16.jar:$PROJ_HOME/lib/slf4j-api-1.6.2.jar:$PROJ_HOME/lib/slf4j-log4j12-1.6.2.jar metro.pub.TrainProducer localhost:9999 TRAINTOPIC 8
