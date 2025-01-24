package com.example.transaction.exception;

public class InvalidTransactionException extends TransactionException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
