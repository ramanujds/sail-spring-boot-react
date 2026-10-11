-- Runs once, when the container creates its database for the first time.
-- The testapp user itself is created by the image (APP_USER in docker-compose.yml).
-- The image runs this script as SYS in the root container, so switch to the PDB first.

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
