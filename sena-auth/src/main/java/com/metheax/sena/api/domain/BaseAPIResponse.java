package com.metheax.sena.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseAPIResponse {

    private int status;
    private String message;

    public int getStatus() {
        return status;
    }

    @JsonIgnore
    public HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(status);
    }

    public void setStatus(HttpStatus status) {
        this.status = status.value();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
