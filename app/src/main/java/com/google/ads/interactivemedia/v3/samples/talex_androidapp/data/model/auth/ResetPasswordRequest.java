package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class ResetPasswordRequest {
    private String verificationToken;
    private String otpCode;
    private String newPassword;
    private String confirmPassword;

    public ResetPasswordRequest(String verificationToken, String otpCode, String newPassword, String confirmPassword) {
        this.verificationToken = verificationToken;
        this.otpCode = otpCode;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}

