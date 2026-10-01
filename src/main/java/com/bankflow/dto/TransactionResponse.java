package com.bankflow.dto;

import com.bankflow.entity.Transaction;
import com.bankflow.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(

        Long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Long accountId,
        LocalDateTime createdAt

) {

    public static TransactionResponse from(
            Transaction transaction) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getAccount().getId(),
                transaction.getCreatedAt()
        );
    }
}