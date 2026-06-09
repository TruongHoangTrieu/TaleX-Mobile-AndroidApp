package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// 1. ĐÃ ĐƯA CÁC DÒNG IMPORT THƯ VIỆN MÃ HÓA LÊN ĐỈNH FILE CHUẨN BÀI
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.VerifyEmailRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.MainActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText edtOtpCode;
    private Button btnVerify;
    private String verificationToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        // Lấy token được gửi từ màn hình Register sang
        verificationToken = getIntent().getStringExtra("VERIFICATION_TOKEN");

        edtOtpCode = findViewById(R.id.edt_otp_code);
        btnVerify = findViewById(R.id.btn_otp_verify);
        ImageView btnBack = findViewById(R.id.btn_otp_back);

        btnBack.setOnClickListener(v -> finish());

        btnVerify.setOnClickListener(v -> {
            String otp = edtOtpCode.getText().toString().trim();
            if (!TextUtils.isEmpty(otp)) {
                executeVerifyEmailApi(verificationToken, otp);
            } else {
                Toast.makeText(this, "Vui lòng nhập mã OTP!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executeVerifyEmailApi(String token, String otp) {
        btnVerify.setEnabled(false);
        btnVerify.setText("ĐANG KIỂM TRA...");

        VerifyEmailRequest request = new VerifyEmailRequest(token, otp);

        ApiClient.getApiService().verifyEmail(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnVerify.setEnabled(true);
                btnVerify.setText("XÁC MINH NGAY");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {

                        // Gọi hàm lưu Token mã hóa an toàn bảo mật
                        saveTokens(loginResponse.getData().getAccessToken(), loginResponse.getData().getRefreshToken());

                        Toast.makeText(VerifyOtpActivity.this, "Kích hoạt tài khoản thành công!", Toast.LENGTH_LONG).show();

                        // Vào thẳng MainActivity
                        Intent intent = new Intent(VerifyOtpActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(VerifyOtpActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(VerifyOtpActivity.this, "Mã OTP không đúng hoặc đã hết hạn!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                btnVerify.setText("XÁC MINH NGAY");
                Toast.makeText(VerifyOtpActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 2. CHỈ GIỮ LẠI MỘT HÀM MÃ HÓA BẢO MẬT XỊN NÀY BÊN TRONG CLASS
    private void saveTokens(String access, String refresh) {
        try {
            // Tạo hoặc lấy Khóa chủ (Master Key) dùng để mã hóa từ Android Keystore
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            // Khởi tạo EncryptedSharedPreferences thay vì SharedPreferences thường
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "TaleXSecurePref", // Tên file mã hóa riêng biệt
                    masterKeyAlias,
                    this, // Context
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, // Mã hóa Key
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM // Mã hóa Value
            );

            // Ghi dữ liệu dạng mật mã bảo mật cấp độ cao
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("ACCESS_TOKEN", access);
            editor.putString("REFRESH_TOKEN", refresh);
            editor.apply();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi bảo mật khi lưu Token!", Toast.LENGTH_SHORT).show();
        }
    }
} // ◄ Đóng ngoặc chuẩn xác kết thúc toàn bộ Class