
--file -inlinebatch END_OF_DROP_BATCH


DROP TOPIC RECHARGE                             IF EXISTS;

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
  export_time           BIGINT         NOT NULL,
  station_name          VARCHAR(25)    NOT NULL,
  name                  VARCHAR(50)    NOT NULL,
  phone                 VARCHAR(10)    NOT NULL, -- phone number, assumes North America
  email                 VARCHAR(50)    NOT NULL,
  notify                TINYINT           DEFAULT 0, -- 0=don't contact, 1=email, 2=text
  alert_message         VARCHAR(64)    NOT NULL
);


CREATE PROCEDURE RechargeCard PARTITION ON TABLE cards COLUMN card_id PARAMETER 1 AS
BEGIN
UPDATE cards SET balance = balance + ?
WHERE card_id = ? AND card_type = 0;
END;

CREATE TOPIC RECHARGE execute procedure RechargeCard;

--END_OF_BATCH
