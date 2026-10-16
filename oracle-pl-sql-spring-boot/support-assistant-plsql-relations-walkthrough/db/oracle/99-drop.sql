-- Oracle only. Removes everything that 01-tables.sql and 02-package.sql created, so that you can start again.
-- The child table goes before the parent table, because DESK_COMMENTS refers to DESK_TICKETS.

DROP PACKAGE DESK_TICKET_PKG;
DROP TABLE DESK_COMMENTS;
DROP TABLE DESK_TICKETS;
DROP SEQUENCE DESK_COMMENTS_SEQ;
DROP SEQUENCE DESK_TICKETS_SEQ;
