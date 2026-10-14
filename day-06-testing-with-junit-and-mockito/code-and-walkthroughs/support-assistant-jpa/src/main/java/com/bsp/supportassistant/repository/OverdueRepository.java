package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.Priority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.util.Map;

// Calls the PL/SQL programs of db/oracle/plsql.sql.
@Repository
public class OverdueRepository {

    private final SimpleJdbcCall overdueCount;
    private final SimpleJdbcCall escalateOverdue;

    public OverdueRepository(JdbcTemplate jdbcTemplate) {
        this.overdueCount = new SimpleJdbcCall(jdbcTemplate).withFunctionName("OVERDUE_COUNT");
        this.escalateOverdue = new SimpleJdbcCall(jdbcTemplate).withProcedureName("ESCALATE_OVERDUE");
    }

    // Calls the function OVERDUE_COUNT(p_priority). Oracle returns a NUMBER, read here as a Number.
    public int countOverdue(Priority priority) {
        Number count = overdueCount.executeFunction(Number.class, priority.name());
        return count.intValue();
    }

    // Calls the procedure ESCALATE_OVERDUE and reads its OUT parameter.
    public int escalateOverdue() {
        Map<String, Object> result = escalateOverdue.execute();
        return ((Number) result.get("P_ESCALATED")).intValue();
    }
}
