package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth.LoginResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.auth.ResetPasswordRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";

    private EditText edtOtp, edtNewPassword, edtConfirmPassword;
    private Button btnReset;
    private String verificationToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Nhận data từ ForgotPasswordActivity
        verificationToken = getIntent().getStringExtra("VERIFICATION_TOKEN");
        String email = getIntent().getStringExtra("EMAIL");

        if (TextUtils.isEmpty(verificationToken)) {
            Toast.makeText(this, "Thiếu token xác minh. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Ánh xạ
        edtOtp = findViewById(R.id.edt_otp);
        edtNewPassword = findViewById(R.id.edt_new_password);
        edtConfirmPassword = findViewById(R.id.edt_confirm_password);
        btnReset = findViewById(R.id.btn_reset);
        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvEmailHint = findViewById(R.id.tv_email_hint);

        // Hiện email hint
        if (tvEmailHint != null && email != null) {
            tvEmailHint.setText("Nhập mã OTP đã gửi tới " + email);
        }

        // Nút Back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Submit
        btnReset.setOnClickListener(v -> {
            String otp = edtOtp.getText().toString().trim();
            String newPassword = edtNewPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(otp) || otp.length() != 6) {
                Toast.makeText(this, "Vui lòng nhập mã OTP 6 chữ số!", Toast.LENGTH_SHORT).show();
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

            executeResetPassword(otp, newPassword, confirmPassword);
        });
    }

    private void executeResetPassword(String otp, String newPassword, String confirmPassword) {
        btnReset.setEnabled(false);
        btnReset.setText("ĐANG XỬ LÝ...");

        ResetPasswordRequest request = new ResetPasswordRequest(verificationToken, otp, newPassword, confirmPassword);

        ApiClient.getApiService().resetPassword(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnReset.setEnabled(true);
                btnReset.setText("ĐẶT LẠI MẬT KHẨU");

                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this,
                            "Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();

                    // Navigate to Login
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ResetPasswordActivity.this,
                            "Đặt lại mật khẩu thất bại! Kiểm tra lại mã OTP.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnReset.setEnabled(true);
                btnReset.setText("ĐẶT LẠI MẬT KHẨU");
                Log.e(TAG, "Reset password error", t);
                Toast.makeText(ResetPasswordActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

