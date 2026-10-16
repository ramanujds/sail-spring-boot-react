-- Oracle only. Removes everything that 01-tables.sql, 02-package.sql and 03-page-package.sql created,
-- so that you can start again.

DROP PACKAGE SUPPORT_TICKET_PAGE_PKG;

DROP PACKAGE SUPPORT_TICKET_PKG;
DROP TABLE SUPPORT_TICKET_LOG;
DROP TABLE SUPPORT_TICKETS;
DROP SEQUENCE SUPPORT_TICKETS_SEQ;
