package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class EKycResultResponse {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private EKycResultData data;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public EKycResultData getData() {
        return data;
    }

    public static class EKycResultData {

        @SerializedName("isSuccess")
        private boolean isSuccess;

        @SerializedName("message")
        private String message;

        // Dùng JsonObject để chứa cục raw JSON từ FPT.AI trả về.
        // Mobile thường không cần đọc sâu vào cục này trừ khi muốn hiện chi tiết lỗi cho user.
        @SerializedName("rawResponse")
        private JsonObject rawResponse;

        public boolean isSuccess() {
            return isSuccess;
        }

        public String getMessage() {
            return message;
        }

        public JsonObject getRawResponse() {
            return rawResponse;
        }
    }
}