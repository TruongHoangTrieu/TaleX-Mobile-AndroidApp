package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model;

public class Comic {
    private String title;       // Tên truyện
    private int imageResource;  // ID của hình ảnh trong thư mục drawable
    private String salePercent; // Phần trăm giảm giá (Ví dụ: "-30%")

    // Hàm khởi tạo (Constructor)
    public Comic(String title, int imageResource, String salePercent) {
        this.title = title;
        this.imageResource = imageResource;
        this.salePercent = salePercent;
    }

    // Các hàm Getter để lấy dữ liệu ra dùng
    public String getTitle() { return title; }
    public int getImageResource() { return imageResource; }
    public String getSalePercent() { return salePercent; }
}