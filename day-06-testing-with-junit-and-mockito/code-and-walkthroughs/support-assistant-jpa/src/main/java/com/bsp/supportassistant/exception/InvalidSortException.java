package com.bsp.supportassistant.exception;

// Thrown when ?sort= names a field that a ticket list cannot be sorted by.
public class InvalidSortException extends RuntimeException {

    public InvalidSortException(String field) {
        super("Cannot sort by '" + field + "'");
    }
}
