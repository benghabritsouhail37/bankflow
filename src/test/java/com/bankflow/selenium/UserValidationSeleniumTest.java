
package com.bankflow.selenium;

import com.bankflow.entity.User;
import com.bankflow.repository.UserRepository;
import com.bankflow.selenium.pages.UserFormPage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("local")
class UserValidationSeleniumTest {

    private WebDriver driver;

    private UserFormPage userFormPage;

    private Long createdTestUserId;

    @Autowired
    private UserRepository userRepository;


    // Préparation du navigateur avant chaque test
    @BeforeEach
    void setUp() {

        driver = new ChromeDriver();

        driver.manage().window().maximize();

        userFormPage = new UserFormPage(driver);
    }


    // Fermeture du navigateur et nettoyage PostgreSQL
    @AfterEach
    void tearDown() {

        try {

            if (driver != null) {
                driver.quit();
            }

        } finally {

            if (createdTestUserId != null) {

                userRepository.findById(createdTestUserId)
                        .ifPresent(userRepository::delete);
            }
        }
    }


    // SEL-003 : email invalide
    @Test
    void shouldDisplayValidationErrorWhenEmailIsInvalid() {

        // GIVEN
        String invalidEmail =
                "invalid-" + UUID.randomUUID();

        assertFalse(
                userRepository.findByEmail(invalidEmail)
                        .isPresent()
        );

        userFormPage.open();


        // WHEN
        userFormPage.fillForm(
                "Selenium",
                "Invalid",
                invalidEmail
        );

        userFormPage.submit();


        // THEN : vérifier le message de validation
        assertEquals(
                "Email must be valid",
                userFormPage.getEmailError()
        );


        // Vérifier qu'aucun utilisateur n'a été créé
        assertFalse(
                userRepository.findByEmail(invalidEmail)
                        .isPresent(),
                "Un email invalide ne doit pas être enregistré"
        );
    }


    // SEL-004 : email déjà existant
    @Test
    void shouldDisplayErrorWhenEmailAlreadyExists() {

        // GIVEN : créer un utilisateur existant
        String email =
                "selenium-duplicate-"
                        + UUID.randomUUID()
                        + "@example.com";

        User existingUser = new User();

        existingUser.setFirstName("First");
        existingUser.setLastName("User");
        existingUser.setEmail(email);


        // Préparer la base PostgreSQL
        User savedUser =
                userRepository.saveAndFlush(existingUser);

        createdTestUserId = savedUser.getId();

        assertNotNull(createdTestUserId);


        // WHEN : tenter une deuxième création
        userFormPage.open();

        userFormPage.fillForm(
                "Second",
                "User",
                email
        );

        userFormPage.submit();


        // THEN : vérifier le message de doublon
        String expectedMessage =
                "A user with email "
                        + email
                        + " already exists";

        assertEquals(
                expectedMessage,
                userFormPage.getDuplicateEmailMessage(email)
        );


        // Vérifier que l'utilisateur initial existe toujours
        User userAfterAttempt = userRepository
                .findByEmail(email)
                .orElseThrow();


        // Vérifier que son ID n'a pas changé
        assertEquals(
                createdTestUserId,
                userAfterAttempt.getId()
        );


        // Vérifier que ses informations sont inchangées
        assertEquals(
                "First",
                userAfterAttempt.getFirstName()
        );

        assertEquals(
                "User",
                userAfterAttempt.getLastName()
        );
    }


    // SEL-005 : prénom vide
    @Test
    void shouldDisplayValidationErrorWhenFirstNameIsBlank() {

        // GIVEN
        userFormPage.open();


        // WHEN
        userFormPage.fillForm(
                "   ",
                "Selenium",
                "valid-test@example.com"
        );

        userFormPage.submit();


        // THEN : vérifier l'erreur du prénom
        assertEquals(
                "First name is required",
                userFormPage.getFirstNameError()
        );


        // Vérifier que le formulaire conserve la valeur
        assertEquals(
                "   ",
                userFormPage.getFirstNameValue()
        );
    }


    // SEL-006 : plusieurs champs invalides
    @Test
    void shouldDisplayMultipleValidationErrors() {

        // GIVEN
        String invalidEmail =
                "invalid-" + UUID.randomUUID();

        assertFalse(
                userRepository.findByEmail(invalidEmail)
                        .isPresent()
        );

        userFormPage.open();


        // WHEN
        userFormPage.fillForm(
                "   ",
                "Selenium",
                invalidEmail
        );

        userFormPage.submit();


        // THEN : vérifier les deux erreurs
        assertEquals(
                "First name is required",
                userFormPage.getFirstNameError()
        );

        assertEquals(
                "Email must be valid",
                userFormPage.getEmailError()
        );


        // Vérifier que les valeurs sont conservées
        assertEquals(
                "   ",
                userFormPage.getFirstNameValue()
        );

        assertEquals(
                invalidEmail,
                userFormPage.getEmailValue()
        );


        // Vérifier qu'aucun utilisateur n'a été créé
        assertFalse(
                userRepository.findByEmail(invalidEmail)
                        .isPresent(),
                "Aucun utilisateur invalide ne doit être créé"
        );
    }
}