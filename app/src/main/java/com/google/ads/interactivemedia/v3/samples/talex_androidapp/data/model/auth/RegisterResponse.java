package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class RegisterResponse {
    private boolean success;
    private String message;
    private String data; // Đây chính là chuỗi verificationToken trả về
    private String timestamp;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getData() { return data; }
}
