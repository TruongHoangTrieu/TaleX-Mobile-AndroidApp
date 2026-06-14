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
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ForgotPasswordRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ForgotPasswordResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText edtEmail;
    private Button btnSendOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtEmail = findViewById(R.id.edt_email);
        btnSendOtp = findViewById(R.id.btn_send_otp);
        ImageView btnBack = findViewById(R.id.btn_back);
        TextView txtBackLogin = findViewById(R.id.txt_back_login);

        // Nút Back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Quay lại Login
        if (txtBackLogin != null) {
            txtBackLogin.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Gửi OTP
        btnSendOtp.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim().toLowerCase();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
                return;
            }

            executeForgotPassword(email);
        });
    }

    private void executeForgotPassword(String email) {
        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("ĐANG GỬI...");

        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        ApiClient.getApiService().forgotPassword(request).enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(Call<ForgotPasswordResponse> call, Response<ForgotPasswordResponse> response) {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("GỬI MÃ OTP");

                if (response.isSuccessful() && response.body() != null) {
                    ForgotPasswordResponse forgotResponse = response.body();

                    if (forgotResponse.isSuccess() && forgotResponse.getData() != null) {
                        String verificationToken = forgotResponse.getData();

                        Toast.makeText(ForgotPasswordActivity.this,
                                "Nếu email tồn tại, mã OTP đã được gửi!", Toast.LENGTH_LONG).show();

                        // Navigate to ResetPasswordActivity
                        Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("VERIFICATION_TOKEN", verificationToken);
                        intent.putExtra("EMAIL", email);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                forgotResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Không thể gửi mã OTP. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ForgotPasswordResponse> call, Throwable t) {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("GỬI MÃ OTP");
                Log.e(TAG, "Forgot password error", t);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
