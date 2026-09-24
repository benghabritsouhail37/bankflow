
package com.bankflow.selenium;

import com.bankflow.selenium.pages.UserFormPage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserFormSeleniumTest {

    private WebDriver driver;

    private UserFormPage userFormPage;


    @BeforeEach
    void setUp() {

        // Démarrer Chrome
        driver = new ChromeDriver();

        driver.manage().window().maximize();

        // Initialiser notre Page Object
        userFormPage = new UserFormPage(driver);
    }


    @AfterEach
    void tearDown() {

        // Fermer Chrome
        if (driver != null) {
            driver.quit();
        }
    }


    // SEL-001 : vérifier l'affichage du formulaire
    @Test
    void shouldDisplayUserCreationForm() {

        // GIVEN : ouvrir BankFlow
        userFormPage.open();

        // THEN : vérifier le titre
        assertEquals(
                "BankFlow",
                userFormPage.getPageTitle()
        );

        // Vérifier les champs
        assertTrue(
                userFormPage.isFirstNameVisible()
        );

        assertTrue(
                userFormPage.isLastNameVisible()
        );

        assertTrue(
                userFormPage.isEmailVisible()
        );

        // Vérifier le bouton
        assertTrue(
                userFormPage.isCreateButtonVisible()
        );

        assertTrue(
                userFormPage.isCreateButtonEnabled()
        );
    }
}