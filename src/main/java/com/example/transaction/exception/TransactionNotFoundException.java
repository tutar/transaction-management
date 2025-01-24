package com.example.transaction.exception;

public class TransactionNotFoundException extends TransactionException {
    public TransactionNotFoundException(Long id) {
        super("Transaction not found with id: " + id);
    }
}
