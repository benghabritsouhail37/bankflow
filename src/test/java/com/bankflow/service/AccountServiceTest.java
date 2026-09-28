
package com.bankflow.service;

import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;
import com.bankflow.entity.User;
import com.bankflow.exception.UserNotFoundException;

import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    // Simuler le repository des comptes
    @Mock
    private AccountRepository accountRepository;

    // Simuler le repository des utilisateurs
    @Mock
    private UserRepository userRepository;

    // Service réel que nous allons tester
    @InjectMocks
    private AccountService accountService;


    // TEST 1 : création réussie d'un compte bancaire
    @Test
    void shouldCreateAccountForExistingUser() {

        // ==============================
        // GIVEN : préparation du scénario
        // ==============================

        // Créer un utilisateur fictif
        User user = new User();

        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");

        // Simuler un utilisateur existant
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // Simuler l'enregistrement du compte
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );


        // ==============================
        // WHEN : exécution de la méthode
        // ==============================

        Account result = accountService.createAccount(
                1L,
                AccountCurrency.MAD
        );


        // ==============================
        // THEN : vérification des résultats
        // ==============================

        // Vérifier que le compte a été créé
        assertNotNull(result);

        // Vérifier son propriétaire
        assertSame(user, result.getUser());

        // Vérifier la devise
        assertEquals(
                AccountCurrency.MAD,
                result.getCurrency()
        );

        // Vérifier le solde initial
        assertEquals(
                0,
                new BigDecimal("0.00")
                        .compareTo(result.getBalance())
        );

        // Vérifier le statut initial
        assertEquals(
                AccountStatus.ACTIVE,
                result.getStatus()
        );

        // Vérifier le numéro de compte généré
        assertNotNull(result.getAccountNumber());

        assertTrue(
                result.getAccountNumber()
                        .matches("BF-[A-F0-9]{32}")
        );

        // Vérifier les interactions avec les repositories
        verify(userRepository).findById(1L);

        verify(accountRepository).save(result);
    }

    
    // TEST 2 : refuser la création si l'utilisateur n'existe pas
    @Test
    void shouldRejectAccountCreationWhenUserDoesNotExist() {

        // GIVEN : aucun utilisateur avec l'ID 999
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // WHEN + THEN : la création doit lever une exception
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> accountService.createAccount(
                        999L,
                        AccountCurrency.MAD
                )
        );

        // Vérifier le message de l'exception
        assertEquals(
                "User not found with id: 999",
                exception.getMessage()
        );

        // Vérifier que le propriétaire a été recherché
        verify(userRepository).findById(999L);

        // Aucun compte ne doit être enregistré
        verify(
                accountRepository,
                never()
        ).save(any(Account.class));
    }

    
    // TEST 3 : refuser la création si la devise est absente
    @Test
    void shouldRejectAccountCreationWhenCurrencyIsNull() {

        // GIVEN : un utilisateur existant
        User user = new User();

        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");

        // Simuler l'existence de cet utilisateur
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));


        // WHEN + THEN : tenter une création sans devise
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(
                        1L,
                        null
                )
        );


        // Vérifier le message de l'exception
        assertEquals(
                "Account currency is required",
                exception.getMessage()
        );


        // Vérifier que le propriétaire a bien été recherché
        verify(userRepository).findById(1L);


        // Vérifier qu'aucun compte n'a été enregistré
        verify(
                accountRepository,
                never()
        ).save(any(Account.class));
    }

    
    // TEST 4 : deux comptes doivent avoir des numéros différents
    @Test
    void shouldGenerateDifferentAccountNumbers() {

        // GIVEN : préparer un utilisateur existant

        User user = new User();

        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");


        // Simuler l'existence de l'utilisateur

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));


        // Simuler l'enregistrement des comptes

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );


        // WHEN : créer deux comptes pour le même utilisateur

        Account firstAccount = accountService.createAccount(
                1L,
                AccountCurrency.MAD
        );

        Account secondAccount = accountService.createAccount(
                1L,
                AccountCurrency.EUR
        );


        // THEN : vérifier que les deux comptes existent

        assertNotNull(firstAccount);
        assertNotNull(secondAccount);


        // Vérifier que les numéros ont bien été générés

        assertNotNull(firstAccount.getAccountNumber());
        assertNotNull(secondAccount.getAccountNumber());


        // Vérifier que les deux numéros sont différents

        assertNotEquals(
                firstAccount.getAccountNumber(),
                secondAccount.getAccountNumber()
        );


        // Vérifier le format des deux numéros

        assertTrue(
                firstAccount.getAccountNumber()
                        .matches("BF-[A-F0-9]{32}")
        );

        assertTrue(
                secondAccount.getAccountNumber()
                        .matches("BF-[A-F0-9]{32}")
        );


        // Vérifier que les deux comptes appartiennent
        // au même utilisateur

        assertSame(user, firstAccount.getUser());
        assertSame(user, secondAccount.getUser());


        // Vérifier que les deux enregistrements
        // ont été demandés au repository

        verify(
                accountRepository,
                times(2)
        ).save(any(Account.class));
    }
}