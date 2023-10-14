package com.ipap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record EmployeeDto(
        @NotNull(message =  "Firstname is mandatory")
        String firstname,
        @NotNull(message =  "Lastname is mandatory")
        String lastname,
        @Email
        @NotNull(message =  "Email is mandatory")
        String email
) {
}
