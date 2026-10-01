package com.bankflow.controller;

import com.bankflow.dto.AccountResponse;
import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;
import com.bankflow.entity.User;
import com.bankflow.exception.GlobalExceptionHandler;
import com.bankflow.service.AccountService;
import com.bankflow.exception.UserNotFoundException;
import com.bankflow.exception.InsufficientFundsException;
import com.bankflow.entity.Transaction;
import com.bankflow.entity.TransactionType;

import com.bankflow.exception.AccountBlockedException;
import com.bankflow.exception.AccountNotFoundException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void shouldReturn201WhenAccountIsCreated() throws Exception {

        // GIVEN : utilisateur propriétaire
        User user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");

        // Compte retourné par le service simulé
        Account account = new Account();

        account.setAccountNumber(
                "BF-998ED164DA39438A8A8E89E9E90E6F09"
        );

        account.setBalance(
                new BigDecimal("0.00")
        );

        account.setCurrency(
                AccountCurrency.MAD
        );

        account.setStatus(
                AccountStatus.ACTIVE
        );

        account.setUser(user);

        when(
                accountService.createAccount(
                        1L,
                        AccountCurrency.MAD
                )
        ).thenReturn(account);

        // WHEN + THEN
        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "userId": 1,
                                          "currency": "MAD"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.accountNumber")
                                .value(
                                        "BF-998ED164DA39438A8A8E89E9E90E6F09"
                                )
                )
                .andExpect(
                        jsonPath("$.balance")
                                .value(0.00)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("MAD")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(1)
                );
    }
    @Test
void shouldReturn404WhenUserDoesNotExist() throws Exception {

    // GIVEN : le service ne trouve pas l'utilisateur
    when(
            accountService.createAccount(
                    999L,
                    AccountCurrency.MAD
            )
    ).thenThrow(
            new UserNotFoundException(999L)
    );

    // WHEN + THEN
    mockMvc.perform(
                    post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": 999,
                                      "currency": "MAD"
                                    }
                                    """)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                    jsonPath("$.title")
                            .value("User not found")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(404)
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value("User not found with id: 999")
            );
}
// TEST 3 : devise manquante -> 400
@Test
void shouldReturn400WhenCurrencyIsMissing() throws Exception {

    // WHEN + THEN
    mockMvc.perform(
                    post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": 1
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                    jsonPath("$.title")
                            .value("Validation failed")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(400)
            )
            .andExpect(
                    jsonPath("$.errors.currency")
                            .value("Account currency is required")
            );

    // La validation doit bloquer la requête
    // avant l'appel au service
    verify(
            accountService,
            never()
    ).createAccount(
            anyLong(),
            any(AccountCurrency.class)
    );
}
// TEST 4 : devise invalide -> 400
@Test
void shouldReturn400WhenCurrencyIsInvalid() throws Exception {

    mockMvc.perform(
                    post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": 1,
                                      "currency": "USD"
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                    jsonPath("$.title")
                            .value("Invalid request")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(400)
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value(
                                    "The request body is missing or contains invalid values"
                            )
            );

    verify(
            accountService,
            never()
    ).createAccount(
            anyLong(),
            any(AccountCurrency.class)
    );
}
// =========================================================
// DEPOSIT TEST 1
// Dépôt réussi -> 200
// =========================================================

@Test
void shouldReturn200WhenDepositIsSuccessful() throws Exception {

    User user = new User();
    user.setId(1L);
    user.setFirstName("Test");
    user.setLastName("User");
    user.setEmail("test@example.com");

    Account account = new Account();
    account.setAccountNumber(
            "BF-998ED164DA39438A8A8E89E9E90E6F09"
    );
    account.setBalance(new BigDecimal("150.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.ACTIVE);
    account.setUser(user);

    when(
            accountService.deposit(
                    1L,
                    new BigDecimal("50.00")
            )
    ).thenReturn(account);

    mockMvc.perform(
                    post("/api/accounts/1/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 50.00
                                    }
                                    """)
            )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.balance")
                            .value(150.00)
            )
            .andExpect(
                    jsonPath("$.currency")
                            .value("MAD")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value("ACTIVE")
            )
            .andExpect(
                    jsonPath("$.userId")
                            .value(1)
            );
}   
// =========================================================
// DEPOSIT TEST 2
// Compte inexistant -> 404
// =========================================================

@Test
void shouldReturn404WhenDepositingIntoUnknownAccount() throws Exception {

    when(
            accountService.deposit(
                    999L,
                    new BigDecimal("50.00")
            )
    ).thenThrow(
            new AccountNotFoundException(999L)
    );

    mockMvc.perform(
                    post("/api/accounts/999/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 50.00
                                    }
                                    """)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                    jsonPath("$.title")
                            .value("Account not found")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(404)
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value(
                                    "Account not found with id: 999"
                            )
            );
}   
// =========================================================
// DEPOSIT TEST 3
// Montant = 0 -> 400
// =========================================================

@Test
void shouldReturn400WhenDepositAmountIsZero() throws Exception {

    mockMvc.perform(
                    post("/api/accounts/1/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 0
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                    jsonPath("$.title")
                            .value("Validation failed")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(400)
            )
            .andExpect(
                    jsonPath("$.errors.amount")
                            .value(
                                    "Amount must be greater than 0"
                            )
            );

    verify(
            accountService,
            never()
    ).deposit(
            anyLong(),
            any(BigDecimal.class)
    );
}
// =========================================================
// DEPOSIT TEST 4
// Compte bloqué -> 409
// =========================================================

@Test
void shouldReturn409WhenDepositingIntoBlockedAccount()
        throws Exception {

    when(
            accountService.deposit(
                    1L,
                    new BigDecimal("50.00")
            )
    ).thenThrow(
            new AccountBlockedException(1L)
    );

    mockMvc.perform(
                    post("/api/accounts/1/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 50.00
                                    }
                                    """)
            )
            .andExpect(status().isConflict())
            .andExpect(
                    jsonPath("$.title")
                            .value("Account blocked")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(409)
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value(
                                    "Account is blocked with id: 1"
                            )
            );
}
// =========================================================
// WITHDRAWAL TEST 1
// Retrait réussi -> 200
// =========================================================

@Test
void shouldReturn200WhenWithdrawalIsSuccessful() throws Exception {

    User user = new User();
    user.setId(1L);
    user.setFirstName("Test");
    user.setLastName("User");
    user.setEmail("test@example.com");

    Account account = new Account();
    account.setAccountNumber(
            "BF-998ED164DA39438A8A8E89E9E90E6F09"
    );
    account.setBalance(new BigDecimal("300.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.ACTIVE);
    account.setUser(user);

    when(
            accountService.withdraw(
                    1L,
                    new BigDecimal("200.00")
            )
    ).thenReturn(account);

    mockMvc.perform(
                    post("/api/accounts/1/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 200.00
                                    }
                                    """)
            )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.balance")
                            .value(300.00)
            )
            .andExpect(
                    jsonPath("$.currency")
                            .value("MAD")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value("ACTIVE")
            )
            .andExpect(
                    jsonPath("$.userId")
                            .value(1)
            );
}
// =========================================================
// WITHDRAWAL TEST 2
// Compte inexistant -> 404
// =========================================================

@Test
void shouldReturn404WhenWithdrawingFromUnknownAccount()
        throws Exception {

    when(
            accountService.withdraw(
                    999L,
                    new BigDecimal("50.00")
            )
    ).thenThrow(
            new AccountNotFoundException(999L)
    );

    mockMvc.perform(
                    post("/api/accounts/999/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 50.00
                                    }
                                    """)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                    jsonPath("$.title")
                            .value("Account not found")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(404)
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value(
                                    "Account not found with id: 999"
                            )
            );
}
// =========================================================
// WITHDRAWAL TEST 3
// Montant zéro -> 400
// =========================================================

@Test
void shouldReturn400WhenWithdrawalAmountIsZero()
        throws Exception {

    mockMvc.perform(
                    post("/api/accounts/1/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 0
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                    jsonPath("$.title")
                            .value("Validation failed")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(400)
            )
            .andExpect(
                    jsonPath("$.errors.amount")
                            .value(
                                    "Amount must be greater than 0"
                            )
            );

    verify(
            accountService,
            never()
    ).withdraw(
            anyLong(),
            any(BigDecimal.class)
    );
}
// =========================================================
// WITHDRAWAL TEST 4
// Compte bloqué -> 409
// =========================================================

@Test
void shouldReturn409WhenWithdrawingFromBlockedAccount()
        throws Exception {

    when(
            accountService.withdraw(
                    1L,
                    new BigDecimal("50.00")
            )
    ).thenThrow(
            new AccountBlockedException(1L)
    );

    mockMvc.perform(
                    post("/api/accounts/1/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 50.00
                                    }
                                    """)
            )
            .andExpect(status().isConflict())
            .andExpect(
                    jsonPath("$.title")
                            .value("Account blocked")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(409)
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value(
                                    "Account is blocked with id: 1"
                            )
            );
}
// =========================================================
// WITHDRAWAL TEST 5
// Solde insuffisant -> 409
// =========================================================

@Test
void shouldReturn409WhenBalanceIsInsufficient()
        throws Exception {

    when(
            accountService.withdraw(
                    1L,
                    new BigDecimal("500.00")
            )
    ).thenThrow(
            new InsufficientFundsException(1L)
    );

    mockMvc.perform(
                    post("/api/accounts/1/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 500.00
                                    }
                                    """)
            )
            .andExpect(status().isConflict())
            .andExpect(
                    jsonPath("$.title")
                            .value("Insufficient funds")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(409)
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value(
                                    "Insufficient funds for account with id: 1"
                            )
            );
}
@Test
void shouldReturnTransactionHistory() throws Exception {

    Account account = new Account();

    Transaction deposit = new Transaction();
    deposit.setType(TransactionType.DEPOSIT);
    deposit.setAmount(new BigDecimal("500.00"));
    deposit.setBalanceAfter(new BigDecimal("500.00"));
    deposit.setAccount(account);

    Transaction withdrawal = new Transaction();
    withdrawal.setType(TransactionType.WITHDRAWAL);
    withdrawal.setAmount(new BigDecimal("100.00"));
    withdrawal.setBalanceAfter(new BigDecimal("400.00"));
    withdrawal.setAccount(account);

    when(accountService.getTransactions(1L))
            .thenReturn(
                    List.of(withdrawal, deposit)
            );

    mockMvc.perform(
                    get("/api/accounts/1/transactions")
            )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.length()")
                            .value(2)
            )
            .andExpect(
                    jsonPath("$[0].type")
                            .value("WITHDRAWAL")
            )
            .andExpect(
                    jsonPath("$[0].amount")
                            .value(100.00)
            )
            .andExpect(
                    jsonPath("$[0].balanceAfter")
                            .value(400.00)
            )
            .andExpect(
                    jsonPath("$[1].type")
                            .value("DEPOSIT")
            );
}
@Test
void shouldReturn404WhenTransactionHistoryAccountDoesNotExist()
        throws Exception {

    when(accountService.getTransactions(999L))
            .thenThrow(
                    new AccountNotFoundException(999L)
            );

    mockMvc.perform(
                    get("/api/accounts/999/transactions")
            )
            .andExpect(status().isNotFound())
            .andExpect(
                    jsonPath("$.title")
                            .value("Account not found")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(404)
            )
            .andExpect(
                    jsonPath("$.detail")
                            .value(
                                    "Account not found with id: 999"
                            )
            );
}
}