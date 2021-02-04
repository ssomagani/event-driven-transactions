
--file -inlinebatch END_OF_DROP_BATCH
DROP TOPIC CARD_ALERT_EXPORT					IF EXISTS;

DROP TOPIC RECHARGE                             IF EXISTS;

DROP VIEW card_export_stats						IF EXISTS;

DROP PROCEDURE RechargeCard                     IF EXISTS;

DROP STREAM CARD_ALERT_EXPORT                   IF EXISTS;

DROP TABLE CARDS                                IF EXISTS;

--END_OF_DROP_BATCH

--file -inlinebatch END_OF_BATCH

CREATE TABLE CARDS(
  card_id               INTEGER        NOT NULL,
  enabled               TINYINT        DEFAULT 1 NOT NULL, -- 1=enabled, 0=disabled
  card_type             TINYINT        DEFAULT 0 NOT NULL, -- 0=pay per ride, 1=unlimited
  balance               INTEGER        DEFAULT 0, -- implicitly divide by 100 to get currency value
  expires               TIMESTAMP,
  name                  VARCHAR(50)    NOT NULL,
  phone                 VARCHAR(10)    NOT NULL, -- phone number, assumes North America
  email                 VARCHAR(50)    NOT NULL,
  notify                TINYINT           DEFAULT 0, -- 0=don't contact, 1=email, 2=text
  CONSTRAINT PK_cards_card_id PRIMARY KEY ( card_id )
);
PARTITION TABLE cards ON COLUMN card_id;

CREATE STREAM CARD_ALERT_EXPORT PARTITION ON COLUMN CARD_ID (
  card_id               INTEGER        NOT NULL,
  export_time           TIMESTAMP      NOT NULL,
  station_name          VARCHAR(25)    NOT NULL,
  name                  VARCHAR(50)    NOT NULL,
  phone                 VARCHAR(10)    NOT NULL, -- phone number, assumes North America
  email                 VARCHAR(50)    NOT NULL,
  notify                TINYINT           DEFAULT 0, -- 0=don't contact, 1=email, 2=text
  alert_message         VARCHAR(64)    NOT NULL
);

CREATE PROCEDURE PARTITION ON TABLE cards COLUMN card_id PARAMETER 0 FROM CLASS metro.cards.RechargeCard;

CREATE TOPIC RECHARGE execute procedure RechargeCard;

CREATE VIEW card_export_stats(card_id, station_name, rechargeCount) AS SELECT card_id, station_name, count(*) from CARD_ALERT_EXPORT GROUP BY card_id, station_name;

CREATE TOPIC using stream CARD_ALERT_EXPORT properties(topic.format=avro);

--END_OF_BATCH
