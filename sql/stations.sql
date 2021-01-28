
DROP TABLE STATIONS                     IF EXISTS;

CREATE TABLE stations (
  station_id            SMALLINT          NOT NULL,
  name                  VARCHAR(25 BYTES) NOT NULL,
  fare                  SMALLINT          DEFAULT 250 NOT NULL,
  weight                INTEGER           NOT NULL,
  CONSTRAINT PK_stations PRIMARY KEY (station_id)
);
