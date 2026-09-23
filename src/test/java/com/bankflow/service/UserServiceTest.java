package com.bankflow.service;

import com.bankflow.entity.User;
import com.bankflow.exception.EmailAlreadyExistsException;
import com.bankflow.exception.UserNotFoundException;
import com.bankflow.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // TEST 1 : Création réussie
    @Test
    void shouldCreateUserWhenEmailDoesNotExist() {

        // GIVEN
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        // WHEN
        User result = userService.createUser(user);

        // THEN
        assertSame(user, result);
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository).save(user);
    }

    // TEST 2 : Email déjà existant
    @Test
    void shouldRejectUserWhenEmailAlreadyExists() {

        // GIVEN
        User user = new User();
        user.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        // WHEN + THEN
        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.createUser(user)
        );

        assertEquals(
                "A user with email test@example.com already exists",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
    }

    // TEST 3 : Recherche d'un utilisateur existant
    @Test
    void shouldReturnUserWhenIdExists() {

        // GIVEN
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // WHEN
        User result = userService.getUserById(1L);

        // THEN
        assertSame(user, result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository).findById(1L);
    }

    // TEST 4 : Recherche d'un utilisateur inexistant
    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        // GIVEN
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // WHEN + THEN
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(999L)
        );

        assertEquals(
                "User not found with id: 999",
                exception.getMessage()
        );

        verify(userRepository).findById(999L);
    }
}