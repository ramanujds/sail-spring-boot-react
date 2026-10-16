-- Oracle only. Run after 02-package.sql. It does not change SUPPORT_TICKET_PKG.
-- OFFSET ... FETCH NEXT needs Oracle 12c or later.

CREATE OR REPLACE PACKAGE SUPPORT_TICKET_PAGE_PKG AS

    -- One page of tickets, and the number of tickets in all pages.
    -- p_status     NULL for every status
    -- p_page       the page number, from 0
    -- p_size       how many tickets in a page
    -- p_sort       'DUE_BY' or 'CREATED_AT'. Any other value orders by ID only
    -- p_direction  'ASC' or 'DESC'
    PROCEDURE find_page (
        p_status     IN  VARCHAR2,
        p_page       IN  NUMBER,
        p_size       IN  NUMBER,
        p_sort       IN  VARCHAR2,
        p_direction  IN  VARCHAR2,
        p_tickets    OUT SYS_REFCURSOR,
        p_total      OUT NUMBER);

END SUPPORT_TICKET_PAGE_PKG;
/

CREATE OR REPLACE PACKAGE BODY SUPPORT_TICKET_PAGE_PKG AS

    PROCEDURE find_page (
        p_status     IN  VARCHAR2,
        p_page       IN  NUMBER,
        p_size       IN  NUMBER,
        p_sort       IN  VARCHAR2,
        p_direction  IN  VARCHAR2,
        p_tickets    OUT SYS_REFCURSOR,
        p_total      OUT NUMBER) IS
    BEGIN
        -- The total, with the same WHERE as the page.
        SELECT COUNT(*)
          INTO p_total
          FROM SUPPORT_TICKETS
         WHERE p_status IS NULL OR STATUS = p_status;

        -- The page. Only one of the four CASE lines has a value. The others are NULL for every row,
        -- so they do not change the order. ID comes last, so two calls always give the same order.
        OPEN p_tickets FOR
            SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
              FROM SUPPORT_TICKETS
             WHERE p_status IS NULL OR STATUS = p_status
             ORDER BY
                   CASE WHEN p_sort = 'DUE_BY'     AND p_direction = 'ASC'  THEN DUE_BY     END ASC,
                   CASE WHEN p_sort = 'DUE_BY'     AND p_direction = 'DESC' THEN DUE_BY     END DESC,
                   CASE WHEN p_sort = 'CREATED_AT' AND p_direction = 'ASC'  THEN CREATED_AT END ASC,
                   CASE WHEN p_sort = 'CREATED_AT' AND p_direction = 'DESC' THEN CREATED_AT END DESC,
                   ID
            OFFSET p_page * p_size ROWS FETCH NEXT p_size ROWS ONLY;
    END find_page;

END SUPPORT_TICKET_PAGE_PKG;
/
