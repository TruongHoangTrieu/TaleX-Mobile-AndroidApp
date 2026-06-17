package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model;

public class UpdateProfileRequest {
    private String username;
    private String fullName;
    private String phone;
    private String dateOfBirth;
    private String avatarUrl;

    public UpdateProfileRequest(String username, String fullName, String phone, String dateOfBirth, String avatarUrl) {
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.avatarUrl = avatarUrl;
    }
}