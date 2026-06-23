package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class GoogleLoginResponse {
    private boolean success;
    private String message;
    private GoogleLoginData data;
    private String timestamp;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public GoogleLoginData getData() { return data; }

    public static class GoogleLoginData {
        private String status;
        private String accessToken;
        private String refreshToken;
        private String verificationToken;

        public String getStatus() { return status; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getVerificationToken() { return verificationToken; }
    }
}

