
package com.bankflow.service;

import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;
import com.bankflow.entity.User;
import com.bankflow.exception.AccountBlockedException;
import com.bankflow.exception.AccountNotFoundException;
import com.bankflow.exception.InvalidAmountException;
import com.bankflow.exception.InsufficientFundsException;
import com.bankflow.entity.Transaction;
import com.bankflow.entity.TransactionType;
import com.bankflow.repository.TransactionRepository;


import com.bankflow.exception.UserNotFoundException;

import com.bankflow.repository.AccountRepository;
import com.bankflow.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

   public AccountService(
        AccountRepository accountRepository,
        UserRepository userRepository,
        TransactionRepository transactionRepository) {

    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
    this.transactionRepository = transactionRepository;
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
    @Transactional
public Account deposit(Long accountId, BigDecimal amount) {

    Account account = accountRepository.findById(accountId)
            .orElseThrow(
                    () -> new AccountNotFoundException(accountId)
            );

    if (amount == null ||
            amount.compareTo(BigDecimal.ZERO) <= 0) {

        throw new InvalidAmountException(
                "Deposit amount must be greater than 0"
        );
    }

    if (account.getStatus() == AccountStatus.BLOCKED) {

        throw new AccountBlockedException(accountId);
    }

    account.setBalance(
        account.getBalance().add(amount)
);

Account savedAccount =
        accountRepository.save(account);

Transaction transaction = new Transaction();

transaction.setType(TransactionType.DEPOSIT);
transaction.setAmount(amount);
transaction.setBalanceAfter(savedAccount.getBalance());
transaction.setAccount(savedAccount);

transactionRepository.save(transaction);

return savedAccount;
}
@Transactional
public Account withdraw(Long accountId, BigDecimal amount) {

    Account account = accountRepository.findById(accountId)
            .orElseThrow(
                    () -> new AccountNotFoundException(accountId)
            );

    if (amount == null ||
            amount.compareTo(BigDecimal.ZERO) <= 0) {

        throw new InvalidAmountException(
                "Withdrawal amount must be greater than 0"
        );
    }

    if (account.getStatus() == AccountStatus.BLOCKED) {
        throw new AccountBlockedException(accountId);
    }

    if (amount.compareTo(account.getBalance()) > 0) {
        throw new InsufficientFundsException(accountId);
    }

    account.setBalance(
        account.getBalance().subtract(amount)
);

Account savedAccount =
        accountRepository.save(account);

Transaction transaction = new Transaction();

transaction.setType(TransactionType.WITHDRAWAL);
transaction.setAmount(amount);
transaction.setBalanceAfter(savedAccount.getBalance());
transaction.setAccount(savedAccount);

transactionRepository.save(transaction);

return savedAccount;
}
@Transactional(readOnly = true)
public List<Transaction> getTransactions(Long accountId) {

    if (!accountRepository.existsById(accountId)) {
        throw new AccountNotFoundException(accountId);
    }

    return transactionRepository
            .findByAccountIdOrderByCreatedAtDesc(accountId);
}
@Transactional(readOnly = true)
public List<Account> getAccountsByUserId(Long userId) {

    if (!userRepository.existsById(userId)) {
        throw new UserNotFoundException(userId);
    }

    return accountRepository.findByUserId(userId);
}
}