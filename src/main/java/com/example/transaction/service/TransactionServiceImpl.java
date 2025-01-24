package com.example.transaction.service;

import com.example.transaction.exception.InvalidTransactionException;
import com.example.transaction.exception.TransactionNotFoundException;
import com.example.transaction.model.Page;
import com.example.transaction.model.Transaction;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final ConcurrentHashMap<Long, Transaction> transactions = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Transaction createTransaction(Transaction transaction) {
        if (transaction.getId() != null && transactions.containsKey(transaction.getId())) {
            throw new InvalidTransactionException("Transaction with ID " + transaction.getId() + " already exists");
        }
        
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be greater than 0");
        }
        
        // 根据交易类型进行特定验证
        switch (transaction.getType()) {
            case WITHDRAWAL:
            case WITHDRAW:
                validateWithdrawal(transaction);
                break;
            case TRANSFER:
                validateTransfer(transaction);
                break;
            case REFUND:
                validateRefund(transaction);
                break;
            case INTEREST_INCOME:
            case INTEREST_EXPENSE:
            case FEE_INCOME:
            case FEE_EXPENSE:
                validateSystemTransaction(transaction);
                break;
            case DEPOSIT:
            default:
                // 存款不需要额外验证
                break;
        }
        
        long id = idCounter.getAndIncrement();
        transaction.setId(id);
        transaction.setTimestamp(java.time.LocalDateTime.now());
        transactions.put(id, transaction);
        return transaction;
    }

    private void validateWithdrawal(Transaction transaction) {
        // 提现和取款需要验证账户余额
        BigDecimal balance = calculateAccountBalance();
        if (transaction.getAmount().compareTo(balance) > 0) {
            throw new InvalidTransactionException("Insufficient balance for withdrawal");
        }
    }

    private void validateTransfer(Transaction transaction) {
        // 转账需要验证目标账户
        if (transaction.getTargetAccount() == null) {
            throw new InvalidTransactionException("Target account is required for transfer");
        }
    }

    private void validateRefund(Transaction transaction) {
        // 退款需要关联原始交易
        if (transaction.getOriginalTransactionId() == null) {
            throw new InvalidTransactionException("Original transaction ID is required for refund");
        }
    }

    private void validateSystemTransaction(Transaction transaction) {
        // 系统交易需要验证发起方
        if (transaction.getInitiatedBy() == null || !transaction.getInitiatedBy().equals("SYSTEM")) {
            throw new InvalidTransactionException("System transactions must be initiated by SYSTEM");
        }
    }

    private BigDecimal calculateAccountBalance() {
        // 计算账户余额的简单实现
        return transactions.values().stream()
            .map(t -> {
                switch (t.getType()) {
                    case DEPOSIT:
                    case INTEREST_INCOME:
                    case FEE_INCOME:
                    case REFUND:
                        return t.getAmount();
                    case WITHDRAWAL:
                    case WITHDRAW:
                    case INTEREST_EXPENSE:
                    case FEE_EXPENSE:
                        return t.getAmount().negate();
                    case TRANSFER:
                        return BigDecimal.ZERO;
                    default:
                        return BigDecimal.ZERO;
                }
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Cacheable(value = "transactions", key = "#id")
    public Optional<Transaction> getTransactionById(Long id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public Page<Transaction> getAllTransactions(int page, int size) {
        List<Transaction> content = transactions.values().stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
        
        long totalElements = transactions.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        return new Page<>(content, page + 1, totalPages, totalElements);
    }

    @Override
    @CachePut(value = "transactions", key = "#id")
    public Transaction updateTransaction(Long id, Transaction transaction) {
        if (transactions.containsKey(id)) {
            transaction.setId(id);
            transactions.put(id, transaction);
            return transaction;
        }
        throw new TransactionNotFoundException(id);
    }

    @Override
    @CacheEvict(value = "transactions", key = "#id")
    public void deleteTransaction(Long id) {
        if (!transactions.containsKey(id)) {
            throw new TransactionNotFoundException(id);
        }
        transactions.remove(id);
    }

    // For testing purposes only
    public void clearTransactions() {
        transactions.clear();
        idCounter.set(1);
    }
}
