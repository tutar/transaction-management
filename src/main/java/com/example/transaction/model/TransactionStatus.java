package com.example.transaction.model;

public enum TransactionStatus {
    PENDING("待处理"),
    PROCESSING("处理中"),
    COMPLETED("已完成"),
    FAILED("失败"),
    CANCELLED("已取消"),
    REFUNDED("已退款");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
