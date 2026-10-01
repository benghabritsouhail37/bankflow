package com.bankflow.api;

import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;
import com.bankflow.entity.User;
import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.UserRepository;
import com.bankflow.repository.TransactionRepository;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("local")
class AccountApiRestAssuredTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Long createdUserId;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
    @AfterEach
void cleanUp() {

    if (createdUserId != null) {

        List<Account> accounts =
                accountRepository.findByUserId(createdUserId);

        // 1. Supprimer les transactions
        for (Account account : accounts) {

            transactionRepository.deleteAll(
                    transactionRepository
                            .findByAccountIdOrderByCreatedAtDesc(
                                    account.getId()
                            )
            );
        }

        // 2. Supprimer les comptes
        accountRepository.deleteAll(accounts);

        // 3. Supprimer l'utilisateur
        userRepository.findById(createdUserId)
                .ifPresent(userRepository::delete);
    }
}

    private User createTestUser(String prefix) {

        User user = new User();

        user.setFirstName(prefix);
        user.setLastName("REST Test");

        user.setEmail(
                prefix.toLowerCase()
                        + "-"
                        + UUID.randomUUID()
                        + "@example.com"
        );

        User savedUser = userRepository.save(user);

        createdUserId = savedUser.getId();

        return savedUser;
    }

    private Account createTestAccount(
            User user,
            BigDecimal balance,
            AccountStatus status) {

        Account account = new Account();

        account.setAccountNumber(
                "BF-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase()
        );

        account.setBalance(balance);
        account.setCurrency(AccountCurrency.MAD);
        account.setStatus(status);
        account.setUser(user);

        return accountRepository.save(account);
    }

    // tes tests ici...



    // =========================================================
    // TEST 1
    // Création compte MAD
    // =========================================================

    @Test
    void shouldCreateMadAccountThroughRealHttpRequest() {

        User savedUser = createTestUser("MAD");

        Map<String, Object> requestBody = Map.of(
                "userId", savedUser.getId(),
                "currency", "MAD"
        );

        String accountNumber =

                given()
                        .contentType(ContentType.JSON)
                        .body(requestBody)

                .when()
                        .post("/api/accounts")

                .then()
                        .statusCode(201)

                        .body(
                                "id",
                                notNullValue()
                        )

                        .body(
                                "accountNumber",
                                matchesPattern(
                                        "BF-[A-F0-9]{32}"
                                )
                        )

                        .body(
                                "balance",
                                equalTo(0.0f)
                        )

                        .body(
                                "currency",
                                equalTo("MAD")
                        )

                        .body(
                                "status",
                                equalTo("ACTIVE")
                        )

                        .body(
                                "userId",
                                equalTo(
                                        savedUser
                                                .getId()
                                                .intValue()
                                )
                        )

                        .body(
                                "createdAt",
                                notNullValue()
                        )

                        .extract()
                        .path("accountNumber");


        Account savedAccount =
                accountRepository
                        .findByAccountNumber(accountNumber)
                        .orElseThrow();


        assertEquals(
                AccountCurrency.MAD,
                savedAccount.getCurrency()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                savedAccount.getStatus()
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        savedAccount.getBalance()
                )
        );

        assertEquals(
                savedUser.getId(),
                savedAccount.getUser().getId()
        );

        assertTrue(
                savedAccount
                        .getAccountNumber()
                        .matches("BF-[A-F0-9]{32}")
        );
    }


    // =========================================================
    // TEST 2
    // Création compte EUR
    // =========================================================

    @Test
    void shouldCreateEurAccount() {

        User savedUser = createTestUser("EUR");

        Map<String, Object> requestBody = Map.of(
                "userId", savedUser.getId(),
                "currency", "EUR"
        );


        String accountNumber =

                given()
                        .contentType(ContentType.JSON)
                        .body(requestBody)

                .when()
                        .post("/api/accounts")

                .then()
                        .statusCode(201)

                        .body(
                                "accountNumber",
                                matchesPattern(
                                        "BF-[A-F0-9]{32}"
                                )
                        )

                        .body(
                                "balance",
                                equalTo(0.0f)
                        )

                        .body(
                                "currency",
                                equalTo("EUR")
                        )

                        .body(
                                "status",
                                equalTo("ACTIVE")
                        )

                        .body(
                                "userId",
                                equalTo(
                                        savedUser
                                                .getId()
                                                .intValue()
                                )
                        )

                        .extract()
                        .path("accountNumber");


        Account savedAccount =
                accountRepository
                        .findByAccountNumber(accountNumber)
                        .orElseThrow();


        assertEquals(
                AccountCurrency.EUR,
                savedAccount.getCurrency()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                savedAccount.getStatus()
        );
    }


    // =========================================================
    // TEST 3
    // Un utilisateur peut avoir plusieurs comptes
    // =========================================================

    @Test
    void shouldAllowUserToHaveMultipleAccounts() {

        User savedUser = createTestUser("MULTI");


        Map<String, Object> madRequest = Map.of(
                "userId", savedUser.getId(),
                "currency", "MAD"
        );


        String firstAccountNumber =

                given()
                        .contentType(ContentType.JSON)
                        .body(madRequest)

                .when()
                        .post("/api/accounts")

                .then()
                        .statusCode(201)

                        .body(
                                "currency",
                                equalTo("MAD")
                        )

                        .extract()
                        .path("accountNumber");


        Map<String, Object> eurRequest = Map.of(
                "userId", savedUser.getId(),
                "currency", "EUR"
        );


        String secondAccountNumber =

                given()
                        .contentType(ContentType.JSON)
                        .body(eurRequest)

                .when()
                        .post("/api/accounts")

                .then()
                        .statusCode(201)

                        .body(
                                "currency",
                                equalTo("EUR")
                        )

                        .extract()
                        .path("accountNumber");


        assertNotEquals(
                firstAccountNumber,
                secondAccountNumber
        );


        List<Account> accounts =
                accountRepository.findByUserId(
                        savedUser.getId()
                );


        assertEquals(
                2,
                accounts.size()
        );
    }


    // =========================================================
    // TEST 4
    // Utilisateur inexistant
    // =========================================================

    @Test
    void shouldReturn404WhenUserDoesNotExist() {

        long accountsBefore =
                accountRepository.count();


        Map<String, Object> requestBody = Map.of(
                "userId", 999999999L,
                "currency", "MAD"
        );


        given()
                .contentType(ContentType.JSON)
                .body(requestBody)

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(404)

                .body(
                        "title",
                        equalTo("User not found")
                )

                .body(
                        "status",
                        equalTo(404)
                )

                .body(
                        "detail",
                        equalTo(
                                "User not found with id: 999999999"
                        )
                );


        long accountsAfter =
                accountRepository.count();


        assertEquals(
                accountsBefore,
                accountsAfter
        );
    }


    // =========================================================
    // TEST 5
    // Devise manquante
    // =========================================================

    @Test
    void shouldReturn400WhenCurrencyIsMissing() {

        User savedUser =
                createTestUser("MISSING-CURRENCY");

        long accountsBefore =
                accountRepository.count();


        Map<String, Object> requestBody = Map.of(
                "userId", savedUser.getId()
        );


        given()
                .contentType(ContentType.JSON)
                .body(requestBody)

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(400)

                .body(
                        "title",
                        equalTo("Validation failed")
                )

                .body(
                        "status",
                        equalTo(400)
                )

                .body(
                        "errors.currency",
                        equalTo(
                                "Account currency is required"
                        )
                );


        assertEquals(
                accountsBefore,
                accountRepository.count()
        );
    }


    // =========================================================
    // TEST 6
    // User ID manquant
    // =========================================================

    @Test
    void shouldReturn400WhenUserIdIsMissing() {

        long accountsBefore =
                accountRepository.count();


        Map<String, Object> requestBody = Map.of(
                "currency", "MAD"
        );


        given()
                .contentType(ContentType.JSON)
                .body(requestBody)

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(400)

                .body(
                        "title",
                        equalTo("Validation failed")
                )

                .body(
                        "status",
                        equalTo(400)
                )

                .body(
                        "errors.userId",
                        equalTo(
                                "User ID is required"
                        )
                );


        assertEquals(
                accountsBefore,
                accountRepository.count()
        );
    }


    // =========================================================
    // TEST 7
    // User ID et devise manquants
    // =========================================================

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() {

        long accountsBefore =
                accountRepository.count();


        given()
                .contentType(ContentType.JSON)
                .body("{}")

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(400)

                .body(
                        "title",
                        equalTo("Validation failed")
                )

                .body(
                        "status",
                        equalTo(400)
                )

                .body(
                        "errors.userId",
                        equalTo(
                                "User ID is required"
                        )
                )

                .body(
                        "errors.currency",
                        equalTo(
                                "Account currency is required"
                        )
                );


        assertEquals(
                accountsBefore,
                accountRepository.count()
        );
    }


    // =========================================================
    // TEST 8
    // Devise USD non autorisée
    // =========================================================

    @Test
    void shouldReturn400WhenCurrencyIsInvalid() {

        User savedUser =
                createTestUser("INVALID-CURRENCY");

        long accountsBefore =
                accountRepository.count();


        Map<String, Object> requestBody = Map.of(
                "userId", savedUser.getId(),
                "currency", "USD"
        );


        given()
                .contentType(ContentType.JSON)
                .body(requestBody)

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(400)

                .body(
                        "title",
                        equalTo("Invalid request")
                )

                .body(
                        "status",
                        equalTo(400)
                )

                .body(
                        "detail",
                        equalTo(
                                "The request body is missing or contains invalid values"
                        )
                );


        assertEquals(
                accountsBefore,
                accountRepository.count()
        );
    }


    // =========================================================
    // TEST 9
    // Enum sensible à la casse
    // =========================================================

    @Test
    void shouldReturn400WhenCurrencyIsLowercase() {

        User savedUser =
                createTestUser("LOWERCASE");

        long accountsBefore =
                accountRepository.count();


        given()
                .contentType(ContentType.JSON)

                .body("""
                        {
                          "userId": %d,
                          "currency": "mad"
                        }
                        """.formatted(savedUser.getId()))

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(400)

                .body(
                        "title",
                        equalTo("Invalid request")
                );


        assertEquals(
                accountsBefore,
                accountRepository.count()
        );
    }


    // =========================================================
    // TEST 10
    // JSON mal formé
    // =========================================================

    @Test
    void shouldReturn400WhenJsonIsMalformed() {

        long accountsBefore =
                accountRepository.count();


        String malformedJson = """
                {
                  "userId": 1,
                  "currency": "MAD"
                """;


        given()
                .contentType(ContentType.JSON)
                .body(malformedJson)

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(400)

                .body(
                        "title",
                        equalTo("Invalid request")
                );


        assertEquals(
                accountsBefore,
                accountRepository.count()
        );
    }


    // =========================================================
    // TEST 11
    // Body vide
    // =========================================================

    @Test
    void shouldReturn400WhenRequestBodyIsEmpty() {

        long accountsBefore =
                accountRepository.count();


        given()
                .contentType(ContentType.JSON)
                .body("")

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(400)

                .body(
                        "title",
                        equalTo("Invalid request")
                );


        assertEquals(
                accountsBefore,
                accountRepository.count()
        );
    }


    // =========================================================
    // TEST 12
    // Mauvais Content-Type
    // =========================================================

    @Test
    void shouldReturn415WhenContentTypeIsUnsupported() {

        given()
                .contentType(ContentType.TEXT)

                .body("""
                        {
                          "userId": 1,
                          "currency": "MAD"
                        }
                        """)

        .when()
                .post("/api/accounts")

        .then()
                .statusCode(415);
    }


    // =========================================================
    // TEST 13
    // Méthode HTTP PUT non supportée
    // =========================================================

    @Test
    void shouldReturn405WhenHttpMethodIsNotSupported() {

        given()
                .contentType(ContentType.JSON)

                .body("""
                        {
                          "userId": 1,
                          "currency": "MAD"
                        }
                        """)

        .when()
                .put("/api/accounts")

        .then()
                .statusCode(405);
    }


    // =========================================================
    // TEST 14
    // Route inexistante
    // =========================================================

    @Test
    void shouldReturn404WhenRouteDoesNotExist() {

        given()
                .contentType(ContentType.JSON)

        .when()
                .get("/api/accounts/not-existing-route")

        .then()
                .statusCode(404);
    }
    // =========================================================
// DEPOSIT REST TEST 1
// Dépôt réussi
// =========================================================

@Test
void shouldDepositMoneyThroughRealHttpRequest() {

    User user = createTestUser("DEPOSIT");

    Account account = createTestAccount(
            user,
            new BigDecimal("100.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 50.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/deposit"
            )

    .then()
            .statusCode(200)
            .body("balance", equalTo(150.0f))
            .body("currency", equalTo("MAD"))
            .body("status", equalTo("ACTIVE"))
            .body(
                    "userId",
                    equalTo(user.getId().intValue())
            );


    Account updatedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("150.00")
                    .compareTo(updatedAccount.getBalance())
    );
}


// =========================================================
// DEPOSIT REST TEST 2
// Compte inexistant
// =========================================================

@Test
void shouldReturn404WhenDepositingIntoNonExistingAccount() {

    long accountsBefore =
            accountRepository.count();

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 50.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + Long.MAX_VALUE
                            + "/deposit"
            )

    .then()
            .statusCode(404)
            .body(
                    "title",
                    equalTo("Account not found")
            )
            .body(
                    "status",
                    equalTo(404)
            )
            .body(
                    "detail",
                    equalTo(
                            "Account not found with id: "
                                    + Long.MAX_VALUE
                    )
            );

    assertEquals(
            accountsBefore,
            accountRepository.count()
    );
}


// =========================================================
// DEPOSIT REST TEST 3
// Montant zéro
// =========================================================

@Test
void shouldReturn400WhenDepositAmountIsZero() {

    User user = createTestUser("ZERO-DEPOSIT");

    Account account = createTestAccount(
            user,
            new BigDecimal("100.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 0
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/deposit"
            )

    .then()
            .statusCode(400)
            .body(
                    "title",
                    equalTo("Validation failed")
            )
            .body(
                    "status",
                    equalTo(400)
            )
            .body(
                    "errors.amount",
                    equalTo(
                            "Amount must be greater than 0"
                    )
            );


    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("100.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}


// =========================================================
// DEPOSIT REST TEST 4
// Montant négatif
// =========================================================

@Test
void shouldReturn400WhenDepositAmountIsNegative() {

    User user = createTestUser("NEGATIVE-DEPOSIT");

    Account account = createTestAccount(
            user,
            new BigDecimal("100.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": -50.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/deposit"
            )

    .then()
            .statusCode(400)
            .body(
                    "title",
                    equalTo("Validation failed")
            )
            .body(
                    "errors.amount",
                    equalTo(
                            "Amount must be greater than 0"
                    )
            );


    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("100.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}


// =========================================================
// DEPOSIT REST TEST 5
// Montant absent
// =========================================================

@Test
void shouldReturn400WhenDepositAmountIsMissing() {

    User user = createTestUser("MISSING-AMOUNT");

    Account account = createTestAccount(
            user,
            new BigDecimal("100.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("{}")

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/deposit"
            )

    .then()
            .statusCode(400)
            .body(
                    "title",
                    equalTo("Validation failed")
            )
            .body(
                    "status",
                    equalTo(400)
            )
            .body(
                    "errors.amount",
                    equalTo("Amount is required")
            );


    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("100.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}


// =========================================================
// DEPOSIT REST TEST 6
// Compte bloqué
// =========================================================

@Test
void shouldReturn409WhenDepositingIntoBlockedAccount() {

    User user = createTestUser("BLOCKED-DEPOSIT");

    Account account = createTestAccount(
            user,
            new BigDecimal("100.00"),
            AccountStatus.BLOCKED
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 50.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/deposit"
            )

    .then()
            .statusCode(409)
            .body(
                    "title",
                    equalTo("Account blocked")
            )
            .body(
                    "status",
                    equalTo(409)
            )
            .body(
                    "detail",
                    equalTo(
                            "Account is blocked with id: "
                                    + account.getId()
                    )
            );


    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("100.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
@Test
void shouldWithdrawMoneyThroughRealHttpRequest() {

    User user = createTestUser("WITHDRAW");

    Account account = createTestAccount(
            user,
            new BigDecimal("500.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 200.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/withdraw"
            )

    .then()
            .statusCode(200)
            .body("balance", equalTo(300.0f))
            .body("currency", equalTo("MAD"))
            .body("status", equalTo("ACTIVE"))
            .body(
                    "userId",
                    equalTo(user.getId().intValue())
            );

    Account updatedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("300.00")
                    .compareTo(updatedAccount.getBalance())
    );
}
@Test
void shouldReturn404WhenWithdrawingFromNonExistingAccount() {

    long accountsBefore = accountRepository.count();

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 50.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + Long.MAX_VALUE
                            + "/withdraw"
            )

    .then()
            .statusCode(404)
            .body(
                    "title",
                    equalTo("Account not found")
            )
            .body(
                    "status",
                    equalTo(404)
            )
            .body(
                    "detail",
                    equalTo(
                            "Account not found with id: "
                                    + Long.MAX_VALUE
                    )
            );

    assertEquals(
            accountsBefore,
            accountRepository.count()
    );
}
@Test
void shouldReturn400WhenWithdrawalAmountIsZero() {

    User user = createTestUser("ZERO-WITHDRAW");

    Account account = createTestAccount(
            user,
            new BigDecimal("500.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 0
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/withdraw"
            )

    .then()
            .statusCode(400)
            .body(
                    "title",
                    equalTo("Validation failed")
            )
            .body(
                    "errors.amount",
                    equalTo("Amount must be greater than 0")
            );

    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("500.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
@Test
void shouldReturn400WhenWithdrawalAmountIsNegative() {

    User user = createTestUser("NEGATIVE-WITHDRAW");

    Account account = createTestAccount(
            user,
            new BigDecimal("500.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": -50.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/withdraw"
            )

    .then()
            .statusCode(400)
            .body(
                    "title",
                    equalTo("Validation failed")
            )
            .body(
                    "errors.amount",
                    equalTo("Amount must be greater than 0")
            );

    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("500.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
@Test
void shouldReturn400WhenWithdrawalAmountIsMissing() {

    User user = createTestUser("MISSING-WITHDRAW");

    Account account = createTestAccount(
            user,
            new BigDecimal("500.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("{}")

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/withdraw"
            )

    .then()
            .statusCode(400)
            .body(
                    "title",
                    equalTo("Validation failed")
            )
            .body(
                    "errors.amount",
                    equalTo("Amount is required")
            );

    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("500.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
@Test
void shouldReturn409WhenWithdrawingFromBlockedAccount() {

    User user = createTestUser("BLOCKED-WITHDRAW");

    Account account = createTestAccount(
            user,
            new BigDecimal("500.00"),
            AccountStatus.BLOCKED
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 50.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/withdraw"
            )

    .then()
            .statusCode(409)
            .body(
                    "title",
                    equalTo("Account blocked")
            )
            .body(
                    "status",
                    equalTo(409)
            );

    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("500.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
@Test
void shouldReturn409WhenWithdrawalExceedsBalance() {

    User user = createTestUser("INSUFFICIENT-WITHDRAW");

    Account account = createTestAccount(
            user,
            new BigDecimal("100.00"),
            AccountStatus.ACTIVE
    );

    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 150.00
                    }
                    """)

    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/withdraw"
            )

    .then()
            .statusCode(409)
            .body(
                    "title",
                    equalTo("Insufficient funds")
            )
            .body(
                    "status",
                    equalTo(409)
            )
            .body(
                    "detail",
                    equalTo(
                            "Insufficient funds for account with id: "
                                    + account.getId()
                    )
            );

    Account unchangedAccount =
            accountRepository
                    .findById(account.getId())
                    .orElseThrow();

    assertEquals(
            0,
            new BigDecimal("100.00")
                    .compareTo(unchangedAccount.getBalance())
    );
}
@Test
void shouldReturnTransactionHistoryThroughRealHttp() {

    User user = createTestUser("HISTORY");

    Account account = createTestAccount(
            user,
            new BigDecimal("100.00"),
            AccountStatus.ACTIVE
    );

    // Dépôt
    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 100.00
                    }
                    """)
    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/deposit"
            )
    .then()
            .statusCode(200);


    // Retrait
    given()
            .contentType(ContentType.JSON)
            .body("""
                    {
                      "amount": 50.00
                    }
                    """)
    .when()
            .post(
                    "/api/accounts/"
                            + account.getId()
                            + "/withdraw"
            )
    .then()
            .statusCode(200);


    // Historique
    given()

    .when()
            .get(
                    "/api/accounts/"
                            + account.getId()
                            + "/transactions"
            )

    .then()
            .statusCode(200)

            .body(
                    "size()",
                    equalTo(2)
            )

            .body(
                    "[0].type",
                    equalTo("WITHDRAWAL")
            )

            .body(
                    "[0].amount",
                    equalTo(50.0f)
            )

            .body(
                    "[0].balanceAfter",
                    equalTo(150.0f)
            )

            .body(
                    "[1].type",
                    equalTo("DEPOSIT")
            )

            .body(
                    "[1].amount",
                    equalTo(100.0f)
            )

            .body(
                    "[1].balanceAfter",
                    equalTo(200.0f)
            );
}
@Test
void shouldReturnEmptyHistoryForAccountWithoutTransactions() {

    User user = createTestUser("EMPTY-HISTORY");

    Account account = createTestAccount(
            user,
            BigDecimal.ZERO,
            AccountStatus.ACTIVE
    );

    given()

    .when()
            .get(
                    "/api/accounts/"
                            + account.getId()
                            + "/transactions"
            )

    .then()
            .statusCode(200)
            .body(
                    "size()",
                    equalTo(0)
            );
}
@Test
void shouldReturn404ForTransactionHistoryOfUnknownAccount() {

    given()

    .when()
            .get(
                    "/api/accounts/"
                            + Long.MAX_VALUE
                            + "/transactions"
            )

    .then()
            .statusCode(404)
            .body(
                    "title",
                    equalTo("Account not found")
            )
            .body(
                    "status",
                    equalTo(404)
            );
}
}