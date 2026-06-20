package com.metheax.sena.api.auth;

import tools.jackson.databind.ObjectMapper;
import com.metheax.sena.api.domain.RefreshTokenPayload;
import com.metheax.sena.api.domain.RequestTokenPayload;
import com.metheax.sena.api.domain.RevokeTokenPayload;
import com.metheax.sena.api.domain.Token;
import com.metheax.sena.api.service.MetheaAuthenticationService;
import com.metheax.sena.exception.InvalidClientSecretException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private MetheaAuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    // ---- POST /auth/token ----

    @Test
    void generateTokenFromUser_validCredentials_returns200WithToken() throws Exception {
        Token token = new Token();
        token.setAccessToken("access-token-value");
        token.setRefreshToken("refresh-token-value");
        token.setTokenType("Bearer ");
        token.setExpiredIn("1234567890");

        when(authenticationService.generateTokenFromUser(any(RequestTokenPayload.class), any()))
                .thenReturn(token);

        RequestTokenPayload payload = new RequestTokenPayload();
        payload.setUsername("user");
        payload.setPassword("password");

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Access token generated."))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void generateTokenFromUser_invalidCredentials_returns400() throws Exception {
        when(authenticationService.generateTokenFromUser(any(RequestTokenPayload.class), any()))
                .thenReturn(null);

        RequestTokenPayload payload = new RequestTokenPayload();
        payload.setUsername("user");
        payload.setPassword("wrong");

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void generateTokenFromUser_serviceThrowsException_returnsEmptyMap() throws Exception {
        when(authenticationService.generateTokenFromUser(any(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        RequestTokenPayload payload = new RequestTokenPayload();
        payload.setUsername("user");
        payload.setPassword("pass");

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError());
    }

    // ---- POST /auth/refresh/token ----

    @Test
    void generateTokenFromRefreshToken_validToken_returns200WithToken() throws Exception {
        Token token = new Token();
        token.setAccessToken("new-access-token");

        when(authenticationService.generateTokenFromRefreshToken(any(RefreshTokenPayload.class), any()))
                .thenReturn(token);

        RefreshTokenPayload payload = new RefreshTokenPayload();
        payload.setRefreshToken("valid-refresh-token");

        mockMvc.perform(post("/auth/refresh/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void generateTokenFromRefreshToken_invalidToken_returns400() throws Exception {
        when(authenticationService.generateTokenFromRefreshToken(any(RefreshTokenPayload.class), any()))
                .thenReturn(null);

        RefreshTokenPayload payload = new RefreshTokenPayload();
        payload.setRefreshToken("expired-or-invalid-token");

        mockMvc.perform(post("/auth/refresh/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    // ---- POST /auth/token/revoke ----

    @Test
    void revokeAccessToken_success_returns200() throws Exception {
        doNothing().when(authenticationService).revokeAccessToken(any(RevokeTokenPayload.class), any());

        RevokeTokenPayload payload = new RevokeTokenPayload();
        payload.setUsername("user");
        payload.setToken("some-access-token");

        mockMvc.perform(post("/auth/token/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Access token revoked."));
    }

    @Test
    void revokeAccessToken_invalidClientSecret_returns400() throws Exception {
        doThrow(new InvalidClientSecretException("Provided client secret invalid!"))
                .when(authenticationService).revokeAccessToken(any(RevokeTokenPayload.class), any());

        RevokeTokenPayload payload = new RevokeTokenPayload();
        payload.setUsername("wronguser");
        payload.setToken("some-access-token");

        mockMvc.perform(post("/auth/token/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Failed to revoke access token. Please check system logs."));
    }
}
