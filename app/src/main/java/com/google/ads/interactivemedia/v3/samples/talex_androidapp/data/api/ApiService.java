package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.VerifyEmailRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ResendOtpRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.GoogleLoginRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.GoogleLoginResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.CompleteProfileRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ChangePasswordRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ForgotPasswordRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ForgotPasswordResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ResetPasswordRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ProfileResponse;

// --- CÁC MODEL MỚI CHO LUỒNG EKYC SẼ ĐƯỢC TẠO Ở BƯỚC TIẾP THEO ---
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.TermsResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.CreatorRegisterRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.CreatorRegisterResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.EKycResultResponse;
import com.google.gson.JsonElement;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {
    @POST("api/auth/login")
    Call<LoginResponse> loginUser(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("api/auth/verify-email")
    Call<LoginResponse> verifyEmail(@Body VerifyEmailRequest request);

    @POST("api/auth/logout")
    Call<LogoutResponse> logoutUser(@Body LogoutRequest request);

    @POST("api/auth/resend-otp")
    Call<RegisterResponse> resendOtp(@Body ResendOtpRequest request);

    @POST("api/auth/google")
    Call<GoogleLoginResponse> googleLogin(@Body GoogleLoginRequest request);

    @POST("api/auth/complete-profile")
    Call<LoginResponse> completeProfile(@Body CompleteProfileRequest request);

    @POST("api/auth/change-password")
    Call<LoginResponse> changePassword(@Header("Authorization") String token, @Body ChangePasswordRequest request);

    @POST("api/auth/forgot-password")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/reset-password")
    Call<LoginResponse> resetPassword(@Body ResetPasswordRequest request);

    @GET("api/auth/me")
    Call<ProfileResponse> getCurrentProfile(@Header("Authorization") String token);

    // =========================================================================
    // LUỒNG ĐIỀU KHOẢN (TERMS) & ĐĂNG KÝ CREATOR
    // =========================================================================

    @GET("api/v1/terms-versions/active/{type}")
    Call<TermsResponse> getActiveTerms(
            @Header("Authorization") String token,
            @Path("type") String type // type = "CREATOR"
    );

    @POST("api/v1/creators")
    Call<CreatorRegisterResponse> registerCreator(
            @Header("Authorization") String token,
            @Body CreatorRegisterRequest request // Truyền termsId vào đây
    );

    // =========================================================================
    // LUỒNG EKYC (CAMERA & MULTIPART UPLOAD)
    // =========================================================================

    @Multipart
    @POST("api/v1/kyc-sessions/{kycSessionId}/id-card/front-image")
    Call<EKycResultResponse> uploadFrontId(
            @Header("Authorization") String token,
            @Path("kycSessionId") String kycSessionId,
            @Part MultipartBody.Part frontImage
    );
    @GET("api/v1/creators/identities/own")
    Call<JsonElement> getCreatorIdentities(@Header("Authorization") String token);
    @Multipart
    @POST("api/v1/kyc-sessions/{kycSessionId}/id-card/back-image")
    Call<EKycResultResponse> uploadBackId(
            @Header("Authorization") String token,
            @Path("kycSessionId") String kycSessionId,
            @Part MultipartBody.Part backImage
    );

    @Multipart
    @POST("api/v1/kyc-sessions/{kycSessionId}/liveness")
    Call<JsonElement> verifyLiveness(
            @Header("Authorization") String token,
            @Path("kycSessionId") String kycSessionId,
            @Part MultipartBody.Part video, // File video khuôn mặt
            @Part MultipartBody.Part cmnd   // File ảnh mặt trước CCCD
    );
}
