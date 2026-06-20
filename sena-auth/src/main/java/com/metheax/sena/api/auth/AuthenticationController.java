package com.metheax.sena.api.auth;

import com.metheax.sena.api.domain.*;
import com.metheax.sena.api.service.MetheaAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;

/**
 * Author : Kuylim Tith
 * Date : 08/08/2020
 */
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuthenticationController {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);
    private static final String GET_ACCESS_TOKEN_URL = "/auth/token";
    private static final String VERIFY_REFRESH_TOKEN = "/auth/refresh/token";
    private static final String REVOKE_ACCESS_TOKEN_URL = "/auth/token/revoke";

    private final MetheaAuthenticationService authenticationService;

    @Inject
    public AuthenticationController(MetheaAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping(value = GET_ACCESS_TOKEN_URL, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccessTokenResponse> generateTokenFromUser(@RequestBody RequestTokenPayload client, HttpServletRequest req) {
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        try {
            Token token = authenticationService.generateTokenFromUser(client, req);
            if (ObjectUtils.isEmpty(token)) {
                accessTokenResponse.setMessage("Invalid username or password.");
                accessTokenResponse.setStatus(HttpStatus.UNAUTHORIZED);

            } else {
                accessTokenResponse.setToken(token);
                accessTokenResponse.setMessage("Access token generated.");
                accessTokenResponse.setStatus(HttpStatus.OK);
            }
        } catch (Exception ex) {
            log.error("=========> Generate access token from user error: ", ex);
            accessTokenResponse.setMessage("Failed to generate access token. Please check system logs.");
            accessTokenResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(accessTokenResponse, accessTokenResponse.getHttpStatus());
    }

    @PostMapping(value = VERIFY_REFRESH_TOKEN, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccessTokenResponse> generateTokenFromRefreshToken(@RequestBody RefreshTokenPayload payload, HttpServletRequest req) {
        log.info("==========> start get access token form refresh token");
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();

        try {
            Token token = authenticationService.generateTokenFromRefreshToken(payload, req);
            if (ObjectUtils.isEmpty(token)) {
                accessTokenResponse.setMessage("Invalid refresh token.");
                accessTokenResponse.setStatus(HttpStatus.UNAUTHORIZED);
            } else {
                accessTokenResponse.setToken(token);
                accessTokenResponse.setMessage("Access token generated.");
                accessTokenResponse.setStatus(HttpStatus.OK);
            }
        } catch (Exception ex) {
            log.error("=========> Generate access token from refresh token error: ", ex);
            accessTokenResponse.setMessage("Failed to generate access token. Please check system logs.");
            accessTokenResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(accessTokenResponse, accessTokenResponse.getHttpStatus());
    }

    @PostMapping(value = REVOKE_ACCESS_TOKEN_URL, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseAPIResponse> revokeAccessToken(@RequestBody RevokeTokenPayload payload,
                                                                 HttpServletRequest request) {
       BaseAPIResponse baseAPIResponse = new BaseAPIResponse();

        try {
            authenticationService.revokeAccessToken(payload, request);
            baseAPIResponse.setMessage("Access token revoked.");
            baseAPIResponse.setStatus(HttpStatus.OK);
        } catch (Exception ex) {
            log.error("=========> revokeAccessToken error: ", ex);
            baseAPIResponse.setMessage("Failed to revoke access token. Please check system logs.");
            baseAPIResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(baseAPIResponse, baseAPIResponse.getHttpStatus());
    }
}
