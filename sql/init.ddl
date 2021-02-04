DROP TOPIC _schemas                 IF EXISTS;
DROP TOPIC CONFIG                   IF EXISTS;
DROP TOPIC OFFSET                   IF EXISTS;
DROP TOPIC STATUS                   IF EXISTS;         

CREATE OPAQUE TOPIC _schemas PROFILE retain_compact;
CREATE OPAQUE TOPIC CONFIG PROFILE retain_compact;
CREATE OPAQUE TOPIC OFFSET PROFILE retain_compact;
CREATE OPAQUE TOPIC STATUS PROFILE retain_compact;
