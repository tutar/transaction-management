package com.example.transaction.controller;

import com.example.transaction.model.Transaction;
import com.example.transaction.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    classes = com.example.transaction.TransactionManagementApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.test.context.cache.maxSize=1"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransactionControllerLoadTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final int THREAD_COUNT = 10;
    private final int REQUEST_COUNT = 100;
    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                }
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting for executor to terminate");
                Thread.currentThread().interrupt();
            }
        }
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.main.allow-bean-definition-overriding", () -> "true");
    }


    /**
     * 基本并发测试
     * @throws InterruptedException
     */
    @Test
    void testConcurrentTransactions() throws InterruptedException {
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // 清理测试数据
        restTemplate.delete("/api/transactions");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < REQUEST_COUNT; i++) {
            executor.execute(() -> {
                Transaction transaction = new Transaction();
                transaction.setType(TransactionType.DEPOSIT);
                transaction.setAmount(new BigDecimal("100.00"));
                transaction.setDescription("Load test transaction");

                ResponseEntity<Transaction> response = restTemplate.postForEntity(
                    "/api/transactions",
                    transaction, 
                    Transaction.class
                );

                if (response.getStatusCode() == HttpStatus.CREATED) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Load test results:");
        System.out.println("Total requests: " + REQUEST_COUNT);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + failureCount.get());
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Requests per second: " + (REQUEST_COUNT * 1000.0 / duration));

        assertEquals(REQUEST_COUNT, successCount.get());
    }

    /**
     * 混合操作测试
     * @throws InterruptedException
     */
    @Test
    void testMixedOperationsLoad() throws InterruptedException {
        int threadCount = 50;
        int operationsPerThread = 20;
        executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger createSuccess = new AtomicInteger(0);
        AtomicInteger readSuccess = new AtomicInteger(0);
        AtomicInteger updateSuccess = new AtomicInteger(0);
        AtomicInteger deleteSuccess = new AtomicInteger(0);

        // 清理测试数据
        restTemplate.delete("/api/transactions");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    // Create
                    Transaction transaction = new Transaction();
                    transaction.setType(TransactionType.DEPOSIT);
                    transaction.setAmount(new BigDecimal("100.00"));
                    transaction.setDescription("Mixed load test");

                    ResponseEntity<Transaction> createResponse = restTemplate.postForEntity(
                        "/api/transactions",
                        transaction, 
                        Transaction.class
                    );

                    if (createResponse.getStatusCode() == HttpStatus.CREATED) {
                        createSuccess.incrementAndGet();
                        Long id = createResponse.getBody().getId();

                        // Read
                        ResponseEntity<Transaction> readResponse = restTemplate.getForEntity(
                            "/api/transactions/" + id,
                            Transaction.class
                        );
                        if (readResponse.getStatusCode() == HttpStatus.OK) {
                            readSuccess.incrementAndGet();
                        }

                        // Update
                        transaction.setAmount(new BigDecimal("200.00"));
                        ResponseEntity<Transaction> updateResponse = restTemplate.exchange(
                            "/api/transactions/" + id,
                            HttpMethod.PUT,
                            new HttpEntity<>(transaction),
                            Transaction.class
                        );
                        if (updateResponse.getStatusCode() == HttpStatus.OK) {
                            updateSuccess.incrementAndGet();
                        }

                        // Delete
                        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                            "/api/transactions/" + id,
                            HttpMethod.DELETE,
                            null,
                            Void.class
                        );
                        if (deleteResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                            deleteSuccess.incrementAndGet();
                        }
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Mixed operations load test results:");
        System.out.println("Total threads: " + threadCount);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Successful creates: " + createSuccess.get());
        System.out.println("Successful reads: " + readSuccess.get());
        System.out.println("Successful updates: " + updateSuccess.get());
        System.out.println("Successful deletes: " + deleteSuccess.get());
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Operations per second: " + 
            (threadCount * operationsPerThread * 4 * 1000.0 / duration));
    }

    /**
     * 资源监控测试
     * @throws InterruptedException
     */
    @Test
    void testResourceMonitoring() throws InterruptedException {
        int threadCount = 50;
        int operationsPerThread = 100;
        executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 清理测试数据
        restTemplate.delete("/api/transactions");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        Transaction transaction = new Transaction();
                        transaction.setType(TransactionType.DEPOSIT);
                        transaction.setAmount(new BigDecimal("100.00"));
                        transaction.setDescription("Resource monitoring test");

                        ResponseEntity<Transaction> response = restTemplate.postForEntity(
                            "/api/transactions",
                            transaction, 
                            Transaction.class
                        );

                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 获取系统资源使用情况
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        int availableProcessors = runtime.availableProcessors();

        System.out.println("Resource monitoring test results:");
        System.out.println("Total threads: " + threadCount);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + failureCount.get());
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Operations per second: " + 
            (threadCount * operationsPerThread * 1000.0 / duration));
        System.out.println("Memory usage: " + (usedMemory / 1024 / 1024) + " MB");
        System.out.println("Available processors: " + availableProcessors);
    }

    /**
     * 长时间运行稳定性测试
     * @throws InterruptedException
     */
    @Test
    void testLongRunningStability() throws InterruptedException {
        int durationMinutes = 5;
        int threadCount = 20;
        executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long endTime = System.currentTimeMillis() + durationMinutes * 60 * 1000;

        while (System.currentTimeMillis() < endTime) {
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        // Create
                        Transaction transaction = new Transaction();
                        transaction.setType(TransactionType.DEPOSIT);
                        transaction.setAmount(new BigDecimal("100.00"));
                        transaction.setDescription("Stability test");

                        ResponseEntity<Transaction> response = restTemplate.postForEntity(
                            "/api/transactions",
                            transaction, 
                            Transaction.class
                        );

                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                });
            }
            Thread.sleep(100); // Add small delay between batches
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        System.out.println("Stability test results:");
        System.out.println("Duration: " + durationMinutes + " minutes");
        System.out.println("Thread count: " + threadCount);
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + failureCount.get());
        System.out.println("Operations per second: " + 
            (successCount.get() / (durationMinutes * 60.0)));
    }
}
