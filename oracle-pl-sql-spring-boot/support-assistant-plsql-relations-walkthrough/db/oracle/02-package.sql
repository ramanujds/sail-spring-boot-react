-- Oracle only. Run after 01-tables.sql. You can run it again: CREATE OR REPLACE replaces the package.
-- No program in this package has a COMMIT. The caller decides when to save.

CREATE OR REPLACE PACKAGE DESK_TICKET_PKG AS

    -- Error numbers that this package raises with RAISE_APPLICATION_ERROR.
    -- -20001  The title is missing
    -- -20002  The priority is not HIGH, MEDIUM or LOW
    -- -20003  The ticket does not exist
    -- -20004  The status change is not allowed
    -- -20005  The author or the text of a comment is missing
    -- -20006  The ticket is CLOSED, so it takes no more comments

    -- Creates a ticket and gives back its id.
    PROCEDURE create_ticket (
        p_title       IN  VARCHAR2,
        p_description IN  VARCHAR2,
        p_priority    IN  VARCHAR2,
        p_ticket_id   OUT NUMBER);

    -- Moves a ticket to its next status.
    PROCEDURE change_status (
        p_ticket_id   IN  NUMBER,
        p_new_status  IN  VARCHAR2);

    -- The tickets with this status, or all tickets when p_status is NULL. No comments.
    PROCEDURE find_tickets (
        p_status      IN  VARCHAR2,
        p_tickets     OUT SYS_REFCURSOR);

    -- Adds a comment to a ticket and gives back the id of the comment.
    PROCEDURE add_comment (
        p_ticket_id   IN  NUMBER,
        p_author      IN  VARCHAR2,
        p_body        IN  VARCHAR2,
        p_comment_id  OUT NUMBER);

    -- One ticket and its comments. Two cursors: one row of the parent, and the rows of the child.
    PROCEDURE get_ticket (
        p_ticket_id   IN  NUMBER,
        p_ticket      OUT SYS_REFCURSOR,
        p_comments    OUT SYS_REFCURSOR);

    -- Many tickets and their comments in one cursor. A ticket with 3 comments has 3 rows.
    -- A ticket with no comments has 1 row, and the comment columns are NULL.
    PROCEDURE find_tickets_with_comments (
        p_status      IN  VARCHAR2,
        p_rows        OUT SYS_REFCURSOR);

END DESK_TICKET_PKG;
/

CREATE OR REPLACE PACKAGE BODY DESK_TICKET_PKG AS

    -- Private: it is not in the specification, so Java cannot call it. It must come before the
    -- programs that use it. It reads the status of a ticket and locks the row, so that nobody
    -- else changes the ticket until the caller saves or undoes its work.
    FUNCTION lock_ticket (p_ticket_id IN NUMBER) RETURN VARCHAR2 IS
        v_status DESK_TICKETS.STATUS%TYPE;
    BEGIN
        SELECT STATUS INTO v_status FROM DESK_TICKETS WHERE ID = p_ticket_id FOR UPDATE;
        RETURN v_status;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'Ticket ' || p_ticket_id || ' does not exist');
    END lock_ticket;

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

        p_ticket_id := DESK_TICKETS_SEQ.NEXTVAL;

        INSERT INTO DESK_TICKETS (ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY)
        VALUES (p_ticket_id, p_title, p_description, 'OPEN', p_priority,
                LOCALTIMESTAMP, LOCALTIMESTAMP + NUMTODSINTERVAL(v_days, 'DAY'));
    END create_ticket;

    PROCEDURE change_status (
        p_ticket_id   IN  NUMBER,
        p_new_status  IN  VARCHAR2) IS
        v_status DESK_TICKETS.STATUS%TYPE;
        v_next   DESK_TICKETS.STATUS%TYPE;
    BEGIN
        v_status := lock_ticket(p_ticket_id);

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

        UPDATE DESK_TICKETS SET STATUS = p_new_status WHERE ID = p_ticket_id;
    END change_status;

    PROCEDURE find_tickets (
        p_status      IN  VARCHAR2,
        p_tickets     OUT SYS_REFCURSOR) IS
    BEGIN
        OPEN p_tickets FOR
            SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
              FROM DESK_TICKETS
             WHERE p_status IS NULL OR STATUS = p_status
             ORDER BY ID;
    END find_tickets;

    PROCEDURE add_comment (
        p_ticket_id   IN  NUMBER,
        p_author      IN  VARCHAR2,
        p_body        IN  VARCHAR2,
        p_comment_id  OUT NUMBER) IS
        v_status DESK_TICKETS.STATUS%TYPE;
    BEGIN
        IF p_author IS NULL OR p_body IS NULL THEN
            RAISE_APPLICATION_ERROR(-20005, 'Author and text are required');
        END IF;

        -- The parent decides what the child may do. Locking the ticket also means that
        -- nobody can close it between this check and the INSERT below.
        v_status := lock_ticket(p_ticket_id);
        IF v_status = 'CLOSED' THEN
            RAISE_APPLICATION_ERROR(-20006, 'Ticket ' || p_ticket_id || ' is CLOSED and takes no comments');
        END IF;

        p_comment_id := DESK_COMMENTS_SEQ.NEXTVAL;

        INSERT INTO DESK_COMMENTS (ID, TICKET_ID, AUTHOR, BODY, CREATED_AT)
        VALUES (p_comment_id, p_ticket_id, p_author, p_body, LOCALTIMESTAMP);
    END add_comment;

    PROCEDURE get_ticket (
        p_ticket_id   IN  NUMBER,
        p_ticket      OUT SYS_REFCURSOR,
        p_comments    OUT SYS_REFCURSOR) IS
        v_found NUMBER;
    BEGIN
        -- A cursor that finds no row is not an error. Check first, so that a missing ticket is a 404.
        SELECT COUNT(*) INTO v_found FROM DESK_TICKETS WHERE ID = p_ticket_id;
        IF v_found = 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'Ticket ' || p_ticket_id || ' does not exist');
        END IF;

        OPEN p_ticket FOR
            SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
              FROM DESK_TICKETS
             WHERE ID = p_ticket_id;

        OPEN p_comments FOR
            SELECT ID, AUTHOR, BODY, CREATED_AT
              FROM DESK_COMMENTS
             WHERE TICKET_ID = p_ticket_id
             ORDER BY ID;
    END get_ticket;

    PROCEDURE find_tickets_with_comments (
        p_status      IN  VARCHAR2,
        p_rows        OUT SYS_REFCURSOR) IS
    BEGIN
        -- LEFT JOIN keeps a ticket that has no comment. A plain JOIN would drop it from the result.
        OPEN p_rows FOR
            SELECT t.ID, t.TITLE, t.DESCRIPTION, t.STATUS, t.PRIORITY, t.CREATED_AT, t.DUE_BY,
                   c.ID AS COMMENT_ID, c.AUTHOR, c.BODY, c.CREATED_AT AS COMMENT_AT
              FROM DESK_TICKETS t
              LEFT JOIN DESK_COMMENTS c ON c.TICKET_ID = t.ID
             WHERE p_status IS NULL OR t.STATUS = p_status
             ORDER BY t.ID, c.ID;
    END find_tickets_with_comments;

END DESK_TICKET_PKG;
/
