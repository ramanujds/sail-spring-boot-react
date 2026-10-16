package com.bsp.supportassistant.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

// Calls the package SUPPORT_ERROR_PKG.
@Repository
public class ErrorLogRepository {

    private final SimpleJdbcCall logError;

    public ErrorLogRepository(JdbcTemplate jdbcTemplate) {
        this.logError = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("SUPPORT_ERROR_PKG").withProcedureName("LOG_ERROR");
    }

    public void logError(String message) {
        logError.execute(message);
    }
}
