
--file -inlinebatch END_OF_DROP_BATCH

DROP PROCEDURE ProcessExit                      IF EXISTS;
DROP PROCEDURE ValidateEntry                    IF EXISTS;

DROP VIEW CARD_HISTORY_SECOND            IF EXISTS;

DROP TOPIC TRIPS                                IF EXISTS;
DROP TOPIC FRAUD                               IF EXISTS;

DROP STREAM TRIPS                               IF EXISTS;
DROP STREAM FRAUD                               IF EXISTS;

DROP TABLE CARD_EVENTS                          IF EXISTS;


--END_OF_DROP_BATCH

--file -inlinebatch END_OF_BATCH


CREATE TABLE CARD_EVENTS(
  card_id               INTEGER        NOT NULL,
  date_time             TIMESTAMP      NOT NULL,
  station_id            SMALLINT       NOT NULL,
  activity_code         TINYINT        NOT NULL, -- 1=entry, 2=purchase, -1=Exit
  amount                INTEGER        NOT NULL,
  accept                TINYINT        NOT NULL, -- 1=accepted, 0=rejected
);
PARTITION TABLE CARD_EVENTS ON COLUMN card_id;
CREATE INDEX aCardDate ON CARD_EVENTS (card_id, date_time, activity_code);
CREATE INDEX aStationDateCard ON CARD_EVENTS (station_id, date_time, card_id);

CREATE VIEW CARD_HISTORY_SECOND as select card_id, TRUNCATE(SECOND, date_time) scnd from card_events group by card_id, scnd;

CREATE STREAM TRIPS partition on column CARD_ID (
  trip_id varchar not null, 
  card_id integer not null,
  entry_station integer not null,
  entry_time varchar, 
  exit_station integer not null,
  exit_time varchar
);
create topic using stream TRIPS properties(topic.format=avro,consumer.keys=TRIP_ID);

CREATE STREAM FRAUD partition on column CARD_ID (
  TRANS_ID varchar not null,
  CARD_ID integer not null,
  DATE_TIME timestamp not null,
  STATION integer not null,
  ACTIVITY_TYPE TINYINT not null,
  AMT integer not null
);
create topic using stream FRAUD properties(topic.format=avro,consumer.keys=TRANS_ID);

-------------- PROCEDURES -------------------------------------------------------

CREATE PROCEDURE PARTITION ON TABLE cards COLUMN card_id PARAMETER 0 FROM CLASS metro.cards.ProcessExit;
CREATE PROCEDURE PARTITION ON TABLE cards COLUMN card_id PARAMETER 0 FROM CLASS metro.cards.ValidateEntry;

--END_OF_BATCH
