package com.example.transaction.service;

import com.example.transaction.model.Page;
import com.example.transaction.model.Transaction;
import com.example.transaction.model.TransactionType;
import com.example.transaction.exception.InvalidTransactionException;
import com.example.transaction.exception.TransactionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCacheAndTransactions() {
        cacheManager.getCache("transactions").clear();
        ((TransactionServiceImpl) transactionService).clearTransactions();
    }

    /**
     * 测试创建金额为0的交易
     * 预期抛出InvalidTransactionException异常
     */
    @Test
    void testCreateTransactionWithZeroAmount() {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(BigDecimal.ZERO);
        transaction.setDescription("Zero amount");

        assertThrows(InvalidTransactionException.class, 
            () -> transactionService.createTransaction(transaction));
    }

    /**
     * 测试创建金额为负数的交易
     * 预期抛出InvalidTransactionException异常
     */
    @Test
    void testCreateTransactionWithNegativeAmount() {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(new BigDecimal("-100.00"));
        transaction.setDescription("Negative amount");

        assertThrows(InvalidTransactionException.class, 
            () -> transactionService.createTransaction(transaction));
    }

    /**
     * 测试并发创建交易
     * 验证在多线程环境下交易创建的正确性
     */
    @Test
    void testConcurrentTransactionCreation() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    Transaction transaction = new Transaction();
                    transaction.setType(TransactionType.DEPOSIT);
                    transaction.setAmount(new BigDecimal("100.00"));
                    transaction.setDescription("Concurrent test");
                    transactionService.createTransaction(transaction);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Page<Transaction> page = transactionService.getAllTransactions(0, 100);
        assertEquals(threadCount, page.getTotalElements());
    }

    /**
     * 测试余额不足时的取款操作
     * 预期抛出InvalidTransactionException异常
     */
    @Test
    void testWithdrawalWithInsufficientBalance() {
        // 先存入100元
        Transaction deposit = createTestTransaction();
        deposit.setType(TransactionType.DEPOSIT);
        deposit.setAmount(new BigDecimal("100.00"));
        transactionService.createTransaction(deposit);
        
        // 尝试取款200元
        Transaction withdrawal = createTestTransaction();
        withdrawal.setType(TransactionType.WITHDRAWAL);
        withdrawal.setAmount(new BigDecimal("200.00"));
        
        assertThrows(InvalidTransactionException.class, 
            () -> transactionService.createTransaction(withdrawal));
    }

    /**
     * 测试缺少目标账户的转账操作
     * 预期抛出InvalidTransactionException异常
     */
    @Test
    void testTransferWithoutTargetAccount() {
        Transaction transfer = createTestTransaction();
        transfer.setType(TransactionType.TRANSFER);
        transfer.setAmount(new BigDecimal("100.00"));
        
        assertThrows(InvalidTransactionException.class, 
            () -> transactionService.createTransaction(transfer));
    }

    /**
     * 测试缺少原始交易ID的退款操作
     * 预期抛出InvalidTransactionException异常
     */
    @Test
    void testRefundWithoutOriginalTransactionId() {
        Transaction refund = createTestTransaction();
        refund.setType(TransactionType.REFUND);
        refund.setAmount(new BigDecimal("100.00"));
        
        assertThrows(InvalidTransactionException.class, 
            () -> transactionService.createTransaction(refund));
    }

    /**
     * 测试非系统发起的系统交易
     * 预期抛出InvalidTransactionException异常
     */
    @Test
    void testSystemTransactionWithoutSystemInitiation() {
        Transaction systemTx = createTestTransaction();
        systemTx.setType(TransactionType.INTEREST_INCOME);
        systemTx.setAmount(new BigDecimal("100.00"));
        
        assertThrows(InvalidTransactionException.class, 
            () -> transactionService.createTransaction(systemTx));
    }

    /**
     * 测试创建重复交易
     * 预期抛出InvalidTransactionException异常
     */
    @Test
    void testCreateDuplicateTransaction() {
        Transaction transaction = createTestTransaction();
        transactionService.createTransaction(transaction);
        
        // 尝试创建相同ID的交易
        assertThrows(InvalidTransactionException.class, 
            () -> transactionService.createTransaction(transaction));
    }

    /**
     * 测试删除不存在的交易
     * 预期抛出TransactionNotFoundException异常
     */
    @Test
    void testDeleteNonExistingTransaction() {
        assertThrows(TransactionNotFoundException.class, 
            () -> transactionService.deleteTransaction(999L));
    }

    /**
     * 测试更新交易后的缓存失效
     * 验证缓存更新机制的正确性
     */
    @Test
    void testCacheEvictionAfterUpdate() {
        Transaction transaction = createTestTransaction();
        Transaction created = transactionService.createTransaction(transaction);
        
        // First get - should populate cache
        transactionService.getTransactionById(created.getId());
        
        // Update transaction
        created.setAmount(new BigDecimal("200.00"));
        transactionService.updateTransaction(created.getId(), created);
        
        // Second get - should reflect updated value
        Transaction updated = transactionService.getTransactionById(created.getId())
                .orElseThrow();
        assertEquals(new BigDecimal("200.00"), updated.getAmount());
    }

    // @Test
    // void testTransactionRollbackOnFailure() {
    //     Transaction transaction1 = createTestTransaction();
    //     Transaction transaction2 = createTestTransaction();
    //     transaction2.setAmount(null); // Invalid amount
    //     
    //     try {
    //         transactionService.createTransaction(transaction1);
    //         transactionService.createTransaction(transaction2);
    //         fail("Expected exception not thrown");
    //     } catch (Exception e) {
    //         // Verify transaction1 was rolled back
    //         Page<Transaction> page = transactionService.getAllTransactions(0, 10);
    //         assertEquals(0, page.getTotalElements());
    //     }
    // }

    private Transaction createTestTransaction() {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setDescription("Test transaction");
        return transaction;
    }

    private void createTestTransactions(int count) {
        for (int i = 0; i < count; i++) {
            Transaction transaction = new Transaction();
            transaction.setType(TransactionType.TRANSFER);
            transaction.setAmount(new BigDecimal((i + 1) * 100));
            transaction.setDescription("Test transaction " + i);
            transactionService.createTransaction(transaction);
        }
    }
}
