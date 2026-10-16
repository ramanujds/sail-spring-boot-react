-- Oracle only. Run once, as the user that your application connects as.
-- The names start with DESK_ so they do not clash with the SUPPORT_ tables of the other projects.

CREATE SEQUENCE DESK_TICKETS_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE DESK_COMMENTS_SEQ START WITH 1 INCREMENT BY 1;

-- The parent table.
CREATE TABLE DESK_TICKETS (
    ID           NUMBER         PRIMARY KEY,
    TITLE        VARCHAR2(100)  NOT NULL,
    DESCRIPTION  VARCHAR2(500),
    STATUS       VARCHAR2(20)   NOT NULL,
    PRIORITY     VARCHAR2(10)   NOT NULL,
    CREATED_AT   TIMESTAMP      NOT NULL,
    DUE_BY       TIMESTAMP      NOT NULL
);

-- The child table. TICKET_ID is the relationship: every comment belongs to exactly one ticket.
-- The REFERENCES clause makes Oracle refuse a comment for a ticket that does not exist.
CREATE TABLE DESK_COMMENTS (
    ID          NUMBER         PRIMARY KEY,
    TICKET_ID   NUMBER         NOT NULL REFERENCES DESK_TICKETS (ID),
    AUTHOR      VARCHAR2(50)   NOT NULL,
    BODY        VARCHAR2(500)  NOT NULL,
    CREATED_AT  TIMESTAMP      NOT NULL
);

-- Oracle does not index a foreign key by itself. This index makes "the comments of ticket 7" fast.
CREATE INDEX DESK_COMMENTS_TICKET_IX ON DESK_COMMENTS (TICKET_ID);
