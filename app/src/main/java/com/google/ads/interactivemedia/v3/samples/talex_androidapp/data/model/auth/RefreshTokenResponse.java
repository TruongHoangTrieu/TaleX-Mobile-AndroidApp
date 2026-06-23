package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class RefreshTokenResponse {
    private boolean success;
    private String message;
    private TokenData data;
    private String timestamp;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public TokenData getData() { return data; }

    public static class TokenData {
        private String accessToken;
        private String refreshToken;

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
