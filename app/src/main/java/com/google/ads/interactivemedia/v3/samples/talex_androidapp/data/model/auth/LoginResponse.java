package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class LoginResponse {
    private boolean success;
    private String message;
    private AuthToken data; // Object chứa 2 token
    private String timestamp;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public AuthToken getData() { return data; }
}
