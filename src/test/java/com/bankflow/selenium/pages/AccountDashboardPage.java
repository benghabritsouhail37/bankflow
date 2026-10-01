package com.bankflow.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class AccountDashboardPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private static final String BASE_URL =
            "http://localhost:5173";

    private final By existingUserId =
            By.id("existingUserId");

    private final By loadUserButton =
            By.id("loadUserButton");

    private final By activeUser =
            By.id("activeUser");

    private final By accountCurrency =
            By.id("accountCurrency");

    private final By createAccountButton =
            By.id("createAccountButton");

    private final By accountCards =
            By.cssSelector(
                    "[data-testid='account-card']"
            );

    private final By message =
            By.id("message");

    private final By transactionHistory =
            By.id("transaction-history");

    private final By transactionRows =
            By.cssSelector(
                    "[data-testid='transaction-row']"
            );


    public AccountDashboardPage(WebDriver driver) {

        this.driver = driver;

        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );
    }


    public void open() {

        driver.get(BASE_URL);

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        existingUserId
                )
        );
    }


    public void loadUser(Long userId) {

        WebElement input =
                driver.findElement(existingUserId);

        input.clear();
        input.sendKeys(String.valueOf(userId));

        driver.findElement(loadUserButton)
                .click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        activeUser
                )
        );

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        activeUser,
                        "ID : " + userId
                )
        );
    }


    public void createAccount(String currency) {

        Select select =
                new Select(
                        driver.findElement(accountCurrency)
                );

        select.selectByValue(currency);

        driver.findElement(createAccountButton)
                .click();

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        message,
                        "Compte " + currency
                                + " créé avec succès."
                )
        );

        wait.until(driver ->
                !driver.findElements(accountCards)
                        .isEmpty()
        );
    }


    private WebElement getAccountCard(Long accountId) {

        By inputLocator =
                By.id("amount-" + accountId);

        WebElement input =
                wait.until(
                        ExpectedConditions
                                .visibilityOfElementLocated(
                                        inputLocator
                                )
                );

        return input.findElement(
                By.xpath("./ancestor::article")
        );
    }


    public String getAccountCardText(Long accountId) {

        return getAccountCard(accountId)
                .getText();
    }


    public void enterAmount(
            Long accountId,
            String amount) {

        WebElement input =
                wait.until(
                        ExpectedConditions
                                .visibilityOfElementLocated(
                                        By.id(
                                                "amount-"
                                                        + accountId
                                        )
                                )
                );

        input.clear();
        input.sendKeys(amount);
    }


    public void deposit(
            Long accountId,
            String amount) {

        enterAmount(accountId, amount);

        WebElement accountCard =
                getAccountCard(accountId);

        accountCard.findElement(
                By.cssSelector(
                        "[data-testid='deposit-button']"
                )
        ).click();

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        message,
                        "Dépôt effectué."
                )
        );
    }


    public void withdraw(
            Long accountId,
            String amount) {

        enterAmount(accountId, amount);

        WebElement accountCard =
                getAccountCard(accountId);

        accountCard.findElement(
                By.cssSelector(
                        "[data-testid='withdraw-button']"
                )
        ).click();
    }


    public void waitForSuccessfulWithdrawal() {

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        message,
                        "Retrait effectué."
                )
        );
    }


    public String getMessage() {

        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        message
                )
        ).getText();
    }


    public void waitForBalance(
            Long accountId,
            String expectedBalance) {

        wait.until(driver ->
                getAccountCardText(accountId)
                        .contains(expectedBalance)
        );
    }


    public void openHistory(Long accountId) {

        WebElement accountCard =
                getAccountCard(accountId);

        accountCard.findElement(
                By.cssSelector(
                        "[data-testid='history-button']"
                )
        ).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        transactionHistory
                )
        );
    }


    public int getTransactionCount() {

        return wait.until(driver -> {

            List<WebElement> rows =
                    driver.findElements(transactionRows);

            return rows.isEmpty()
                    ? null
                    : rows.size();
        });
    }


    public String getTransactionText(int index) {

        return driver.findElements(transactionRows)
                .get(index)
                .getText();
    }
    public String waitForMessageContaining(String expectedText) {

    wait.until(
            ExpectedConditions.textToBePresentInElementLocated(
                    message,
                    expectedText
            )
    );

    return driver.findElement(message)
            .getText();
}
}