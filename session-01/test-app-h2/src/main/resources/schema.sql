-- Runs automatically at startup (embedded database).
-- Same shape as the Oracle version in ../test-app/db/init.sql, using H2 types.

CREATE SEQUENCE MESSAGES_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE MESSAGES (
    ID            BIGINT         PRIMARY KEY,
    MESSAGE_TEXT  VARCHAR(200)   NOT NULL,
    CREATED_AT    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL
);
