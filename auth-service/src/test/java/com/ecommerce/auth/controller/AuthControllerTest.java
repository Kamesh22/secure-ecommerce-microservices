package com.ecommerce.auth.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.auth.dto.AuthResponseDTO;
import com.ecommerce.auth.dto.LoginRequestDTO;
import com.ecommerce.auth.dto.RegisterRequestDTO;
import com.ecommerce.auth.dto.TokenValidationResponseDTO;
import com.ecommerce.auth.entity.UserRole;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Test Suite")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private AuthService authService;

    // Test Constants
    private static final String BASE_URL = "/api/auth";
    private static final String REGISTER_ENDPOINT = BASE_URL + "/register";
    private static final String LOGIN_ENDPOINT = BASE_URL + "/login";
    private static final String VALIDATE_ENDPOINT = BASE_URL + "/validate";

    private static final String TEST_USER = "Rajesh_Kumar";
    private static final String TEST_EMAIL = "rajesh.kumar@example.com";
    private static final String TEST_PASSWORD = "SecurePass@123";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

    private RegisterRequestDTO registerRequestDTO;
    private LoginRequestDTO loginRequestDTO;
    private AuthResponseDTO authResponseDTO;
    private TokenValidationResponseDTO tokenValidationResponseDTO;

    @BeforeEach
    void setUp() {
        registerRequestDTO = RegisterRequestDTO.builder()
                .username(TEST_USER)
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        loginRequestDTO = LoginRequestDTO.builder()
                .username(TEST_USER)
                .password(TEST_PASSWORD)
                .build();

        authResponseDTO = AuthResponseDTO.builder()
                .userId(TEST_USER_ID)
                .username(TEST_USER)
                .email(TEST_EMAIL)
                .role(UserRole.USER)
                .token(TEST_TOKEN)
                .message("Operation successful")
                .build();

        tokenValidationResponseDTO = TokenValidationResponseDTO.builder()
                .userId(TEST_USER_ID)
                .username(TEST_USER)
                .role(UserRole.USER)
                .valid(true)
                .message("Token valid")
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/register Tests")
    class RegisterTests {

        @Test
        @DisplayName("shouldReturnCreated_whenValidRegistrationRequest")
        void shouldReturnCreated_whenValidRegistrationRequest() throws Exception {
            // Arrange
            when(authService.register(any(RegisterRequestDTO.class)))
                    .thenReturn(authResponseDTO);

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId", equalTo(TEST_USER_ID.intValue())))
                    .andExpect(jsonPath("$.username", equalTo(TEST_USER)))
                    .andExpect(jsonPath("$.email", equalTo(TEST_EMAIL)))
                    .andExpect(jsonPath("$.role", equalTo("USER")))
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.message", equalTo("Operation successful")));

            verify(authService, times(1)).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenUsernameAlreadyExists")
        void shouldReturnBadRequest_whenUsernameAlreadyExists() throws Exception {
            // Arrange
            when(authService.register(any(RegisterRequestDTO.class)))
                    .thenThrow(new BusinessException("Username already exists"));

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isBadRequest());

            verify(authService, times(1)).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenEmailAlreadyExists")
        void shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
            // Arrange
            when(authService.register(any(RegisterRequestDTO.class)))
                    .thenThrow(new BusinessException("Email already exists"));

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isBadRequest());

            verify(authService, times(1)).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenUsernameIsBlank")
        void shouldReturnBadRequest_whenUsernameIsBlank() throws Exception {
            // Arrange
            registerRequestDTO.setUsername("");

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService, never()).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenUsernameTooShort")
        void shouldReturnBadRequest_whenUsernameTooShort() throws Exception {
            // Arrange
            registerRequestDTO.setUsername("ab");

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService, never()).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenPasswordIsBlank")
        void shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {
            // Arrange
            registerRequestDTO.setPassword("");

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService, never()).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenPasswordTooShort")
        void shouldReturnBadRequest_whenPasswordTooShort() throws Exception {
            // Arrange
            registerRequestDTO.setPassword("pass");

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService, never()).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenEmailIsInvalid")
        void shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
            // Arrange
            registerRequestDTO.setEmail("invalid-email");

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService, never()).register(any(RegisterRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenEmailIsBlank")
        void shouldReturnBadRequest_whenEmailIsBlank() throws Exception {
            // Arrange
            registerRequestDTO.setEmail("");

            // Act & Assert
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService, never()).register(any(RegisterRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login Tests")
    class LoginTests {

        @Test
        @DisplayName("shouldReturnOk_whenValidLoginRequest")
        void shouldReturnOk_whenValidLoginRequest() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenReturn(authResponseDTO);

            // Act & Assert
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId", equalTo(TEST_USER_ID.intValue())))
                    .andExpect(jsonPath("$.username", equalTo(TEST_USER)))
                    .andExpect(jsonPath("$.email", equalTo(TEST_EMAIL)))
                    .andExpect(jsonPath("$.role", equalTo("USER")))
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.message", equalTo("Operation successful")));

            verify(authService, times(1)).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenUserNotFound")
        void shouldReturnBadRequest_whenUserNotFound() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            // Act & Assert
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isNotFound());

            verify(authService, times(1)).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenInvalidCredentials")
        void shouldReturnBadRequest_whenInvalidCredentials() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new BusinessException("Invalid credentials"));

            // Act & Assert
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isBadRequest());

            verify(authService, times(1)).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenUserAccountDisabled")
        void shouldReturnBadRequest_whenUserAccountDisabled() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new BusinessException("User account is disabled"));

            // Act & Assert
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isBadRequest());

            verify(authService, times(1)).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenUsernameIsBlank")
        void shouldReturnBadRequest_whenUsernameIsBlank() throws Exception {
            // Arrange
            loginRequestDTO.setUsername("");

            // Act & Assert
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService, never()).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("shouldReturnBadRequest_whenPasswordIsBlank")
        void shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {
            // Arrange
            loginRequestDTO.setPassword("");

            // Act & Assert
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService, never()).login(any(LoginRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/validate Tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("shouldReturnOk_whenValidTokenProvided")
        void shouldReturnOk_whenValidTokenProvided() throws Exception {
            // Arrange
            String authHeader = "Bearer " + TEST_TOKEN;
            when(authService.validateToken(TEST_TOKEN))
                    .thenReturn(tokenValidationResponseDTO);

            // Act & Assert
            mockMvc.perform(get(VALIDATE_ENDPOINT)
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId", equalTo(TEST_USER_ID.intValue())))
                    .andExpect(jsonPath("$.username", equalTo(TEST_USER)))
                    .andExpect(jsonPath("$.role", equalTo("USER")))
                    .andExpect(jsonPath("$.valid", equalTo(true)))
                    .andExpect(jsonPath("$.message", equalTo("Token valid")));

            verify(authService, times(1)).validateToken(TEST_TOKEN);
        }

        @Test
        @DisplayName("shouldReturnUnauthorized_whenTokenIsInvalid")
        void shouldReturnUnauthorized_whenTokenIsInvalid() throws Exception {
            // Arrange
            String authHeader = "Bearer invalid.token.here";
            TokenValidationResponseDTO invalidTokenResponse = TokenValidationResponseDTO.builder()
                    .valid(false)
                    .message("Invalid token")
                    .build();

            when(authService.validateToken("invalid.token.here"))
                    .thenReturn(invalidTokenResponse);

            // Act & Assert
            mockMvc.perform(get(VALIDATE_ENDPOINT)
                    .header("Authorization", authHeader))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.valid", equalTo(false)));

            verify(authService, times(1)).validateToken("invalid.token.here");
        }

        @Test
        @DisplayName("shouldReturnUnauthorized_whenAuthorizationHeaderMissing")
        void shouldReturnUnauthorized_whenAuthorizationHeaderMissing() throws Exception {
            // Act & Assert
            mockMvc.perform(get(VALIDATE_ENDPOINT))
                    .andExpect(status().isUnauthorized());

            verify(authService, never()).validateToken(any());
        }

        @Test
        @DisplayName("shouldReturnUnauthorized_whenAuthorizationHeaderInvalid")
        void shouldReturnUnauthorized_whenAuthorizationHeaderInvalid() throws Exception {
            // Arrange
            String authHeader = "Basic " + TEST_TOKEN;

            // Act & Assert
            mockMvc.perform(get(VALIDATE_ENDPOINT)
                    .header("Authorization", authHeader))
                    .andExpect(status().isUnauthorized());

            verify(authService, never()).validateToken(any());
        }

        }
}
