package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.dto.AuthResponseDTO;
import com.ecommerce.auth.dto.LoginRequestDTO;
import com.ecommerce.auth.dto.RegisterRequestDTO;
import com.ecommerce.auth.dto.TokenValidationResponseDTO;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserRole;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.auth.util.JwtUtil;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * REGISTER USER
     */
    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO dto) {

        log.info("Register request username={}", dto.getUsername());

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(UserRole.USER);
        user.setEnabled(true);

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        return AuthResponseDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .message("User registered successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO dto) {

        log.info("Login request username={}", dto.getUsername());

        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("User account is disabled");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);

        return AuthResponseDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .message("Login successful")
                .build();
    }

    //VALIDATE TOKEN (used by API Gateway)
    @Override
    @Transactional(readOnly = true)
    public TokenValidationResponseDTO validateToken(String token) {

        TokenValidationResponseDTO response = new TokenValidationResponseDTO();

        try {

            if (!jwtUtil.validateToken(token)) {
                response.setValid(false);
                response.setMessage("Invalid token");
                return response;
            }

            Long userId = jwtUtil.extractUserId(token);
            String username = jwtUtil.extractUsername(token);
            UserRole role = jwtUtil.extractRole(token);

            // verify user still exists
            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            response.setValid(true);
            response.setUserId(userId);
            response.setUsername(username);
            response.setRole(role);
            response.setMessage("Token valid");

            return response;

        } catch (Exception ex) {
            log.error("Token validation failed: {}", ex.getMessage());

            response.setValid(false);
            response.setMessage("Token validation failed");

            return response;
        }
    }
}