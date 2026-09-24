
package com.bankflow.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class UserFormPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private static final String BASE_URL =
            "http://localhost:5173";

    // Localisateurs des éléments React

    private final By pageTitle =
            By.tagName("h1");

    private final By firstNameInput =
            By.id("firstName");

    private final By lastNameInput =
            By.id("lastName");

    private final By emailInput =
            By.id("email");

    private final By createUserButton =
            By.id("createUserButton");

    private final By message =
            By.id("message");

    private final By firstNameError =
            By.id("firstName-error");

    private final By emailError =
            By.id("email-error");


    // Constructeur

    public UserFormPage(WebDriver driver) {

        this.driver = driver;

        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );
    }


    // Ouvrir BankFlow

    public void open() {

        driver.get(BASE_URL);

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        firstNameInput
                )
        );
    }


    // Récupérer le titre

    public String getPageTitle() {

        return driver.findElement(pageTitle)
                .getText();
    }


    // Vérifier la visibilité des champs

    public boolean isFirstNameVisible() {

        return driver.findElement(firstNameInput)
                .isDisplayed();
    }

    public boolean isLastNameVisible() {

        return driver.findElement(lastNameInput)
                .isDisplayed();
    }

    public boolean isEmailVisible() {

        return driver.findElement(emailInput)
                .isDisplayed();
    }


    // Vérifier le bouton

    public boolean isCreateButtonVisible() {

        return driver.findElement(createUserButton)
                .isDisplayed();
    }

    public boolean isCreateButtonEnabled() {

        return driver.findElement(createUserButton)
                .isEnabled();
    }


    // Saisir le prénom

    public void enterFirstName(String firstName) {

        WebElement input = driver.findElement(
                firstNameInput
        );

        input.clear();

        input.sendKeys(firstName);
    }


    // Saisir le nom

    public void enterLastName(String lastName) {

        WebElement input = driver.findElement(
                lastNameInput
        );

        input.clear();

        input.sendKeys(lastName);
    }


    // Saisir l'email

    public void enterEmail(String email) {

        WebElement input = driver.findElement(
                emailInput
        );

        input.clear();

        input.sendKeys(email);
    }


    // Remplir le formulaire complet

    public void fillForm(
            String firstName,
            String lastName,
            String email) {

        enterFirstName(firstName);

        enterLastName(lastName);

        enterEmail(email);
    }


    // Soumettre le formulaire

    public void submit() {

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        createUserButton
                )
        ).click();
    }


    // Attendre et récupérer le message de confirmation

    public String getSuccessMessage() {

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        message,
                        "créé avec succès"
                )
        );

        return driver.findElement(message)
                .getText();
    }


    // Attendre et récupérer le message d'erreur du prénom

    public String getFirstNameError() {

        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        firstNameError
                )
        ).getText();
    }


    // Attendre et récupérer le message d'erreur de l'email

    public String getEmailError() {

        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        emailError
                )
        ).getText();
    }


    // Attendre un message de doublon

    public String getDuplicateEmailMessage(String email) {

        String expectedMessage =
                "A user with email "
                        + email
                        + " already exists";

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        message,
                        expectedMessage
                )
        );

        return driver.findElement(message)
                .getText();
    }

    
    // Récupérer la valeur actuelle du prénom
    public String getFirstNameValue() {

        return driver.findElement(firstNameInput)
                .getDomProperty("value");
    }


    // Récupérer la valeur actuelle de l'email
    public String getEmailValue() {

        return driver.findElement(emailInput)
                .getDomProperty("value");
    }
}