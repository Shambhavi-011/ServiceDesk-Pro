package com.servicedeskpro.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedeskpro.dto.request.LoginRequestDto;
import com.servicedeskpro.dto.request.RefreshTokenRequestDto;
import com.servicedeskpro.dto.request.RegisterRequestDto;
import com.servicedeskpro.entity.Role;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.repository.RefreshTokenRepository;
import com.servicedeskpro.repository.RoleRepository;
import com.servicedeskpro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        if (roleRepository.findByName(RoleName.ROLE_EMPLOYEE).isEmpty()) {
            roleRepository.save(Role.builder().name(RoleName.ROLE_EMPLOYEE).build());
        }
        if (roleRepository.findByName(RoleName.ROLE_SUPPORT_AGENT).isEmpty()) {
            roleRepository.save(Role.builder().name(RoleName.ROLE_SUPPORT_AGENT).build());
        }
        if (roleRepository.findByName(RoleName.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(Role.builder().name(RoleName.ROLE_ADMIN).build());
        }
    }

    @Test
    @DisplayName("POST /api/auth/register - Successfully register new employee")
    void testRegisterSuccess() throws Exception {
        RegisterRequestDto registerDto = RegisterRequestDto.builder()
                .username("alice.tech")
                .email("alice.tech@enterprise.com")
                .password("Password@123")
                .firstName("Alice")
                .lastName("Smith")
                .department("Engineering")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alice.tech"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_EMPLOYEE"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Reject duplicate username")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequestDto firstDto = RegisterRequestDto.builder()
                .username("bob.builder")
                .email("bob1@enterprise.com")
                .password("Password@123")
                .firstName("Bob")
                .lastName("Builder")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstDto)))
                .andExpect(status().isCreated());

        RegisterRequestDto duplicateDto = RegisterRequestDto.builder()
                .username("bob.builder")
                .email("bob2@enterprise.com")
                .password("Password@123")
                .firstName("Bob")
                .lastName("Builder")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username 'bob.builder' is already taken"));
    }

    @Test
    @DisplayName("Complete Auth Flow: Register -> Login -> Access /me -> Refresh Token -> Logout")
    void testCompleteAuthenticationFlow() throws Exception {
        // 1. Register User
        RegisterRequestDto registerDto = RegisterRequestDto.builder()
                .username("carol.security")
                .email("carol.security@enterprise.com")
                .password("SecurePass@2026")
                .firstName("Carol")
                .lastName("Danvers")
                .department("InfoSec")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated());

        // 2. Login
        LoginRequestDto loginDto = LoginRequestDto.builder()
                .usernameOrEmail("carol.security")
                .password("SecurePass@2026")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("data").get("accessToken").asText();
        String refreshToken = loginJson.get("data").get("refreshToken").asText();

        // 3. Access Protected /api/auth/me WITHOUT token -> Expect 401
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // 4. Access Protected /api/auth/me WITH valid Bearer Token -> Expect 200
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("carol.security"))
                .andExpect(jsonPath("$.data.email").value("carol.security@enterprise.com"));

        // 5. Rotate Refresh Token
        RefreshTokenRequestDto refreshDto = RefreshTokenRequestDto.builder()
                .refreshToken(refreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJson.get("data").get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken); // Verify rotation!

        // 6. Logout / Invalidate
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefreshTokenRequestDto.builder().refreshToken(newRefreshToken).build())))
                .andExpect(status().isOk());
    }
}