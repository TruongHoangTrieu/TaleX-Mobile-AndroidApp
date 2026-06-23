package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class EKycResultResponse {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private EKycData data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EKycData getData() {
        return data;
    }

    public void setData(EKycData data) {
        this.data = data;
    }

    // Class nội bộ đại diện cho object "data"
    public static class EKycData {

        @SerializedName("isSuccess")
        private boolean isSuccess;

        @SerializedName("message")
        private String message;

        // ĐÃ SỬA LỖI: Dùng JsonElement để chấp nhận mọi định dạng (kể cả null) từ Backend trả về
        @SerializedName("rawResponse")
        private JsonElement rawResponse;

        public boolean isSuccess() {
            return isSuccess;
        }

        public void setSuccess(boolean success) {
            isSuccess = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public JsonElement getRawResponse() {
            return rawResponse;
        }

        public void setRawResponse(JsonElement rawResponse) {
            this.rawResponse = rawResponse;
        }
    }
}