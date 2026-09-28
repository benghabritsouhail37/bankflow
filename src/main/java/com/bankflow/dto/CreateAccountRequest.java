
package com.bankflow.dto;

import com.bankflow.entity.AccountCurrency;

import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(

        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Account currency is required")
        AccountCurrency currency

) {
}