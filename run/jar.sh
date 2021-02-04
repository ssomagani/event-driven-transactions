#!/bin/bash

export PROJ_HOME=/Users/seetasomagani/Projects/event-driven-transactions

mkdir -p $PROJ_HOME/bin/events
javac -cp $PROJ_HOME/lib/kafka-clients-2.3.0.jar:$PROJ_HOME/lib/log4j-1.2.16.jar:$PROJ_HOME/lib/slf4j-api-1.6.2.jar:$PROJ_HOME/lib/slf4j-log4j12-1.6.2.jar:$PROJ_HOME/lib/voltdbclient-10.0.1.jar -d $PROJ_HOME/bin $PROJ_HOME/events/metro/*.java $PROJ_HOME/events/metro/pub/*.java $PROJ_HOME/events/metro/serde/*.java
jar -cf $PROJ_HOME/dist/events.jar -C $PROJ_HOME/bin .

mkdir -p $PROJ_HOME/bin/processors
javac -cp $PROJ_HOME/lib/voltdb-10.0.1.jar -d $PROJ_HOME/bin/processors $PROJ_HOME/processors/metro/cards/*.java
jar -cf $PROJ_HOME/dist/procs.jar -C $PROJ_HOME/bin/processors .
