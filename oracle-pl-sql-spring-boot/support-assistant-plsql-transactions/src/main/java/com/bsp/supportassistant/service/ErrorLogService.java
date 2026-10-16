package com.bsp.supportassistant.service;

import com.bsp.supportassistant.repository.ErrorLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Writes a line to SUPPORT_ERROR_LOG that stays, even when the caller's transaction is rolled back.
// It is a class of its own on purpose: a call from inside the same class does not pass through Spring.
@Service
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    public ErrorLogService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    // REQUIRES_NEW: pause the caller's transaction, run this one on its own, and commit it at once.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String message) {
        errorLogRepository.logError(message);
    }
}
