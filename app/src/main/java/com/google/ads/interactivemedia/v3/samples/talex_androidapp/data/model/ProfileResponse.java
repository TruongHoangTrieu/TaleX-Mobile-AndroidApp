package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model;

public class ProfileResponse {
    private boolean success;
    private String message;
    private UserData data;

    public boolean isSuccess() { return success; }
    public UserData getData() { return data; }
    public String getMessage() { return message; }

    public static class UserData {
        private String username;
        private String email;
        private String fullName;
        private String phone;
        private String dateOfBirth;
        private String avatarUrl;
        private String roleName;
        private boolean hasPassword;
        private boolean googleLinked;

        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getPhone() { return phone; }
        public String getDateOfBirth() { return dateOfBirth; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getRoleName() { return roleName; }
        public boolean isHasPassword() { return hasPassword; }
        public boolean isGoogleLinked() { return googleLinked; }
    }
}