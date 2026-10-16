-- Oracle only. Run after 02-package.sql. It does not change SUPPORT_TICKET_PKG.
-- A table for requests that failed, and a small package that writes to it.

CREATE TABLE SUPPORT_ERROR_LOG (
    MESSAGE    VARCHAR2(500)  NOT NULL,
    LOGGED_AT  TIMESTAMP      DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE OR REPLACE PACKAGE SUPPORT_ERROR_PKG AS

    -- Writes one line. Like SUPPORT_TICKET_PKG, it has no COMMIT. The caller decides when to save.
    PROCEDURE log_error (p_message IN VARCHAR2);

END SUPPORT_ERROR_PKG;
/

CREATE OR REPLACE PACKAGE BODY SUPPORT_ERROR_PKG AS

    PROCEDURE log_error (p_message IN VARCHAR2) IS
    BEGIN
        INSERT INTO SUPPORT_ERROR_LOG (MESSAGE) VALUES (SUBSTR(p_message, 1, 500));
    END log_error;

END SUPPORT_ERROR_PKG;
/
