package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.MainActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLoginSubmit;
    private TextView txtGotoRegister;
    private CardView cardGg; // Đã xóa cardFb, cardApple
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ thành phần giao diện
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnLoginSubmit = findViewById(R.id.btn_login_submit);

        cardGg = findViewById(R.id.card_social_gg); // Chỉ giữ lại Google
        btnBack = findViewById(R.id.btn_login_back);
        txtGotoRegister = findViewById(R.id.txt_goto_register);

        // 2. Xử lý sự kiện Đăng nhập Gọi API thực tế
        if (btnLoginSubmit != null) {
            btnLoginSubmit.setOnClickListener(v -> {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ tài khoản & mật khẩu!", Toast.LENGTH_SHORT).show();
                } else {
                    // Thực hiện gửi API lên Backend
                    executeLoginApi(email, password);
                }
            });
        }

        // 4. Chỉ giữ lại nút Đăng nhập bằng Google
        if (cardGg != null) {
            cardGg.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Google...", Toast.LENGTH_SHORT).show());
        }

        // 5. Nút Back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // 6. Chuyển sang trang Đăng Ký
        if (txtGotoRegister != null) {
            String customText = "Chưa có tài khoản? <font color='#A52A2A'><b>Đăng ký</b></font>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                txtGotoRegister.setText(android.text.Html.fromHtml(customText, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                txtGotoRegister.setText(android.text.Html.fromHtml(customText));
            }

            txtGotoRegister.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }

        // 7. Logic ẩn/hiện mật khẩu
        final boolean[] isPasswordVisible = {false};
        ImageView imgEye = findViewById(R.id.img_eye);

        if (imgEye != null && edtPassword != null) {
            imgEye.setOnClickListener(v -> {
                if (isPasswordVisible[0]) {
                    edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgEye.setImageResource(R.drawable.ic_eye_close);
                    isPasswordVisible[0] = false;
                } else {
                    edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgEye.setImageResource(R.drawable.ic_eye_open);
                    isPasswordVisible[0] = true;
                }
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }
    }

    // 🆕 HÀM GỌI KẾT NỐI API ĐĂNG NHẬP
    private void executeLoginApi(String email, String password) {
        btnLoginSubmit.setEnabled(false);
        btnLoginSubmit.setText("ĐANG XỬ LÝ...");

        LoginRequest loginRequest = new LoginRequest(email, password);

        ApiClient.getApiService().loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLoginSubmit.setEnabled(true);
                btnLoginSubmit.setText("ĐĂNG NHẬP");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {
                        String accessToken = loginResponse.getData().getAccessToken();
                        String refreshToken = loginResponse.getData().getRefreshToken();

                        try {
                            String masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(
                                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                            );

                            SharedPreferences securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                                    "TaleXSecurePref",
                                    masterKeyAlias,
                                    LoginActivity.this,
                                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                            );

                            SharedPreferences.Editor editor = securePrefs.edit();
                            editor.putString("ACCESS_TOKEN", accessToken);
                            editor.putString("REFRESH_TOKEN", refreshToken);
                            editor.apply();

                        } catch (java.security.GeneralSecurityException | java.io.IOException e) {
                            e.printStackTrace();
                            Toast.makeText(LoginActivity.this, "Lỗi mã hóa dữ liệu đăng nhập!", Toast.LENGTH_SHORT).show();
                        }

                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLoginSubmit.setEnabled(true);
                btnLoginSubmit.setText("ĐĂNG NHẬP");
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}