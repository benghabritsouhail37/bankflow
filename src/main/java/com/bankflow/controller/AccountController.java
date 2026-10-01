
package com.bankflow.controller;

import com.bankflow.dto.AccountResponse;
import com.bankflow.dto.CreateAccountRequest;
import com.bankflow.dto.DepositRequest;
import com.bankflow.dto.WithdrawalRequest;
import com.bankflow.dto.TransactionResponse;

import com.bankflow.entity.Account;

import com.bankflow.service.AccountService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PostMapping("/{accountId}/deposit")
public AccountResponse deposit(
        @PathVariable Long accountId,
        @Valid @RequestBody DepositRequest request) {

    Account account = accountService.deposit(
            accountId,
            request.amount()
    );

    return AccountResponse.from(account);
}
@PostMapping("/{accountId}/withdraw")
public AccountResponse withdraw(
        @PathVariable Long accountId,
        @Valid @RequestBody WithdrawalRequest request) {

    Account account = accountService.withdraw(
            accountId,
            request.amount()
    );

    return AccountResponse.from(account);
}
@GetMapping("/{accountId}/transactions")
public List<TransactionResponse> getTransactions(
        @PathVariable Long accountId) {

    return accountService
            .getTransactions(accountId)
            .stream()
            .map(TransactionResponse::from)
            .toList();
}
@GetMapping("/user/{userId}")
public List<AccountResponse> getAccountsByUser(
        @PathVariable Long userId) {

    return accountService
            .getAccountsByUserId(userId)
            .stream()
            .map(AccountResponse::from)
            .toList();
}
}