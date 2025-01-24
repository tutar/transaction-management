package com.example.transaction.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class Transaction {
    /** 交易ID */
    private Long id;
    
    /** 交易类型 */
    private TransactionType type;
    
    /** 
     * 交易金额
     * @NotNull 金额不能为空
     * @DecimalMin 金额必须大于0
     */
    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Transaction amount must be greater than 0")
    private BigDecimal amount;
    
    /** 交易描述 */
    private String description;
    
    /** 交易时间戳 */
    private LocalDateTime timestamp;
    
    /** 交易状态，默认为PENDING */
    private TransactionStatus status = TransactionStatus.PENDING;
    
    /** 目标账户（用于转账交易） */
    private String targetAccount;
    
    /** 原始交易ID（用于退款交易） */
    private Long originalTransactionId;
    
    /** 交易发起方（用于系统交易） */
    private String initiatedBy;

    // Getters and Setters
    public String getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(String targetAccount) {
        this.targetAccount = targetAccount;
    }

    public Long getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(Long originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}
