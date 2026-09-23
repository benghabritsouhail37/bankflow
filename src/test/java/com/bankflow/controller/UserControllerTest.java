package com.bankflow.controller;

import com.bankflow.entity.User;
import com.bankflow.exception.EmailAlreadyExistsException;
import com.bankflow.exception.GlobalExceptionHandler;
import com.bankflow.exception.UserNotFoundException;
import com.bankflow.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;


    // TEST 1 : utilisateur inexistant -> 404
    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {

        // GIVEN
        when(userService.getUserById(999L))
                .thenThrow(new UserNotFoundException(999L));

        // WHEN + THEN
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.status").value(404));
    }


    // TEST 2 : création réussie -> 201
    @Test
    void shouldReturn201WhenUserIsCreated() throws Exception {

        // GIVEN
        User savedUser = new User();
        savedUser.setId(42L);
        savedUser.setFirstName("Test");
        savedUser.setLastName("User");
        savedUser.setEmail("test@example.com");

        when(userService.createUser(any(User.class)))
                .thenReturn(savedUser);

        // WHEN + THEN
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "User",
                                  "email": "test@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).createUser(any(User.class));
    }


    // TEST 3 : email déjà existant -> 409
    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {

        // GIVEN
        when(userService.createUser(any(User.class)))
                .thenThrow(
                        new EmailAlreadyExistsException("test@example.com")
                );

        // WHEN + THEN
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "User",
                                  "email": "test@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value("A user with email test@example.com already exists"));
    }


    // TEST 4 : email invalide -> 400
    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {

        // WHEN + THEN
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "User",
                                  "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.email")
                        .value("Email must be valid"));

        // Le service ne doit jamais être appelé
        verify(userService, never()).createUser(any(User.class));
    }

    // TEST 5 : prénom vide -> 400
@Test
void shouldReturn400WhenFirstNameIsBlank() throws Exception {

    // WHEN + THEN
    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "firstName": "   ",
                              "lastName": "User",
                              "email": "test@example.com"
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.firstName")
                    .value("First name is required"));

    // Le service ne doit pas être appelé
    verify(userService, never()).createUser(any(User.class));
}

// TEST 6 : email manquant -> 400
@Test
void shouldReturn400WhenEmailIsMissing() throws Exception {

    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "firstName": "Test",
                              "lastName": "User"
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.email")
                    .value("Email is required"));

    verify(userService, never()).createUser(any(User.class));
}

// TEST 7 : prénom trop long -> 400
@Test
void shouldReturn400WhenFirstNameIsTooLong() throws Exception {

    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "firstName": "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXY",
                              "lastName": "User",
                              "email": "test@example.com"
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.firstName")
                    .value("First name must not exceed 50 characters"));

    verify(userService, never()).createUser(any(User.class));
}

// TEST 8 : plusieurs erreurs de validation -> 400
@Test
void shouldReturn400WithMultipleValidationErrors() throws Exception {

    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "firstName": "   ",
                              "lastName": "User",
                              "email": "invalid-email"
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.firstName")
                    .value("First name is required"))
            .andExpect(jsonPath("$.errors.email")
                    .value("Email must be valid"));

    verify(userService, never()).createUser(any(User.class));
}
}