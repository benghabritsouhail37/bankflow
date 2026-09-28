
package com.bankflow.dto;

import com.bankflow.entity.Account;
import com.bankflow.entity.AccountCurrency;
import com.bankflow.entity.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(

        Long id,
        String accountNumber,
        BigDecimal balance,
        AccountCurrency currency,
        AccountStatus status,
        Long userId,
        LocalDateTime createdAt

) {

    public static AccountResponse from(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getUser().getId(),
                account.getCreatedAt()
        );
    }
}