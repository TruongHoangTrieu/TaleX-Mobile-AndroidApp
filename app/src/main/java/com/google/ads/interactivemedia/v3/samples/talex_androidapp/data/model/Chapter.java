package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model;

import java.util.List;

public class Chapter {
    private int seasonNumber;          // Thuộc mùa mấy (1, 2, 3)
    private String chapterName;        // Tên chương (Ví dụ: "Chương 01: Sóng gió gia tộc")
    private List<Episode> episodeList; // 🌟 Danh sách các tập nằm trong chương này

    public Chapter(int seasonNumber, String chapterName, List<Episode> episodeList) {
        this.seasonNumber = seasonNumber;
        this.chapterName = chapterName;
        this.episodeList = episodeList;
    }

    public int getSeasonNumber() { return seasonNumber; }
    public String getChapterName() { return chapterName; }
    public List<Episode> getEpisodeList() { return episodeList; }
}