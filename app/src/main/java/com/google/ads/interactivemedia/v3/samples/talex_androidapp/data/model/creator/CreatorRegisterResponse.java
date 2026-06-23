package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.creator;

import com.google.gson.annotations.SerializedName;

public class CreatorRegisterResponse {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    // Theo Swagger API, trường data ở endpoint này trả về thẳng một String (là kycSessionId)
    @SerializedName("data")
    private String data;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getData() {
        return data;
    }

    // Viết thêm một hàm Helper (Hỗ trợ) để code bên UI gọi cho rõ nghĩa
    public String getKycSessionId() {
        return data;
    }
}
