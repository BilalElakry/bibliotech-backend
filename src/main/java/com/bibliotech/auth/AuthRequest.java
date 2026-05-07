package com.bibliotech.auth;

import jakarta.validation.constraints.*;

public record AuthRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {}