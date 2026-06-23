package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie;

public class Movie {
    private String title;
    private int posterResource;
    private String badgeText;

    public Movie(String title, int posterResource, String badgeText) {
        this.title = title;
        this.posterResource = posterResource;
        this.badgeText = badgeText;
    }

    public String getTitle() { return title; }
    public int getPosterResource() { return posterResource; }
    public String getBadgeText() { return badgeText; }
}
