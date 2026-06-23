package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie;

public class Episode {
    private String episodeName; // Ví dụ: "Tập 01: Khởi đầu"
    private String uploadDate;
    private boolean isVip;

    public Episode(String episodeName, String uploadDate, boolean isVip) {
        this.episodeName = episodeName;
        this.uploadDate = uploadDate;
        this.isVip = isVip;
    }

    public String getEpisodeName() { return episodeName; }
    public String getUploadDate() { return uploadDate; }
    public boolean isVip() { return isVip; }
}
