package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model;

public class CompleteProfileRequest {
    private String verificationToken;
    private String dateOfBirth;
    private String phone;

    public CompleteProfileRequest(String verificationToken, String dateOfBirth, String phone) {
        this.verificationToken = verificationToken;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
    }
}
