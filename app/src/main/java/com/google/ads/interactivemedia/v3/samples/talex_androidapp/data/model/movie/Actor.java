package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie;

public class Actor {
    private String name;
    private String role;
    private int avatarResource;

    public Actor(String name, String role, int avatarResource) {
        this.name = name;
        this.role = role;
        this.avatarResource = avatarResource;
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public int getAvatarResource() { return avatarResource; }
}
