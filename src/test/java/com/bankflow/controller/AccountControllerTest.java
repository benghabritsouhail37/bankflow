package com.bankflow.controller;

import com.bankflow.dto.AccountResponse;
import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;
import com.bankflow.entity.User;
import com.bankflow.exception.GlobalExceptionHandler;
import com.bankflow.service.AccountService;
import com.bankflow.exception.UserNotFoundException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}