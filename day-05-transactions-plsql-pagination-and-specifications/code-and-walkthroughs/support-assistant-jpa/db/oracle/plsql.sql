-- Part 3: two small PL/SQL programs. Run this file once, as the same user that owns TICKETS.
-- The line with only a slash (/) ends each program.

-- A function returns one value. This one counts the overdue tickets of a priority.
CREATE OR REPLACE FUNCTION OVERDUE_COUNT (p_priority IN VARCHAR2) RETURN NUMBER IS
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM TICKETS
     WHERE STATUS IN ('OPEN', 'IN_PROGRESS')
       AND PRIORITY = p_priority
       AND DUE_BY < SYSTIMESTAMP;
    RETURN v_count;
END OVERDUE_COUNT;
/

-- A procedure does work. This one raises every overdue ticket to HIGH,
-- and gives back how many it changed.
CREATE OR REPLACE PROCEDURE ESCALATE_OVERDUE (p_escalated OUT NUMBER) IS
BEGIN
    UPDATE TICKETS
       SET PRIORITY = 'HIGH'
     WHERE STATUS IN ('OPEN', 'IN_PROGRESS')
       AND PRIORITY <> 'HIGH'
       AND DUE_BY < SYSTIMESTAMP;
    p_escalated := SQL%ROWCOUNT;
END ESCALATE_OVERDUE;
/
