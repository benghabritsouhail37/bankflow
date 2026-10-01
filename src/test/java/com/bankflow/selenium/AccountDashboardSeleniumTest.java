package com.bankflow.selenium;

import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;
import com.bankflow.entity.Transaction;
import com.bankflow.entity.User;

import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.TransactionRepository;
import com.bankflow.repository.UserRepository;

import com.bankflow.selenium.pages.AccountDashboardPage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("local")
class AccountDashboardSeleniumTest {

    private WebDriver driver;

    private AccountDashboardPage dashboardPage;

    private Long createdUserId;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;


    @BeforeEach
    void setUp() {

        driver = new ChromeDriver();

        driver.manage()
                .window()
                .maximize();

        dashboardPage =
                new AccountDashboardPage(driver);
    }


    @AfterEach
    void tearDown() {

        try {

            if (driver != null) {
                driver.quit();
            }

        } finally {

            if (createdUserId != null) {

                List<Account> accounts =
                        accountRepository
                                .findByUserId(
                                        createdUserId
                                );

                for (Account account : accounts) {

                    List<Transaction> transactions =
                            transactionRepository
                                    .findByAccountIdOrderByCreatedAtDesc(
                                            account.getId()
                                    );

                    transactionRepository
                            .deleteAll(transactions);
                }

                accountRepository
                        .deleteAll(accounts);

                userRepository
                        .findById(createdUserId)
                        .ifPresent(
                                userRepository::delete
                        );
            }
        }
    }
    


    private User createUser(String prefix) {

        User user = new User();

        user.setFirstName(prefix);
        user.setLastName("Selenium");

        user.setEmail(
                prefix.toLowerCase()
                        + "-"
                        + UUID.randomUUID()
                        + "@example.com"
        );

        User savedUser =
                userRepository.saveAndFlush(user);

        createdUserId =
                savedUser.getId();

        return savedUser;
    }


    private Account createAccount(
            User user,
            BigDecimal balance) {

        Account account = new Account();

        account.setAccountNumber(
                "BF-"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase()
        );

        account.setBalance(balance);

        account.setCurrency(
                AccountCurrency.MAD
        );

        account.setStatus(
                AccountStatus.ACTIVE
        );

        account.setUser(user);

        return accountRepository
                .saveAndFlush(account);
    }


    // =====================================================
    // SEL-007 : création d'un compte
    // =====================================================

    @Test
    void shouldCreateBankAccountFromReactInterface() {

        User user =
                createUser("Account");

        dashboardPage.open();

        dashboardPage.loadUser(
                user.getId()
        );

        dashboardPage.createAccount(
                "MAD"
        );


        List<Account> accounts =
                accountRepository
                        .findByUserId(
                                user.getId()
                        );


        assertEquals(
                1,
                accounts.size()
        );

        Account account =
                accounts.getFirst();


        assertEquals(
                AccountCurrency.MAD,
                account.getCurrency()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                account.getStatus()
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        account.getBalance()
                )
        );


        String card =
                dashboardPage
                        .getAccountCardText(
                                account.getId()
                        );

        assertTrue(
                card.contains("MAD")
        );

        assertTrue(
                card.contains("0.00 MAD")
        );

        assertTrue(
                card.contains("ACTIVE")
        );
    }


    // =====================================================
    // SEL-008 : dépôt
    // =====================================================

    @Test
    void shouldDepositMoneyFromReactInterface() {

        User user =
                createUser("Deposit");

        Account account =
                createAccount(
                        user,
                        BigDecimal.ZERO
                );


        dashboardPage.open();

        dashboardPage.loadUser(
                user.getId()
        );


        dashboardPage.deposit(
                account.getId(),
                "500"
        );


        dashboardPage.waitForBalance(
                account.getId(),
                "500.00 MAD"
        );


        Account updatedAccount =
                accountRepository
                        .findById(account.getId())
                        .orElseThrow();


        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(
                                updatedAccount
                                        .getBalance()
                        )
        );


        assertTrue(
                dashboardPage
                        .getMessage()
                        .contains(
                                "Nouveau solde : 500.00 MAD"
                        )
        );
    }


    // =====================================================
    // SEL-009 : retrait
    // =====================================================

    @Test
    void shouldWithdrawMoneyFromReactInterface() {

        User user =
                createUser("Withdraw");

        Account account =
                createAccount(
                        user,
                        new BigDecimal("500.00")
                );


        dashboardPage.open();

        dashboardPage.loadUser(
                user.getId()
        );


        dashboardPage.withdraw(
                account.getId(),
                "100"
        );

        dashboardPage
                .waitForSuccessfulWithdrawal();

        dashboardPage.waitForBalance(
                account.getId(),
                "400.00 MAD"
        );


        Account updatedAccount =
                accountRepository
                        .findById(account.getId())
                        .orElseThrow();


        assertEquals(
                0,
                new BigDecimal("400.00")
                        .compareTo(
                                updatedAccount
                                        .getBalance()
                        )
        );
    }


    // =====================================================
    // SEL-010 : retrait supérieur au solde
    // =====================================================

    @Test
    void shouldRejectWithdrawalWhenBalanceIsInsufficient() {

        User user =
                createUser("Insufficient");

        Account account =
                createAccount(
                        user,
                        new BigDecimal("400.00")
                );


        dashboardPage.open();

        dashboardPage.loadUser(
                user.getId()
        );


        dashboardPage.withdraw(
                account.getId(),
                "1000"
        );


        String message =
        dashboardPage.waitForMessageContaining(
                "Insufficient funds"
        );

assertTrue(
        message.contains(
                "Insufficient funds"
        )
);


        Account unchangedAccount =
                accountRepository
                        .findById(account.getId())
                        .orElseThrow();


        assertEquals(
                0,
                new BigDecimal("400.00")
                        .compareTo(
                                unchangedAccount
                                        .getBalance()
                        )
        );


        dashboardPage.waitForBalance(
                account.getId(),
                "400.00 MAD"
        );
    }


    // =====================================================
    // SEL-011 : historique
    // =====================================================

    @Test
    void shouldDisplayTransactionHistory() {

        User user =
                createUser("History");

        Account account =
                createAccount(
                        user,
                        BigDecimal.ZERO
                );


        dashboardPage.open();

        dashboardPage.loadUser(
                user.getId()
        );


        // Dépôt 500
        dashboardPage.deposit(
                account.getId(),
                "500"
        );

        dashboardPage.waitForBalance(
                account.getId(),
                "500.00 MAD"
        );


        // Retrait 100
        dashboardPage.withdraw(
                account.getId(),
                "100"
        );

        dashboardPage
                .waitForSuccessfulWithdrawal();

        dashboardPage.waitForBalance(
                account.getId(),
                "400.00 MAD"
        );


        // Ouvrir historique
        dashboardPage.openHistory(
                account.getId()
        );


        assertEquals(
                2,
                dashboardPage
                        .getTransactionCount()
        );


        String firstTransaction =
                dashboardPage
                        .getTransactionText(0);

        String secondTransaction =
                dashboardPage
                        .getTransactionText(1);


        // La plus récente : retrait
        assertTrue(
                firstTransaction.contains(
                        "WITHDRAWAL"
                )
        );

        assertTrue(
                firstTransaction.contains(
                        "100.00"
                )
        );

        assertTrue(
                firstTransaction.contains(
                        "400.00"
                )
        );


        // Puis le dépôt
        assertTrue(
                secondTransaction.contains(
                        "DEPOSIT"
                )
        );

        assertTrue(
                secondTransaction.contains(
                        "500.00"
                )
        );


        List<Transaction> transactions =
                transactionRepository
                        .findByAccountIdOrderByCreatedAtDesc(
                                account.getId()
                        );

        assertEquals(
                2,
                transactions.size()
        );
    }
}