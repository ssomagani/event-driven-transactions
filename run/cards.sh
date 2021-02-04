#!/bin/bash

export PROJ_HOME=/Users/seetasomagani/Projects/event-driven-transactions

echo "--------------------------------------------"
echo "Loading the data model, topics and procs for cards"
echo "--------------------------------------------"

sqlcmd --query="load classes $PROJ_HOME/dist/procs.jar"
sqlcmd < $PROJ_HOME/sql/cards.sql


echo "--------------------------------------------"
echo "Load initial cards data"
echo "--------------------------------------------"

java -cp $PROJ_HOME/dist/events.jar:$PROJ_HOME/lib/voltdbclient-10.0.1.jar:$PROJ_HOME/lib/kafka-clients-2.3.0.jar:$PROJ_HOME/lib/log4j-1.2.16.jar:$PROJ_HOME/lib/slf4j-api-1.6.2.jar:$PROJ_HOME/lib/slf4j-log4j12-1.6.2.jar metro.pub.CardsProducer --mode=new --output=$PROJ_HOME/data/cards.csv
csvloader --file $PROJ_HOME/data/cards.csv --reportdir log cards

echo "--------------------------------------------"
echo "Recharge Cards"
echo "--------------------------------------------"

java -cp $PROJ_HOME/dist/events.jar:$PROJ_HOME/lib/voltdbclient-10.0.1.jar:$PROJ_HOME/lib/kafka-clients-2.3.0.jar:$PROJ_HOME/lib/log4j-1.2.16.jar:$PROJ_HOME/lib/slf4j-api-1.6.2.jar:$PROJ_HOME/lib/slf4j-log4j12-1.6.2.jar metro.pub.CardsProducer --mode=recharge --servers=localhost:9999 --topic=RECHARGE
