
package com.bankflow.service;

import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;
import com.bankflow.entity.User;

import com.bankflow.exception.UserNotFoundException;

import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository) {

        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }


    // Créer un compte bancaire fictif
    @Transactional
    public Account createAccount(
            Long userId,
            AccountCurrency currency) {

        // 1. Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(userId)
                );

        // 2. Vérifier que la devise est renseignée
        if (currency == null) {
            throw new IllegalArgumentException(
                    "Account currency is required"
            );
        }

        // 3. Créer un nouveau compte
        Account account = new Account();

        // 4. Générer automatiquement un numéro
        account.setAccountNumber(
                generateAccountNumber()
        );

        // 5. Initialiser le solde à zéro
        account.setBalance(
                new BigDecimal("0.00")
        );

        // 6. Définir la devise
        account.setCurrency(currency);

        // 7. Définir le statut initial
        account.setStatus(AccountStatus.ACTIVE);

        // 8. Associer le compte à son propriétaire
        account.setUser(user);

        // 9. Enregistrer le compte dans PostgreSQL
        return accountRepository.save(account);
    }


    // Générer un numéro de compte fictif
    private String generateAccountNumber() {

        return "BF-"
                + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .toUpperCase(Locale.ROOT);
    }
}