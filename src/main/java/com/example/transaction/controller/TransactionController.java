package com.example.transaction.controller;

import com.example.transaction.exception.InvalidTransactionException;
import com.example.transaction.exception.TransactionNotFoundException;
import com.example.transaction.model.Page;
import com.example.transaction.model.Transaction;
import com.example.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction API", description = "API for managing financial transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new transaction")
    public Transaction createTransaction(@Valid @RequestBody Transaction transaction) {
        return transactionService.createTransaction(transaction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public Transaction getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @GetMapping
    @Operation(summary = "Get all transactions with pagination")
    public Page<Transaction> getAllTransactions(
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size) {
        // Convert 1-based page to 0-based for service layer
        int zeroBasedPage = page > 0 ? page - 1 : 0;
        return transactionService.getAllTransactions(zeroBasedPage, size);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing transaction")
    public Transaction updateTransaction(@PathVariable Long id, @Valid @RequestBody Transaction transaction) {
        try {
            return transactionService.updateTransaction(id, transaction);
        } catch (TransactionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidTransactionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a transaction")
    public void deleteTransaction(@PathVariable Long id) {
        try {
            transactionService.deleteTransaction(id);
        } catch (TransactionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
