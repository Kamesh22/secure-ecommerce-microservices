package com.ecommerce.auth;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional; // Added Import

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.auth.dto.AuthResponseDTO;
import com.ecommerce.auth.dto.LoginRequestDTO;
import com.ecommerce.auth.dto.RegisterRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional //Ensures DB rolls back after each test
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@DisplayName("Auth Service Integration Test Suite")
class AuthServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Test Constants
    private static final String BASE_URL = "/api/auth";
    private static final String REGISTER_ENDPOINT = BASE_URL + "/register";
    private static final String LOGIN_ENDPOINT = BASE_URL + "/login";
    private static final String VALIDATE_ENDPOINT = BASE_URL + "/validate";

    private static final String USER1_USERNAME = "Rajesh_Kumar";
    private static final String USER1_EMAIL = "rajesh.kumar@example.com";
    private static final String USER1_PASSWORD = "SecurePass@123";

    private static final String USER2_USERNAME = "Priya_Singh";
    private static final String USER2_EMAIL = "priya.singh@example.com";
    private static final String USER2_PASSWORD = "AnotherPass@456";

    private RegisterRequestDTO registerRequestDTO;
    private LoginRequestDTO loginRequestDTO;

    @BeforeEach
    void setUp() {
        registerRequestDTO = RegisterRequestDTO.builder()
                .username(USER1_USERNAME)
                .email(USER1_EMAIL)
                .password(USER1_PASSWORD)
                .build();

        loginRequestDTO = LoginRequestDTO.builder()
                .username(USER1_USERNAME)
                .password(USER1_PASSWORD)
                .build();
    }

    @Nested
    @DisplayName("End-to-End Registration and Login Flow")
    class EndToEndFlowTests {

        @Test
        @DisplayName("shouldCompleteFullAuthenticationFlow_registerLoginValidate")
        void shouldCompleteFullAuthenticationFlow_registerLoginValidate() throws Exception {
            // Step 1: Register user
            MvcResult registerResult = mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId", notNullValue()))
                    .andExpect(jsonPath("$.username", equalTo(USER1_USERNAME)))
                    .andExpect(jsonPath("$.email", equalTo(USER1_EMAIL)))
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andReturn();

            String registerResponse = registerResult.getResponse().getContentAsString();
            AuthResponseDTO registerResponseDTO = objectMapper.readValue(registerResponse, AuthResponseDTO.class);
            String registrationToken = registerResponseDTO.getToken();

            // Step 2: Verify token from registration
            mockMvc.perform(get(VALIDATE_ENDPOINT)
                    .header("Authorization", "Bearer " + registrationToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid", equalTo(true)))
                    .andExpect(jsonPath("$.username", equalTo(USER1_USERNAME)));

            // Step 3: Login with same credentials
            MvcResult loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId", notNullValue()))
                    .andExpect(jsonPath("$.username", equalTo(USER1_USERNAME)))
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            AuthResponseDTO loginResponseDTO = objectMapper.readValue(loginResponse, AuthResponseDTO.class);
            String loginToken = loginResponseDTO.getToken();

            // Step 4: Verify login token
            mockMvc.perform(get(VALIDATE_ENDPOINT)
                    .header("Authorization", "Bearer " + loginToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid", equalTo(true)))
                    .andExpect(jsonPath("$.username", equalTo(USER1_USERNAME)));
        }

        @Test
        @DisplayName("shouldRegisterMultipleUsersIndependently")
        void shouldRegisterMultipleUsersIndependently() throws Exception {
            // Register first user
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", equalTo(USER1_USERNAME)));

            // Register second user
            RegisterRequestDTO secondRegister = RegisterRequestDTO.builder()
                    .username(USER2_USERNAME)
                    .email(USER2_EMAIL)
                    .password(USER2_PASSWORD)
                    .build();

            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondRegister)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", equalTo(USER2_USERNAME)));
        }
    }

    @Nested
    @DisplayName("Duplicate User Prevention Tests")
    class DuplicateUserPreventionTests {

        @Test
        @DisplayName("shouldPreventDuplicateUsernameRegistration")
        void shouldPreventDuplicateUsernameRegistration() throws Exception {
            // Register first user
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated());

            // Attempt to register with same username but different email
            RegisterRequestDTO duplicateUsername = RegisterRequestDTO.builder()
                    .username(USER1_USERNAME)
                    .email("different@example.com")
                    .password(USER1_PASSWORD)
                    .build();

            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateUsername)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldPreventDuplicateEmailRegistration")
        void shouldPreventDuplicateEmailRegistration() throws Exception {
            // Register first user
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated());

            // Attempt to register with same email but different username
            RegisterRequestDTO duplicateEmail = RegisterRequestDTO.builder()
                    .username("DifferentUsername")
                    .email(USER1_EMAIL)
                    .password(USER1_PASSWORD)
                    .build();

            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateEmail)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Password Validation and Verification Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("shouldRejectLoginWithWrongPassword")
        void shouldRejectLoginWithWrongPassword() throws Exception {
            // Register user
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated());

            // Attempt login with wrong password
            LoginRequestDTO wrongPasswordLogin = LoginRequestDTO.builder()
                    .username(USER1_USERNAME)
                    .password("WrongPassword@789")
                    .build();

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(wrongPasswordLogin)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldEncodePasswordDuringRegistration")
        void shouldEncodePasswordDuringRegistration() throws Exception {
            // Register user
            MvcResult result = mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            AuthResponseDTO authResponse = objectMapper.readValue(response, AuthResponseDTO.class);

            // Login should succeed with correct password
            LoginRequestDTO correctLogin = LoginRequestDTO.builder()
                    .username(authResponse.getUsername())
                    .password(USER1_PASSWORD)
                    .build();

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(correctLogin)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Token Management and Validation Tests")
    class TokenManagementTests {

        @Test
        @DisplayName("shouldReturnUserRoleInValidationResponse")
        void shouldReturnUserRoleInValidationResponse() throws Exception {
            // Register user
            MvcResult registerResult = mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String registerResponse = registerResult.getResponse().getContentAsString();
            AuthResponseDTO registerResponseDTO = objectMapper.readValue(registerResponse, AuthResponseDTO.class);
            String token = registerResponseDTO.getToken();

            // Validate token and verify role
            mockMvc.perform(get(VALIDATE_ENDPOINT)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", equalTo("USER")));
        }
    }

    @Nested
    @DisplayName("Error Handling and Validation Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("shouldRejectLoginForNonExistentUser")
        void shouldRejectLoginForNonExistentUser() throws Exception {
            // Attempt login without registration
            LoginRequestDTO nonExistentLogin = LoginRequestDTO.builder()
                    .username("NonExistentUser")
                    .password(USER1_PASSWORD)
                    .build();

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nonExistentLogin)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("shouldRejectInvalidEmailDuringRegistration")
        void shouldRejectInvalidEmailDuringRegistration() throws Exception {
            // Attempt registration with invalid email
            RegisterRequestDTO invalidEmail = RegisterRequestDTO.builder()
                    .username("ValidUsername")
                    .email("invalid-email")
                    .password(USER1_PASSWORD)
                    .build();

            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidEmail)))
                    // FIX 2: Validation errors return 422, not 400
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("shouldRejectShortPasswordDuringRegistration")
        void shouldRejectShortPasswordDuringRegistration() throws Exception {
            // Attempt registration with short password
            RegisterRequestDTO shortPassword = RegisterRequestDTO.builder()
                    .username("ValidUsername")
                    .email(USER1_EMAIL)
                    .password("short")
                    .build();

            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shortPassword)))
                    // FIX 2: Validation errors return 422, not 400
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("shouldRejectValidationWithoutAuthorizationHeader")
        void shouldRejectValidationWithoutAuthorizationHeader() throws Exception {
            // Register user
            mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated());

            // Attempt validation without token
            mockMvc.perform(get(VALIDATE_ENDPOINT))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldRejectValidationWithMalformedAuthorizationHeader")
        void shouldRejectValidationWithMalformedAuthorizationHeader() throws Exception {
            // Register user
            MvcResult registerResult = mockMvc.perform(post(REGISTER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequestDTO)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String registerResponse = registerResult.getResponse().getContentAsString();
            AuthResponseDTO registerResponseDTO = objectMapper.readValue(registerResponse, AuthResponseDTO.class);
            String token = registerResponseDTO.getToken();

            // Attempt validation with malformed header (missing "Bearer ")
            mockMvc.perform(get(VALIDATE_ENDPOINT)
                    .header("Authorization", token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldRejectValidationWithInvalidToken")
        void shouldRejectValidationWithInvalidToken() throws Exception {
            // Attempt validation with invalid token
            mockMvc.perform(get(VALIDATE_ENDPOINT)
                    .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized());
        }
    }
}