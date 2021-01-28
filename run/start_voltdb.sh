#!/bin/bash

export PROJ_HOME=/Users/seetasomagani/Projects/event-driven-transactions

voltdb init -f -C $PROJ_HOME/deployment.xml

voltdb start
