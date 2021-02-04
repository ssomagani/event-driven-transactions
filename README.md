# Event Driven Transactions Example 

## Use Case
--------
Event-driven transactional processing with VoltDB

This application demonstrates how VoltDB can support event-driven architectures making real time decisions.

This example application is imagined as an IT system that supports a subway system by handling event streams and real time request-responses.
Simulated requests to process card swipes (to let passengers onto the platform) and event streams from exit swipes, trains and stations are used to demonstrate the applicability of VoltDB for event-driven applications that need transactions.


## How to run on a single machine
--------
1. cd run; ./start_voltdb.sh (Start VoltDB server)
2. ./stations.sh     (Load Stations data model and initial data from a CSV file)
3. ./trains.sh       (Load Trains data model, create topic, load initial data, and start the event stream)      (Demonstrates publishing events into VoltDB)
4. ./cards.sh        (Load Cards data model, create topics, load initial data, and start the event streams)     (Demonstrates publishing events from VoltDB)
5. ./riders.sh       (Load Riders data model, create topics, and start the event streams)       (Demonstrates merging multiple event streams into one)

## TODO
---------
- Add monitoring
- Demonstrate multi-pass processing of a stream through VoltDB
- General cleanup
