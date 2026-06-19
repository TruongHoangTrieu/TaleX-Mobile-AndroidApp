package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.BuildConfig;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RefreshTokenRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RefreshTokenResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login.LoginActivity;

import java.io.IOException;
import okhttp3.Authenticator;

import java.util.concurrent.TimeUnit; // Thêm thư viện quản lý thời gian

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static Retrofit retrofit = null;
    private static Context appContext = null; // Cần giữ Context tĩnh để xử lý Preference và đá sang Login
    private static final Object REFRESH_LOCK = new Object(); // Lock chống race condition khi refresh token
    private static volatile boolean isRefreshing = false; // Flag tránh nhiều thread cùng refresh

    // Khởi tạo ApiClient và truyền Context từ Application class hoặc MainActivity của bạn vào
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

            if (BuildConfig.DEBUG) {
                logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            } else {
                logging.setLevel(HttpLoggingInterceptor.Level.NONE);
            }

            // Đã nâng cấp: Thêm cấu hình Timeout 60 giây chống đứt kết nối khi upload video Liveness
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)

                    .authenticator(new Authenticator() {
                        @Override
                        public Request authenticate(Route route, Response response) throws IOException {
                            Log.d(TAG, "authenticate: Phát hiện Access Token hết hạn (401). Tiến hành làm mới...");

                            if (responseCount(response) >= 2) {
                                return null;
                            }

                            // Synchronized block: chỉ cho 1 thread refresh tại một thời điểm
                            // Các thread khác đợi rồi dùng token mới đã được thread đầu tiên lưu
                            synchronized (REFRESH_LOCK) {
                                // Kiểm tra lại: thread khác có thể đã refresh xong rồi
                                String currentAccessToken = getStoredAccessToken();
                                String requestToken = response.request().header("Authorization");
                                if (currentAccessToken != null && requestToken != null
                                        && !requestToken.endsWith(currentAccessToken)) {
                                    // Token đã được thread khác refresh → dùng token mới luôn
                                    Log.d(TAG, "authenticate: Token đã được thread khác làm mới. Dùng token mới.");
                                    return response.request().newBuilder()
                                            .header("Authorization", "Bearer " + currentAccessToken)
                                            .build();
                                }

                                String currentRefreshToken = getStoredRefreshToken();
                                if (currentRefreshToken == null || currentRefreshToken.isEmpty()) {
                                    handleForceLogout();
                                    return null;
                                }

                                Retrofit rawRetrofit = new Retrofit.Builder()
                                        .baseUrl(BuildConfig.BASE_URL)
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build();
                                ApiService serviceToRefresh = rawRetrofit.create(ApiService.class);

                                retrofit2.Response<RefreshTokenResponse> refreshResponse =
                                        serviceToRefresh.refreshAccessToken(new RefreshTokenRequest(currentRefreshToken)).execute();

                                if (refreshResponse.isSuccessful() && refreshResponse.body() != null && refreshResponse.body().isSuccess()) {
                                    RefreshTokenResponse.TokenData tokenData = refreshResponse.body().getData();
                                    if (tokenData != null) {
                                        String newAccessToken = tokenData.getAccessToken();
                                        String newRefreshToken = tokenData.getRefreshToken();

                                        saveNewTokens(newAccessToken, newRefreshToken);
                                        Log.d(TAG, "authenticate: Làm mới Token thành công! Đang thực thi lại Request cũ...");

                                        return response.request().newBuilder()
                                                .header("Authorization", "Bearer " + newAccessToken)
                                                .build();
                                    }
                                }

                                Log.e(TAG, "authenticate: Cả Refresh Token cũng hết hạn. Đăng xuất hệ thống.");
                                handleForceLogout();
                                return null;
                            }
                        }
                    })

                    .connectTimeout(60, TimeUnit.SECONDS) // Thời gian tối đa để mở kết nối tới Server
                    .writeTimeout(60, TimeUnit.SECONDS)   // Thời gian tối đa để đẩy file Video lên Server
                    .readTimeout(60, TimeUnit.SECONDS)    // Thời gian tối đa để đợi FPT.AI phân tích và trả kết quả về

                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    private static int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    // ── HÀM ĐỌC ACCESS TOKEN TỪ BỘ NHỚ MÃ HÓA ──────────────────────
    private static String getStoredAccessToken() {
        if (appContext == null) return null;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            return securePrefs.getString("ACCESS_TOKEN", null);
        } catch (Exception e) {
            return null;
        }
    }

    // ── HÀM ĐỌC REFRESH TOKEN TỪ BỘ NHỚ MÃ HÓA ──────────────────────
    private static String getStoredRefreshToken() {
        if (appContext == null) return null;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            // 🟢 ĐÃ SỬA: Thứ tự tham số chuẩn và giữ nguyên Enum gốc không ép sang String
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            return securePrefs.getString("REFRESH_TOKEN", null);
        } catch (Exception e) {
            return null;
        }
    }

    // ── HÀM LƯU CẶP TOKEN MỚI ĐÈ VÀO MÁY ────────────────────────────
    private static void saveNewTokens(String accessToken, String refreshToken) {
        if (appContext == null) return;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            // 🟢 ĐÃ SỬA: Thứ tự tham số chuẩn cho luồng lưu token
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            securePrefs.edit()
                    .putString("ACCESS_TOKEN", accessToken)
                    .putString("REFRESH_TOKEN", refreshToken)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── HÀM ĐÁ USER VỀ MÀN HÌNH ĐĂNG NHẬP KHI PHIÊN HẾT HẠN HOÀN TOÀN ──
    private static void handleForceLogout() {
        if (appContext == null) return;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            // 🟢 ĐÃ SỬA: Thứ tự tham số chuẩn cho luồng xoá sạch bộ nhớ khi logout
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            securePrefs.edit().clear().apply();

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(appContext, "Phiên đăng nhập đã hết hạn! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(appContext, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                appContext.startActivity(intent);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}