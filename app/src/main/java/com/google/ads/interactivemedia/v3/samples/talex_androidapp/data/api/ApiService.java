package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterRequest;  // <--- ADD THIS
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterResponse; // <--- ADD THIS
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.VerifyEmailRequest; // <--- ADD THIS (for verifyEmail endpoint)
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ResendOtpRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ProfileResponse;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
public interface ApiService {
    @POST("api/auth/login")
    Call<LoginResponse> loginUser(@Body LoginRequest request);
    @POST("api/auth/register") // ◄ Thêm Router đăng ký
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    @POST("api/auth/verify-email") // ◄ Thêm Router xác thực OTP
    Call<LoginResponse> verifyEmail(@Body VerifyEmailRequest request);

    @POST("api/auth/logout")
    Call<LogoutResponse> logoutUser(@Body LogoutRequest request);

    @POST("api/auth/resend-otp")
    Call<RegisterResponse> resendOtp(
            @Body ResendOtpRequest request
    );
    @GET("api/auth/me")
    Call<ProfileResponse> getCurrentProfile(@Header("Authorization") String token);
}