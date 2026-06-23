package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth.ChangePasswordRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth.LoginResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";

    private EditText edtCurrentPassword, edtNewPassword, edtConfirmPassword;
    private Button btnSubmit;
    private TextView tvTitle, tvSubtitle, txtForgotPassword;
    private boolean hasPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Nhận flag từ AccountFragment
        hasPassword = getIntent().getBooleanExtra("HAS_PASSWORD", true);

        // Ánh xạ
        edtCurrentPassword = findViewById(R.id.edt_current_password);
        edtNewPassword = findViewById(R.id.edt_new_password);
        edtConfirmPassword = findViewById(R.id.edt_confirm_password);
        btnSubmit = findViewById(R.id.btn_submit);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        txtForgotPassword = findViewById(R.id.txt_forgot_password);
        ImageView btnBack = findViewById(R.id.btn_back);

        // Google user chưa có password → ẩn ô mật khẩu hiện tại, đổi title
        if (!hasPassword) {
            edtCurrentPassword.setVisibility(View.GONE);
            tvTitle.setText("Thiết lập mật khẩu");
            tvSubtitle.setText("Thêm mật khẩu để đăng nhập bằng email");
            txtForgotPassword.setVisibility(View.GONE);
        }

        // Nút Back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Quên mật khẩu
        if (txtForgotPassword != null) {
            txtForgotPassword.setOnClickListener(v -> {
                startActivity(new Intent(this, ForgotPasswordActivity.class));
            });
        }

        // Submit
        btnSubmit.setOnClickListener(v -> {
            String currentPassword = edtCurrentPassword.getText().toString().trim();
            String newPassword = edtNewPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            if (hasPassword && TextUtils.isEmpty(currentPassword)) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu hiện tại!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(newPassword) || newPassword.length() < 8) {
                Toast.makeText(this, "Mật khẩu mới phải tối thiểu 8 ký tự!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            executeChangePassword(hasPassword ? currentPassword : null, newPassword, confirmPassword);
        });
    }

    private void executeChangePassword(String currentPassword, String newPassword, String confirmPassword) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("ĐANG XỬ LÝ...");

        String accessToken = getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String authHeader = "Bearer " + accessToken;
        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword, confirmPassword);

        ApiClient.getApiService().changePassword(authHeader, request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("CẬP NHẬT MẬT KHẨU");

                if (response.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                    // Clear session vì BE revoke tất cả token
                    clearSession();
                    Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ChangePasswordActivity.this, "Đổi mật khẩu thất bại! Kiểm tra lại mật khẩu hiện tại.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("CẬP NHẬT MẬT KHẨU");
                Log.e(TAG, "Change password error", t);
                Toast.makeText(ChangePasswordActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getAccessToken() {
        try {
            String masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(
                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                    "TaleXSecurePref", masterKeyAlias, this,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            return securePrefs.getString("ACCESS_TOKEN", null);
        } catch (Exception e) {
            Log.e(TAG, "Error reading token", e);
            return null;
        }
    }

    private void clearSession() {
        try {
            String masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(
                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                    "TaleXSecurePref", masterKeyAlias, this,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            securePrefs.edit().clear().apply();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing session", e);
        }
    }
}

