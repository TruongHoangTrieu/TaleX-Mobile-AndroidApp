package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class TermsResponse {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private TermsData data;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public TermsData getData() {
        return data;
    }

    // Class tĩnh (Static class) đại diện cho object "data" trả về từ BE
    public static class TermsData {
        @SerializedName("id")
        private String id;

        @SerializedName("version")
        private String version;

        @SerializedName("type")
        private String type;

        @SerializedName("content")
        private String content;

        @SerializedName("isActive")
        private boolean isActive;

        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }

        public String getType() {
            return type;
        }

        public String getContent() {
            return content; // Đây là phần text điều khoản ta sẽ in ra màn hình
        }

        public boolean isActive() {
            return isActive;
        }
    }
}