#!/bin/bash

export PROJ_HOME=/Users/seetasomagani/Projects/event-driven-transactions

echo "--------------------------------------------"
echo "Loading the data model, topics and procs for cards"
echo "--------------------------------------------"

mkdir -p $PROJ_HOME/bin/events
javac -cp $PROJ_HOME/lib/kafka-clients-2.3.0.jar:$PROJ_HOME/lib/log4j-1.2.16.jar:$PROJ_HOME/lib/slf4j-api-1.6.2.jar:$PROJ_HOME/lib/slf4j-log4j12-1.6.2.jar:$PROJ_HOME/lib/voltdbclient-10.0.1.jar -d $PROJ_HOME/bin $PROJ_HOME/events/metro/*.java $PROJ_HOME/events/metro/pub/*.java $PROJ_HOME/events/metro/serde/*.java
jar -cf $PROJ_HOME/dist/events.jar -C $PROJ_HOME/bin .

mkdir -p $PROJ_HOME/bin/processors
javac -cp $PROJ_HOME/lib/voltdb-10.0.1.jar -d $PROJ_HOME/bin/processors $PROJ_HOME/processors/metro/cards/*.java
jar -cf $PROJ_HOME/dist/procs.jar -C $PROJ_HOME/bin/processors .

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
