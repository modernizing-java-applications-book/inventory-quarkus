package com.redhat.cloudnative;

public class InvalidInventoryException extends RuntimeException {

    public InvalidInventoryException(String message) {
        super(message);
    }
}