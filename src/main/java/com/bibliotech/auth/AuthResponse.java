package com.bibliotech.auth;

public record AuthResponse(
        String token,
        String role,
        String nom,
        String email
) {}
