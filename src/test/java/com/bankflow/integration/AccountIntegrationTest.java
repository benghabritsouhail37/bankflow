package com.bankflow.integration;

import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;
import com.bankflow.entity.User;

import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;


    // TEST 1 : créer réellement un compte dans PostgreSQL
    @Test
    void shouldCreateAccountAndSaveItInDatabase() throws Exception {

        // ==============================
        // GIVEN : créer un vrai utilisateur
        // ==============================

        User user = new User();

        user.setFirstName("Account");
        user.setLastName("Integration");

        user.setEmail(
                "account-integration-"
                        + UUID.randomUUID()
                        + "@example.com"
        );

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());


        // ==============================
        // WHEN : créer un compte via l'API
        // ==============================

        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "userId": %d,
                                          "currency": "MAD"
                                        }
                                        """.formatted(savedUser.getId()))
                )

                // ==============================
                // THEN : vérifier la réponse HTTP
                // ==============================

                .andExpect(status().isCreated())

                .andExpect(
                        jsonPath("$.accountNumber")
                                .isNotEmpty()
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
                                .value(savedUser.getId())
                );


        // ==============================
        // THEN : vérifier PostgreSQL
        // ==============================

        List<Account> accounts =
                accountRepository.findByUserId(
                        savedUser.getId()
                );

        assertFalse(accounts.isEmpty());

        assertEquals(
                1,
                accounts.size()
        );

        Account savedAccount = accounts.getFirst();

        // Propriétaire
        assertEquals(
                savedUser.getId(),
                savedAccount.getUser().getId()
        );

        // Devise
        assertEquals(
                AccountCurrency.MAD,
                savedAccount.getCurrency()
        );

        // Statut
        assertEquals(
                AccountStatus.ACTIVE,
                savedAccount.getStatus()
        );

        // Solde initial = 0
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        savedAccount.getBalance()
                )
        );

        // Numéro généré
        assertNotNull(
                savedAccount.getAccountNumber()
        );
    }
    // TEST 2 : refuser la création si l'utilisateur n'existe pas
@Test
void shouldReturn404AndNotCreateAccountWhenUserDoesNotExist() throws Exception {

    // GIVEN : un identifiant utilisateur qui n'existe pas
    Long nonExistingUserId = 999999999L;

    // Compter les comptes avant la requête
    long accountsBefore = accountRepository.count();


    // WHEN + THEN : appeler réellement l'API
    mockMvc.perform(
                    post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": %d,
                                      "currency": "MAD"
                                    }
                                    """.formatted(nonExistingUserId))
            )

            // Vérifier la réponse HTTP
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
                            .value(
                                    "User not found with id: "
                                            + nonExistingUserId
                            )
            );


    // THEN : vérifier qu'aucun compte supplémentaire
    // n'a été enregistré dans PostgreSQL

    long accountsAfter = accountRepository.count();

    assertEquals(
            accountsBefore,
            accountsAfter
    );
}   
@Test
void shouldReturn400AndNotCreateAccountWhenCurrencyIsMissing() throws Exception {

    // GIVEN : créer un utilisateur réel
    User user = new User();
    user.setFirstName("Missing");
    user.setLastName("Currency");
    user.setEmail(
            "missing-currency-"
                    + UUID.randomUUID()
                    + "@example.com"
    );

    User savedUser = userRepository.save(user);

    long accountsBefore = accountRepository.count();

    // WHEN + THEN
    mockMvc.perform(
                    post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": %d
                                    }
                                    """.formatted(savedUser.getId()))
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

    // Vérifier qu'aucun compte n'a été créé
    long accountsAfter = accountRepository.count();

    assertEquals(
            accountsBefore,
            accountsAfter
    );
}
@Test
void shouldReturn400AndNotCreateAccountWhenCurrencyIsInvalid() throws Exception {

    // GIVEN : créer un utilisateur réel
    User user = new User();
    user.setFirstName("Invalid");
    user.setLastName("Currency");
    user.setEmail(
            "invalid-currency-"
                    + UUID.randomUUID()
                    + "@example.com"
    );

    User savedUser = userRepository.save(user);

    long accountsBefore = accountRepository.count();

    // WHEN + THEN
    mockMvc.perform(
                    post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": %d,
                                      "currency": "USD"
                                    }
                                    """.formatted(savedUser.getId()))
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

    // Vérifier qu'aucun compte n'a été créé
    long accountsAfter = accountRepository.count();

    assertEquals(
            accountsBefore,
            accountsAfter
    );
}
// =========================================================
// DEPOSIT INTEGRATION TEST 1
// Dépôt réussi
// =========================================================

@Test
void shouldDepositMoneyAndUpdateBalanceInDatabase() throws Exception {

    // GIVEN : utilisateur réel
    User user = new User();
    user.setFirstName("Deposit");
    user.setLastName("Integration");
    user.setEmail(
            "deposit-" + UUID.randomUUID() + "@example.com"
    );

    User savedUser = userRepository.save(user);

    // Compte réel avec 100 MAD
    Account account = new Account();
    account.setAccountNumber(
            "BF-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase()
    );
    account.setBalance(new BigDecimal("100.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.ACTIVE);
    account.setUser(savedUser);

    Account savedAccount =
            accountRepository.save(account);


    // WHEN : dépôt de 50 MAD
    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + savedAccount.getId()
                                    + "/deposit"
                    )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 50.00
                                    }
                                    """)
            )

            // THEN : réponse API
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
            );


    // Vérification PostgreSQL
    Account updatedAccount =
            accountRepository
                    .findById(savedAccount.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("150.00")
                    .compareTo(updatedAccount.getBalance())
    );
}
@Test
void shouldReturn404WhenDepositingIntoNonExistingAccount()
        throws Exception {

    long accountCountBefore =
            accountRepository.count();

    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + Long.MAX_VALUE
                                    + "/deposit"
                    )
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
            );

    assertEquals(
            accountCountBefore,
            accountRepository.count()
    );
}
@Test
void shouldReturn400AndKeepBalanceWhenDepositAmountIsZero()
        throws Exception {

    User user = new User();
    user.setFirstName("Zero");
    user.setLastName("Deposit");
    user.setEmail(
            "zero-" + UUID.randomUUID() + "@example.com"
    );

    User savedUser = userRepository.save(user);


    Account account = new Account();
    account.setAccountNumber(
            "BF-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase()
    );
    account.setBalance(new BigDecimal("100.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.ACTIVE);
    account.setUser(savedUser);

    Account savedAccount =
            accountRepository.save(account);


    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + savedAccount.getId()
                                    + "/deposit"
                    )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 0
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                    jsonPath("$.errors.amount")
                            .value(
                                    "Amount must be greater than 0"
                            )
            );


    Account unchangedAccount =
            accountRepository
                    .findById(savedAccount.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("100.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
@Test
void shouldReturn409AndKeepBalanceWhenAccountIsBlocked()
        throws Exception {

    User user = new User();
    user.setFirstName("Blocked");
    user.setLastName("Deposit");
    user.setEmail(
            "blocked-" + UUID.randomUUID() + "@example.com"
    );

    User savedUser = userRepository.save(user);


    Account account = new Account();
    account.setAccountNumber(
            "BF-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase()
    );
    account.setBalance(new BigDecimal("100.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.BLOCKED);
    account.setUser(savedUser);

    Account savedAccount =
            accountRepository.save(account);


    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + savedAccount.getId()
                                    + "/deposit"
                    )
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
            );


    Account unchangedAccount =
            accountRepository
                    .findById(savedAccount.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("100.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
// =========================================================
// WITHDRAWAL INTEGRATION TEST 1
// Retrait réussi
// =========================================================

@Test
void shouldWithdrawMoneyAndUpdateBalanceInDatabase() throws Exception {

    User user = new User();
    user.setFirstName("Withdraw");
    user.setLastName("Integration");
    user.setEmail(
            "withdraw-" + UUID.randomUUID() + "@example.com"
    );

    User savedUser = userRepository.save(user);

    Account account = new Account();
    account.setAccountNumber(
            "BF-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase()
    );
    account.setBalance(new BigDecimal("500.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.ACTIVE);
    account.setUser(savedUser);

    Account savedAccount =
            accountRepository.save(account);

    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + savedAccount.getId()
                                    + "/withdraw"
                    )
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
                    jsonPath("$.status")
                            .value("ACTIVE")
            );

    Account updatedAccount =
            accountRepository
                    .findById(savedAccount.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("300.00")
                    .compareTo(updatedAccount.getBalance())
    );
}
// =========================================================
// WITHDRAWAL INTEGRATION TEST 2
// Compte inexistant
// =========================================================

@Test
void shouldReturn404WhenWithdrawingFromNonExistingAccount()
        throws Exception {

    long accountsBefore =
            accountRepository.count();

    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + Long.MAX_VALUE
                                    + "/withdraw"
                    )
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
            );

    assertEquals(
            accountsBefore,
            accountRepository.count()
    );
}
// =========================================================
// WITHDRAWAL INTEGRATION TEST 3
// Montant zéro
// =========================================================

@Test
void shouldReturn400AndKeepBalanceWhenWithdrawalAmountIsZero()
        throws Exception {

    User user = new User();
    user.setFirstName("Zero");
    user.setLastName("Withdrawal");
    user.setEmail(
            "zero-withdraw-"
                    + UUID.randomUUID()
                    + "@example.com"
    );

    User savedUser = userRepository.save(user);

    Account account = new Account();
    account.setAccountNumber(
            "BF-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase()
    );
    account.setBalance(new BigDecimal("500.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.ACTIVE);
    account.setUser(savedUser);

    Account savedAccount =
            accountRepository.save(account);

    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + savedAccount.getId()
                                    + "/withdraw"
                    )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 0
                                    }
                                    """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                    jsonPath("$.errors.amount")
                            .value(
                                    "Amount must be greater than 0"
                            )
            );

    Account unchangedAccount =
            accountRepository
                    .findById(savedAccount.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("500.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
// =========================================================
// WITHDRAWAL INTEGRATION TEST 4
// Compte bloqué
// =========================================================

@Test
void shouldReturn409AndKeepBalanceWhenWithdrawingFromBlockedAccount()
        throws Exception {

    User user = new User();
    user.setFirstName("Blocked");
    user.setLastName("Withdrawal");
    user.setEmail(
            "blocked-withdraw-"
                    + UUID.randomUUID()
                    + "@example.com"
    );

    User savedUser = userRepository.save(user);

    Account account = new Account();
    account.setAccountNumber(
            "BF-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase()
    );
    account.setBalance(new BigDecimal("500.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.BLOCKED);
    account.setUser(savedUser);

    Account savedAccount =
            accountRepository.save(account);

    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + savedAccount.getId()
                                    + "/withdraw"
                    )
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
            );

    Account unchangedAccount =
            accountRepository
                    .findById(savedAccount.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("500.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
// =========================================================
// WITHDRAWAL INTEGRATION TEST 5
// Solde insuffisant
// =========================================================

@Test
void shouldReturn409AndKeepBalanceWhenFundsAreInsufficient()
        throws Exception {

    User user = new User();
    user.setFirstName("Insufficient");
    user.setLastName("Funds");
    user.setEmail(
            "insufficient-"
                    + UUID.randomUUID()
                    + "@example.com"
    );

    User savedUser = userRepository.save(user);

    Account account = new Account();
    account.setAccountNumber(
            "BF-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase()
    );
    account.setBalance(new BigDecimal("100.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.ACTIVE);
    account.setUser(savedUser);

    Account savedAccount =
            accountRepository.save(account);

    mockMvc.perform(
                    post(
                            "/api/accounts/"
                                    + savedAccount.getId()
                                    + "/withdraw"
                    )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "amount": 150.00
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
            );

    Account unchangedAccount =
            accountRepository
                    .findById(savedAccount.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("100.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
@Test
void shouldReturnDepositAndWithdrawalHistory() throws Exception {

    User user = new User();
    user.setFirstName("History");
    user.setLastName("Integration");
    user.setEmail(
            "history-"
                    + UUID.randomUUID()
                    + "@example.com"
    );

    User savedUser = userRepository.save(user);

    Account account = new Account();

    account.setAccountNumber(
            "BF-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase()
    );

    account.setBalance(new BigDecimal("100.00"));
    account.setCurrency(AccountCurrency.MAD);
    account.setStatus(AccountStatus.ACTIVE);
    account.setUser(savedUser);

    Account savedAccount =
            accountRepository.save(account);


    // Dépôt : 100 -> 200
    mockMvc.perform(
            post(
                    "/api/accounts/"
                            + savedAccount.getId()
                            + "/deposit"
            )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "amount": 100.00
                            }
                            """)
    )
    .andExpect(status().isOk());


    // Retrait : 200 -> 150
    mockMvc.perform(
            post(
                    "/api/accounts/"
                            + savedAccount.getId()
                            + "/withdraw"
            )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "amount": 50.00
                            }
                            """)
    )
    .andExpect(status().isOk());


    // Historique
    mockMvc.perform(
                    get(
                            "/api/accounts/"
                                    + savedAccount.getId()
                                    + "/transactions"
                    )
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
                            .value(50.00)
            )
            .andExpect(
                    jsonPath("$[0].balanceAfter")
                            .value(150.00)
            )
            .andExpect(
                    jsonPath("$[1].type")
                            .value("DEPOSIT")
            )
            .andExpect(
                    jsonPath("$[1].amount")
                            .value(100.00)
            )
            .andExpect(
                    jsonPath("$[1].balanceAfter")
                            .value(200.00)
            );
}
@Test
void shouldReturn404WhenGettingHistoryForUnknownAccount()
        throws Exception {

    mockMvc.perform(
                    get(
                            "/api/accounts/"
                                    + Long.MAX_VALUE
                                    + "/transactions"
                    )
            )
            .andExpect(status().isNotFound())
            .andExpect(
                    jsonPath("$.title")
                            .value("Account not found")
            )
            .andExpect(
                    jsonPath("$.status")
                            .value(404)
            );
}
}   