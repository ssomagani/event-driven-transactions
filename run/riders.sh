#!/bin/bash

export PROJ_HOME=/Users/seetasomagani/Projects/event-driven-transactions

echo "--------------------------------------------"
echo "Loading the data model and topic for trains"
echo "--------------------------------------------"

sqlcmd < $PROJ_HOME/sql/riders.sql

java -cp $PROJ_HOME/dist/events.jar:$PROJ_HOME/lib/voltdbclient-10.0.1.jar:$PROJ_HOME/lib/kafka-clients-2.3.0.jar:$PROJ_HOME/lib/log4j-1.2.16.jar:$PROJ_HOME/lib/slf4j-api-1.6.2.jar:$PROJ_HOME/lib/slf4j-log4j12-1.6.2.jar metro.pub.RidersProducer 
