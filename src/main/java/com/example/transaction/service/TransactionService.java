package com.example.transaction.service;

import com.example.transaction.model.Page;
import com.example.transaction.model.Transaction;
import java.util.Optional;

public interface TransactionService {
    Transaction createTransaction(Transaction transaction);
    Optional<Transaction> getTransactionById(Long id);
    Page<Transaction> getAllTransactions(int page, int size);
    Transaction updateTransaction(Long id, Transaction transaction);
    void deleteTransaction(Long id);
}
