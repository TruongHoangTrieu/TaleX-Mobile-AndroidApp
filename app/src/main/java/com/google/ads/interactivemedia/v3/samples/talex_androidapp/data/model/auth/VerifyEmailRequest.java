package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class VerifyEmailRequest {
    private String verificationToken;
    private String otpCode;

    public VerifyEmailRequest(String verificationToken, String otpCode) {
        this.verificationToken = verificationToken;
        this.otpCode = otpCode;
    }
}
