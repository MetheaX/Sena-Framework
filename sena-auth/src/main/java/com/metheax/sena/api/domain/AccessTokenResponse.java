package com.metheax.sena.api.domain;

public class AccessTokenResponse extends BaseAPIResponse {
    private Token token;

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }
}
