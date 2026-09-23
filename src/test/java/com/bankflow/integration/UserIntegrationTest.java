package com.bankflow.integration;

import com.bankflow.entity.User;
import com.bankflow.repository.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;


    // TEST 1 : création réelle + vérification PostgreSQL
    @Test
    void shouldCreateUserAndSaveItInDatabase() throws Exception {

        // GIVEN
        String email =
                "integration-" + UUID.randomUUID() + "@example.com";

        // WHEN + vérification HTTP
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Integration",
                                  "lastName": "Test",
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Integration"))
                .andExpect(jsonPath("$.lastName").value("Test"))
                .andExpect(jsonPath("$.email").value(email));

        // THEN : vérification réelle dans PostgreSQL
        Optional<User> savedUser =
                userRepository.findByEmail(email);

        assertTrue(savedUser.isPresent());

        assertEquals(
                "Integration",
                savedUser.get().getFirstName()
        );

        assertEquals(
                "Test",
                savedUser.get().getLastName()
        );

        assertEquals(
                email,
                savedUser.get().getEmail()
        );
    }


    // TEST 2 : email dupliqué -> 409 et aucun deuxième enregistrement
    @Test
    void shouldReturn409AndNotCreateDuplicateUser() throws Exception {

        // GIVEN : email unique pour ce test
        String email =
                "duplicate-" + UUID.randomUUID() + "@example.com";

        long initialUserCount = userRepository.count();

        // Première création : doit fonctionner
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "First",
                                  "lastName": "User",
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        // Vérifier qu'un utilisateur a bien été ajouté
        assertEquals(
                initialUserCount + 1,
                userRepository.count()
        );

        // WHEN : deuxième création avec le même email
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Second",
                                  "lastName": "User",
                                  "email": "%s"
                                }
                                """.formatted(email)))

                // THEN : l'API doit refuser
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Email already exists"))
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.detail")
                        .value(
                                "A user with email "
                                        + email
                                        + " already exists"
                        ));

        // Vérifier qu'aucun deuxième utilisateur
        // n'a été enregistré
        assertEquals(
                initialUserCount + 1,
                userRepository.count()
        );

        // L'email existe bien une seule fois du point
        // de vue de notre logique de repository
        Optional<User> savedUser =
                userRepository.findByEmail(email);

        assertTrue(savedUser.isPresent());

        assertEquals(
                "First",
                savedUser.get().getFirstName()
        );
    }
}