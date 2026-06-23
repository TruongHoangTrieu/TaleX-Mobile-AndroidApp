package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class ForgotPasswordResponse {
    private boolean success;
    private String message;
    private String data; // verificationToken
    private String timestamp;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getData() { return data; }
}

