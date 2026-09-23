package com.bankflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "First name is required")
        @Size(
                max = 50,
                message = "First name must not exceed 50 characters"
        )
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(
                max = 50,
                message = "Last name must not exceed 50 characters"
        )
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(
                max = 150,
                message = "Email must not exceed 150 characters"
        )
        String email
) {
}