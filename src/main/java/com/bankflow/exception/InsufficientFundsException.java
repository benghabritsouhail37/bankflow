package com.bankflow.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long accountId) {
        super("Insufficient funds for account with id: " + accountId);
    }
}