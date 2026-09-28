
package com.bankflow.controller;

import com.bankflow.dto.AccountResponse;
import com.bankflow.dto.CreateAccountRequest;

import com.bankflow.entity.Account;

import com.bankflow.service.AccountService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        Account account = accountService.createAccount(
                request.userId(),
                request.currency()
        );

        return AccountResponse.from(account);
    }
}