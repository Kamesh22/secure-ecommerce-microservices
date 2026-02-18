package com.ecommerce.auth.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.auth.dto.AuthResponseDTO;
import com.ecommerce.auth.dto.LoginRequestDTO;
import com.ecommerce.auth.dto.RegisterRequestDTO;
import com.ecommerce.auth.dto.TokenValidationResponseDTO;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserRole;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.util.JwtUtil;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Test Suite")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    // Test Constants
    private static final String TEST_USERNAME = "Rajesh_Kumar";
    private static final String TEST_EMAIL = "rajesh.kumar@example.com";
    private static final String TEST_PASSWORD = "SecurePass@123";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPasswordHash";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

    private User testUser;
    private RegisterRequestDTO registerRequestDTO;
    private LoginRequestDTO loginRequestDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .role(UserRole.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registerRequestDTO = RegisterRequestDTO.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        loginRequestDTO = LoginRequestDTO.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();
    }

    @Nested
    @DisplayName("Register User Tests")
    class RegisterUserTests {

        @Test
        @DisplayName("shouldSuccessfullyRegisterUser_whenValidRequestProvided")
        void shouldSuccessfullyRegisterUser_whenValidRequestProvided() {
            // Arrange
            when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(testUser)).thenReturn(TEST_TOKEN);

            // Act
            AuthResponseDTO response = authService.register(registerRequestDTO);

            // Assert
            assertNotNull(response);
            assertEquals(TEST_USER_ID, response.getUserId());
            assertEquals(TEST_USERNAME, response.getUsername());
            assertEquals(TEST_EMAIL, response.getEmail());
            assertEquals(UserRole.USER, response.getRole());
            assertEquals(TEST_TOKEN, response.getToken());
            assertEquals("User registered successfully", response.getMessage());

            verify(userRepository, times(1)).existsByUsername(TEST_USERNAME);
            verify(userRepository, times(1)).existsByEmail(TEST_EMAIL);
            verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
            verify(userRepository, times(1)).save(any(User.class));
            verify(jwtUtil, times(1)).generateToken(testUser);
        }

        @Test
        @DisplayName("shouldThrowException_whenUsernameAlreadyExists")
        void shouldThrowException_whenUsernameAlreadyExists() {
            // Arrange
            when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    authService.register(registerRequestDTO));

            assertEquals("Username already exists", exception.getMessage());
            verify(userRepository, times(1)).existsByUsername(TEST_USERNAME);
            verify(userRepository, never()).existsByEmail(any());
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any(User.class));
            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("shouldThrowException_whenEmailAlreadyExists")
        void shouldThrowException_whenEmailAlreadyExists() {
            // Arrange
            when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    authService.register(registerRequestDTO));

            assertEquals("Email already exists", exception.getMessage());
            verify(userRepository, times(1)).existsByUsername(TEST_USERNAME);
            verify(userRepository, times(1)).existsByEmail(TEST_EMAIL);
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any(User.class));
            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("shouldEncodePasswordCorrectly_whenRegisteringUser")
        void shouldEncodePasswordCorrectly_whenRegisteringUser() {
            // Arrange
            when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(testUser)).thenReturn(TEST_TOKEN);

            // Act
            authService.register(registerRequestDTO);

            // Assert
            verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        }

        @Test
        @DisplayName("shouldGenerateTokenAfterSuccessfulRegistration")
        void shouldGenerateTokenAfterSuccessfulRegistration() {
            // Arrange
            when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(testUser)).thenReturn(TEST_TOKEN);

            // Act
            AuthResponseDTO response = authService.register(registerRequestDTO);

            // Assert
            assertNotNull(response.getToken());
            assertEquals(TEST_TOKEN, response.getToken());
            verify(jwtUtil, times(1)).generateToken(eq(testUser));
        }

        @Test
        @DisplayName("shouldSetUserRoleToDefault_whenRegisteringNewUser")
        void shouldSetUserRoleToDefault_whenRegisteringNewUser() {
            // Arrange
            when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(testUser)).thenReturn(TEST_TOKEN);

            // Act
            AuthResponseDTO response = authService.register(registerRequestDTO);

            // Assert
            assertEquals(UserRole.USER, response.getRole());
        }
    }

    @Nested
    @DisplayName("Login User Tests")
    class LoginUserTests {

        @Test
        @DisplayName("shouldSuccessfullyLoginUser_whenValidCredentialsProvided")
        void shouldSuccessfullyLoginUser_whenValidCredentialsProvided() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(testUser)).thenReturn(TEST_TOKEN);

            // Act
            AuthResponseDTO response = authService.login(loginRequestDTO);

            // Assert
            assertNotNull(response);
            assertEquals(TEST_USER_ID, response.getUserId());
            assertEquals(TEST_USERNAME, response.getUsername());
            assertEquals(TEST_EMAIL, response.getEmail());
            assertEquals(UserRole.USER, response.getRole());
            assertEquals(TEST_TOKEN, response.getToken());
            assertEquals("Login successful", response.getMessage());

            verify(userRepository, times(1)).findByUsername(TEST_USERNAME);
            verify(passwordEncoder, times(1)).matches(TEST_PASSWORD, ENCODED_PASSWORD);
            verify(jwtUtil, times(1)).generateToken(testUser);
        }

        @Test
        @DisplayName("shouldThrowException_whenUserNotFound")
        void shouldThrowException_whenUserNotFound() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    authService.login(loginRequestDTO));

            assertEquals("User not found", exception.getMessage());
            verify(userRepository, times(1)).findByUsername(TEST_USERNAME);
            verify(passwordEncoder, never()).matches(any(), any());
            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("shouldThrowException_whenInvalidPasswordProvided")
        void shouldThrowException_whenInvalidPasswordProvided() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    authService.login(loginRequestDTO));

            assertEquals("Invalid credentials", exception.getMessage());
            verify(userRepository, times(1)).findByUsername(TEST_USERNAME);
            verify(passwordEncoder, times(1)).matches(TEST_PASSWORD, ENCODED_PASSWORD);
            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("shouldThrowException_whenUserAccountIsDisabled")
        void shouldThrowException_whenUserAccountIsDisabled() {
            // Arrange
            testUser.setEnabled(false);
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    authService.login(loginRequestDTO));

            assertEquals("User account is disabled", exception.getMessage());
            verify(userRepository, times(1)).findByUsername(TEST_USERNAME);
            verify(passwordEncoder, never()).matches(any(), any());
            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("shouldGenerateTokenAfterSuccessfulLogin")
        void shouldGenerateTokenAfterSuccessfulLogin() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(testUser)).thenReturn(TEST_TOKEN);

            // Act
            AuthResponseDTO response = authService.login(loginRequestDTO);

            // Assert
            assertNotNull(response.getToken());
            assertEquals(TEST_TOKEN, response.getToken());
            verify(jwtUtil, times(1)).generateToken(eq(testUser));
        }

        @Test
        @DisplayName("shouldVerifyPasswordMatch_whenAuthenticatingUser")
        void shouldVerifyPasswordMatch_whenAuthenticatingUser() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(testUser)).thenReturn(TEST_TOKEN);

            // Act
            authService.login(loginRequestDTO);

            // Assert
            verify(passwordEncoder, times(1)).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        }
    }

    @Nested
    @DisplayName("Validate Token Tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("shouldReturnValidTokenResponse_whenValidTokenProvided")
        void shouldReturnValidTokenResponse_whenValidTokenProvided() {
            // Arrange
            when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(true);
            when(jwtUtil.extractUserId(TEST_TOKEN)).thenReturn(TEST_USER_ID);
            when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
            when(jwtUtil.extractRole(TEST_TOKEN)).thenReturn(UserRole.USER);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // Act
            TokenValidationResponseDTO response = authService.validateToken(TEST_TOKEN);

            // Assert
            assertNotNull(response);
            assertTrue(response.getValid());
            assertEquals(TEST_USER_ID, response.getUserId());
            assertEquals(TEST_USERNAME, response.getUsername());
            assertEquals(UserRole.USER, response.getRole());
            assertEquals("Token valid", response.getMessage());

            verify(jwtUtil, times(1)).validateToken(TEST_TOKEN);
            verify(jwtUtil, times(1)).extractUserId(TEST_TOKEN);
            verify(jwtUtil, times(1)).extractUsername(TEST_TOKEN);
            verify(jwtUtil, times(1)).extractRole(TEST_TOKEN);
            verify(userRepository, times(1)).findById(TEST_USER_ID);
        }

        @Test
        @DisplayName("shouldReturnInvalidTokenResponse_whenTokenIsExpired")
        void shouldReturnInvalidTokenResponse_whenTokenIsExpired() {
            // Arrange
            when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(false);

            // Act
            TokenValidationResponseDTO response = authService.validateToken(TEST_TOKEN);

            // Assert
            assertNotNull(response);
            assertFalse(response.getValid());
            assertEquals("Invalid token", response.getMessage());

            verify(jwtUtil, times(1)).validateToken(TEST_TOKEN);
            verify(jwtUtil, never()).extractUserId(any());
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("shouldReturnInvalidTokenResponse_whenUserNotFound")
        void shouldReturnInvalidTokenResponse_whenUserNotFound() {
            // Arrange
            when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(true);
            when(jwtUtil.extractUserId(TEST_TOKEN)).thenReturn(TEST_USER_ID);
            when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
            when(jwtUtil.extractRole(TEST_TOKEN)).thenReturn(UserRole.USER);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // Act
            TokenValidationResponseDTO response = authService.validateToken(TEST_TOKEN);

            // Assert
            assertNotNull(response);
            assertFalse(response.getValid());
            assertEquals("Token validation failed", response.getMessage());

            verify(jwtUtil, times(1)).validateToken(TEST_TOKEN);
            verify(userRepository, times(1)).findById(TEST_USER_ID);
        }

        @Test
        @DisplayName("shouldReturnInvalidTokenResponse_whenTokenValidationThrowsException")
        void shouldReturnInvalidTokenResponse_whenTokenValidationThrowsException() {
            // Arrange
            when(jwtUtil.validateToken(TEST_TOKEN)).thenThrow(new RuntimeException("Invalid token format"));

            // Act
            TokenValidationResponseDTO response = authService.validateToken(TEST_TOKEN);

            // Assert
            assertNotNull(response);
            assertFalse(response.getValid());
            assertEquals("Token validation failed", response.getMessage());

            verify(jwtUtil, times(1)).validateToken(TEST_TOKEN);
        }

        @Test
        @DisplayName("shouldExtractTokenDetails_whenValidatingToken")
        void shouldExtractTokenDetails_whenValidatingToken() {
            // Arrange
            when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(true);
            when(jwtUtil.extractUserId(TEST_TOKEN)).thenReturn(TEST_USER_ID);
            when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
            when(jwtUtil.extractRole(TEST_TOKEN)).thenReturn(UserRole.USER);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // Act
            authService.validateToken(TEST_TOKEN);

            // Assert
            verify(jwtUtil, times(1)).extractUserId(TEST_TOKEN);
            verify(jwtUtil, times(1)).extractUsername(TEST_TOKEN);
            verify(jwtUtil, times(1)).extractRole(TEST_TOKEN);
        }

        @Test
        @DisplayName("shouldVerifyUserExistence_whenValidatingToken")
        void shouldVerifyUserExistence_whenValidatingToken() {
            // Arrange
            when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(true);
            when(jwtUtil.extractUserId(TEST_TOKEN)).thenReturn(TEST_USER_ID);
            when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
            when(jwtUtil.extractRole(TEST_TOKEN)).thenReturn(UserRole.USER);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            // Act
            authService.validateToken(TEST_TOKEN);

            // Assert
            verify(userRepository, times(1)).findById(TEST_USER_ID);
        }
    }

}
