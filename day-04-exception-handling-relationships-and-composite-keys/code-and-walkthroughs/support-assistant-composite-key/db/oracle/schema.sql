CREATE TABLE TICKETS (
    ID           VARCHAR2(36)   PRIMARY KEY,
    TITLE        VARCHAR2(100)  NOT NULL,
    DESCRIPTION  VARCHAR2(500),
    STATUS       VARCHAR2(20)   NOT NULL,
    PRIORITY     VARCHAR2(10)   NOT NULL
);

CREATE TABLE COMMENTS (
    ID          VARCHAR2(36)   PRIMARY KEY,
    TICKET_ID   VARCHAR2(36)   NOT NULL REFERENCES TICKETS (ID),
    AUTHOR      VARCHAR2(50)   NOT NULL,
    TEXT        VARCHAR2(500)  NOT NULL,
    CREATED_AT  TIMESTAMP      NOT NULL
);

-- Part 5: one row says "this person watches this ticket".
-- The primary key has two columns: the same person can watch many tickets,
-- and a ticket can have many watchers, but a person watches a ticket once.
CREATE TABLE TICKET_WATCHERS (
    TICKET_ID       VARCHAR2(36)  NOT NULL REFERENCES TICKETS (ID) ON DELETE CASCADE,
    USERNAME        VARCHAR2(50)  NOT NULL,
    WATCHING_SINCE  TIMESTAMP     NOT NULL,
    PRIMARY KEY (TICKET_ID, USERNAME)
);
