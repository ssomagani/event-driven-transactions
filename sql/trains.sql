
--file -inlinebatch END_OF_DROP_BATCH

DROP TOPIC TRAINTOPIC                           IF EXISTS;
DROP TABLE TRAINS                               IF EXISTS;
DROP TABLE train_events                         IF EXISTS;

--END_OF_DROP_BATCH

--file -inlinebatch END_OF_BATCH

CREATE TABLE trains (
  train_id            SMALLINT          NOT NULL,
  name                VARCHAR(25) NOT NULL,
  CONSTRAINT PK_trains PRIMARY KEY (train_id)
);

CREATE TABLE train_events (
  train_id              INTEGER        NOT NULL,
  station_id            INTEGER        NOT NULL,
  activity_type         TINYINT        NOT NULL, -- 0 for arrival, 1 for departure.
  time                  TIMESTAMP      NOT NULL,
  PRIMARY KEY (station_id, train_id, time)
);
PARTITION TABLE train_events ON COLUMN station_id;
CREATE INDEX taStationDepart ON train_events (station_id, time);

-------------- PROCEDURES -------------------------------------------------------

CREATE topic TRAINTOPIC execute procedure train_events.insert;

--END_OF_BATCH
