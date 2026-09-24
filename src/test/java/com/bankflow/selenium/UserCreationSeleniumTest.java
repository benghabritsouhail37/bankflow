
package com.bankflow.selenium;

import com.bankflow.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("local")
class UserCreationSeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private String createdEmail;

    @Autowired
    private UserRepository userRepository;

    private static final String BASE_URL =
            "http://localhost:5173";


    // Préparation du navigateur
    @BeforeEach
    void setUp() {

        driver = new ChromeDriver();

        driver.manage().window().maximize();

        wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );
    }


    // Nettoyage après chaque test
    @AfterEach
    void tearDown() {

        try {

            if (driver != null) {
                driver.quit();
            }

        } finally {

            // Supprimer l'utilisateur créé par le test
            if (createdEmail != null) {

                userRepository.findByEmail(createdEmail)
                        .ifPresent(userRepository::delete);
            }
        }
    }


    // SEL-002 : créer un utilisateur depuis React
    @Test
    void shouldCreateUserFromReactInterface() {

        // GIVEN : ouvrir BankFlow
        driver.get(BASE_URL);

        // Générer un email unique
        createdEmail =
                "selenium-" + UUID.randomUUID()
                        + "@example.com";


        // WHEN : remplir le formulaire

        WebElement firstName = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("firstName")
                )
        );

        firstName.sendKeys("Selenium");

        driver.findElement(By.id("lastName"))
                .sendKeys("Test");

        driver.findElement(By.id("email"))
                .sendKeys(createdEmail);


        // Cliquer sur le bouton de création

        driver.findElement(By.id("createUserButton"))
                .click();


// THEN : attendre le message de confirmation

WebElement successMessage = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
                By.id("message")
        )
);

wait.until(
        ExpectedConditions.textToBePresentInElement(
                successMessage,
                "Utilisateur Selenium créé avec succès !"
        )
);

// Vérifier que le message est affiché
assertTrue(successMessage.isDisplayed());


        // Vérifier réellement l'enregistrement PostgreSQL
        assertTrue(
                userRepository.findByEmail(createdEmail).isPresent(),
                "L'utilisateur doit exister dans PostgreSQL"
        );
    }
}