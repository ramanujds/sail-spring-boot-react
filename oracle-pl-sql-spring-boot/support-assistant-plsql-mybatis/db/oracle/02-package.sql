-- Oracle only. Run after 01-tables.sql. You can run it again: CREATE OR REPLACE replaces the package.
-- No program in this package has a COMMIT. The caller decides when to save.

CREATE OR REPLACE PACKAGE SUPPORT_TICKET_PKG AS

    -- Error numbers that this package raises with RAISE_APPLICATION_ERROR.
    -- -20001  The title is missing
    -- -20002  The priority is not HIGH, MEDIUM or LOW
    -- -20003  The ticket does not exist
    -- -20004  The status change is not allowed

    -- Function: how many tickets of this priority are not finished.
    FUNCTION open_count (p_priority IN VARCHAR2) RETURN NUMBER;

    -- Procedure with an OUT parameter: creates a ticket and gives back its id.
    PROCEDURE create_ticket (
        p_title       IN  VARCHAR2,
        p_description IN  VARCHAR2,
        p_priority    IN  VARCHAR2,
        p_ticket_id   OUT NUMBER);

    -- Procedure: moves a ticket to its next status.
    PROCEDURE change_status (
        p_ticket_id   IN  NUMBER,
        p_new_status  IN  VARCHAR2);

    -- Procedure with a result set: the tickets with this status, or all tickets when p_status is NULL.
    PROCEDURE find_tickets (
        p_status      IN  VARCHAR2,
        p_tickets     OUT SYS_REFCURSOR);

END SUPPORT_TICKET_PKG;
/

CREATE OR REPLACE PACKAGE BODY SUPPORT_TICKET_PKG AS

    FUNCTION open_count (p_priority IN VARCHAR2) RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM SUPPORT_TICKETS
         WHERE PRIORITY = p_priority
           AND STATUS IN ('OPEN', 'IN_PROGRESS');
        RETURN v_count;
    END open_count;

    PROCEDURE create_ticket (
        p_title       IN  VARCHAR2,
        p_description IN  VARCHAR2,
        p_priority    IN  VARCHAR2,
        p_ticket_id   OUT NUMBER) IS
        v_days NUMBER;
    BEGIN
        IF p_title IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Title is required');
        END IF;

        -- The service level rule: HIGH is due in 1 day, MEDIUM in 3 days, LOW in 7 days.
        v_days := CASE p_priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 7 END;
        IF v_days IS NULL THEN
            RAISE_APPLICATION_ERROR(-20002, 'Priority must be HIGH, MEDIUM or LOW');
        END IF;

        p_ticket_id := SUPPORT_TICKETS_SEQ.NEXTVAL;

        INSERT INTO SUPPORT_TICKETS (ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY)
        VALUES (p_ticket_id, p_title, p_description, 'OPEN', p_priority,
                LOCALTIMESTAMP, LOCALTIMESTAMP + NUMTODSINTERVAL(v_days, 'DAY'));

        INSERT INTO SUPPORT_TICKET_LOG (TICKET_ID, ACTION) VALUES (p_ticket_id, 'CREATED');
    END create_ticket;

    PROCEDURE change_status (
        p_ticket_id   IN  NUMBER,
        p_new_status  IN  VARCHAR2) IS
        v_status SUPPORT_TICKETS.STATUS%TYPE;
        v_next   SUPPORT_TICKETS.STATUS%TYPE;
    BEGIN
        BEGIN
            SELECT STATUS INTO v_status FROM SUPPORT_TICKETS WHERE ID = p_ticket_id FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20003, 'Ticket ' || p_ticket_id || ' does not exist');
        END;

        -- The life of a ticket: OPEN, IN_PROGRESS, RESOLVED, CLOSED. One step at a time.
        v_next := CASE v_status
                      WHEN 'OPEN'        THEN 'IN_PROGRESS'
                      WHEN 'IN_PROGRESS' THEN 'RESOLVED'
                      WHEN 'RESOLVED'    THEN 'CLOSED'
                  END;

        IF v_next IS NULL THEN
            RAISE_APPLICATION_ERROR(-20004, 'Ticket ' || p_ticket_id || ' is CLOSED and cannot change');
        END IF;
        IF p_new_status IS NULL OR p_new_status <> v_next THEN
            RAISE_APPLICATION_ERROR(-20004, 'Ticket ' || p_ticket_id || ' is ' || v_status
                                            || '. The next status is ' || v_next);
        END IF;

        UPDATE SUPPORT_TICKETS SET STATUS = p_new_status WHERE ID = p_ticket_id;
        INSERT INTO SUPPORT_TICKET_LOG (TICKET_ID, ACTION) VALUES (p_ticket_id, 'STATUS ' || p_new_status);
    END change_status;

    PROCEDURE find_tickets (
        p_status      IN  VARCHAR2,
        p_tickets     OUT SYS_REFCURSOR) IS
    BEGIN
        OPEN p_tickets FOR
            SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
              FROM SUPPORT_TICKETS
             WHERE p_status IS NULL OR STATUS = p_status
             ORDER BY ID;
    END find_tickets;

END SUPPORT_TICKET_PKG;
/
