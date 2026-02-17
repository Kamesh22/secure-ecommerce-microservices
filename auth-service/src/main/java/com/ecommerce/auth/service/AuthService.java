package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthResponseDTO;
import com.ecommerce.auth.dto.LoginRequestDTO;
import com.ecommerce.auth.dto.RegisterRequestDTO;
import com.ecommerce.auth.dto.TokenValidationResponseDTO;

public interface AuthService {

    /**
     * Register a new user
     */
    AuthResponseDTO register(RegisterRequestDTO registerRequestDTO);

    /**
     * Login user with credentials
     */
    AuthResponseDTO login(LoginRequestDTO loginRequestDTO);

    /**
     * Validate JWT token
     */
    TokenValidationResponseDTO validateToken(String token);
}
