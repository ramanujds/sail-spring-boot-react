-- Oracle only. Optional. Creates 12 tickets, so that there is more than one page to read.
-- It calls the same procedure as POST /api/tickets, then saves with COMMIT.

DECLARE
    v_id NUMBER;
BEGIN
    FOR i IN 1 .. 12 LOOP
        SUPPORT_TICKET_PKG.create_ticket(
            p_title       => 'Sample ticket ' || i,
            p_description => NULL,
            p_priority    => CASE MOD(i, 3) WHEN 0 THEN 'HIGH' WHEN 1 THEN 'MEDIUM' ELSE 'LOW' END,
            p_ticket_id   => v_id);
    END LOOP;
    COMMIT;
END;
/
