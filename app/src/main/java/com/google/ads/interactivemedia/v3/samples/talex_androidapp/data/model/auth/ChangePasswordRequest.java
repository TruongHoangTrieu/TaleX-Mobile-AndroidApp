package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    public ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}

