
package com.bankflow.repository;

import com.bankflow.entity.Account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    // Rechercher un compte par son numéro
    Optional<Account> findByAccountNumber(
            String accountNumber
    );

    // Vérifier si un numéro de compte existe déjà
    boolean existsByAccountNumber(
            String accountNumber
    );

    // Récupérer les comptes d'un utilisateur
    List<Account> findByUserId(Long userId);
}