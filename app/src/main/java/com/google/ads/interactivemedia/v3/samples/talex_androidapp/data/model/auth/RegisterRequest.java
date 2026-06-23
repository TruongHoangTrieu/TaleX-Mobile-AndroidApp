package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String dateOfBirth;
    private String phone;

    public RegisterRequest(String username, String email, String password, String fullName, String dateOfBirth, String phone) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
    }
}
