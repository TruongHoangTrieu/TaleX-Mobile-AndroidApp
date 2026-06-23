package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.creator;

import com.google.gson.annotations.SerializedName;

public class CreatorRegisterRequest {

    @SerializedName("termsId")
    private String termsId;

    @SerializedName("accountId")
    private String accountId;

    // Constructor chỉ cần termsId (Dùng khi accountId được lấy tự động từ Token ở BE)
    public CreatorRegisterRequest(String termsId) {
        this.termsId = termsId;
    }

    // Constructor đầy đủ (Dùng khi BE bắt buộc truyền accountId từ phía Mobile)
    public CreatorRegisterRequest(String termsId, String accountId) {
        this.termsId = termsId;
        this.accountId = accountId;
    }

    public String getTermsId() {
        return termsId;
    }

    public void setTermsId(String termsId) {
        this.termsId = termsId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
