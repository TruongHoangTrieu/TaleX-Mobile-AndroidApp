package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.account; // 🟢 Đã sửa: Xóa dấu chấm thừa ở cuối package

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ProfileResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProfileDetailActivity";
    private ProgressBar pbLoading;
    private ScrollView scrollContent;
    private TextView tvFullname, tvUsername, tvEmail, tvPhone, tvDob, tvGoogleStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_detail);

        // 1. Ánh xạ View
        pbLoading = findViewById(R.id.pb_profile_detail_loading);
        scrollContent = findViewById(R.id.scroll_profile_content);
        tvFullname = findViewById(R.id.tv_detail_fullname);
        tvUsername = findViewById(R.id.tv_detail_username);
        tvEmail = findViewById(R.id.tv_detail_email);
        tvPhone = findViewById(R.id.tv_detail_phone);
        tvDob = findViewById(R.id.tv_detail_dob);
        tvGoogleStatus = findViewById(R.id.tv_detail_google_status);
        ImageView btnBack = findViewById(R.id.btn_profile_back);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 2. Lấy Access Token từ SharedPreferences bảo mật để gọi API
        String token = getAccessToken();
        if (token != null) {
            loadUserProfileData("Bearer " + token);
        } else {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Phiên đăng nhập hết hạn! Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ── Hàm gọi API nạp thông tin hồ sơ ──────────────────────────────
    private void loadUserProfileData(String authToken) {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (scrollContent != null) scrollContent.setVisibility(View.GONE);

        ApiClient.getApiService().getCurrentProfile(authToken).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                if (scrollContent != null) scrollContent.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profileResponse = response.body();

                    if (profileResponse.isSuccess() && profileResponse.getData() != null) {
                        // 🔥 Đã sửa: Chuyển thành ProfileResponse.UserData để khớp với file Model của bạn
                        ProfileResponse.UserData data = profileResponse.getData();

                        if (tvFullname != null) tvFullname.setText(data.getFullName());
                        if (tvUsername != null) tvUsername.setText(data.getUsername());
                        if (tvEmail != null) tvEmail.setText(data.getEmail());

                        if (tvPhone != null) {
                            tvPhone.setText(android.text.TextUtils.isEmpty(data.getPhone()) ? "Chưa cập nhật" : data.getPhone());
                        }

                        if (tvDob != null) tvDob.setText(data.getDateOfBirth());

                        if (tvGoogleStatus != null) {
                            boolean isGoogleLinked = data.isGoogleLinked();
                            tvGoogleStatus.setText(isGoogleLinked ? "Đã liên kết ✓" : "Chưa liên kết");
                            tvGoogleStatus.setTextColor(isGoogleLinked ? android.graphics.Color.parseColor("#D4AF37") : android.graphics.Color.parseColor("#888888"));
                        }
                    } else {
                        Toast.makeText(ProfileDetailActivity.this, profileResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ProfileDetailActivity.this, "Không thể lấy dữ liệu hồ sơ!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                Log.e(TAG, "Profile API Failure", t);
                Toast.makeText(ProfileDetailActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Hàm bổ trợ đọc Token bảo mật ──────────────────────────────────
    private String getAccessToken() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            return securePrefs.getString("ACCESS_TOKEN", null);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error reading secure token", e);
            return null;
        }
    }
}