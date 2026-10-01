package com.bankflow.exception;

public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException(Long id) {
        super("Account is blocked with id: " + id);
    }
}