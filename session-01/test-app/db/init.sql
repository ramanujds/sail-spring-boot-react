-- Creates the MESSAGES table and two sample rows.
--
-- Docker container: nothing to do. The image runs this file once, on first start.
--
-- Local Oracle 19c: run this file yourself, connected as YOUR schema user, and
-- skip (delete or comment out) the two ALTER SESSION lines below. They only exist
-- because the Docker image runs this script as SYS in the root container.
--   SQL Developer: open this file and run it as a script (F5).
--   SQL*Plus:      sqlplus <schema-user>@//<host>:<port>/<service-name> @db/init.sql

ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = testapp;

CREATE SEQUENCE MESSAGES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE MESSAGES (
    ID            NUMBER         PRIMARY KEY,
    MESSAGE_TEXT  VARCHAR2(200)  NOT NULL,
    CREATED_AT    TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL
);

INSERT INTO MESSAGES (ID, MESSAGE_TEXT) VALUES (MESSAGES_SEQ.NEXTVAL, 'Hello from Oracle XE 21c');
INSERT INTO MESSAGES (ID, MESSAGE_TEXT) VALUES (MESSAGES_SEQ.NEXTVAL, 'Seeded by db/init.sql');
COMMIT;
