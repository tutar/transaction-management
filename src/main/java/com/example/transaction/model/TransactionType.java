package com.example.transaction.model;

public enum TransactionType {
    DEPOSIT("存款"),
    WITHDRAWAL("取款"), 
    TRANSFER("转账"),
    REFUND("退款"),
    WITHDRAW("提现"),
    INTEREST_INCOME("利息收入"),
    INTEREST_EXPENSE("利息支出"),
    FEE_INCOME("手续费收入"),
    FEE_EXPENSE("手续费支出");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
