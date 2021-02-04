#!/bin/bash

export PROJ_HOME=/Users/seetasomagani/Projects/event-driven-transactions
cd $PROJ_HOME/run

./jar.sh

voltadmin shutdown
voltdb init -f -C $PROJ_HOME/deployment.xml
voltdb start -B

sleep 10s

sqlcmd < ../sql/init.ddl

./run-schema-registry.sh

