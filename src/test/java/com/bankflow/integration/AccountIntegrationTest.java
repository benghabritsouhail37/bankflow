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
}   