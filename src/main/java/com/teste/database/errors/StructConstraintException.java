package com.teste.database.errors;

public class StructConstraintException extends RuntimeException {
    public StructConstraintException(String message) {
        super(message);
    }
    public StructConstraintException(String message, String json) {
        super(message);
    }
}
