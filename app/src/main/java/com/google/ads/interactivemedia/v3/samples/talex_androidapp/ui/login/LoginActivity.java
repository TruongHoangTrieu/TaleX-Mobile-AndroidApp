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
    private CardView cardFb, cardGg, cardApple;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ thành phần giao diện
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnLoginSubmit = findViewById(R.id.btn_login_submit);

        cardFb = findViewById(R.id.card_social_fb);
        cardGg = findViewById(R.id.card_social_gg);
        cardApple = findViewById(R.id.card_social_apple);
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

        // 4. Các nút Đăng nhập Mạng xã hội
        if (cardFb != null) cardFb.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Facebook...", Toast.LENGTH_SHORT).show());
        if (cardGg != null) cardGg.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Google...", Toast.LENGTH_SHORT).show());
        if (cardApple != null) cardApple.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Apple...", Toast.LENGTH_SHORT).show());

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
        // Vô hiệu hóa tạm thời nút bấm để tránh người dùng nhấn liên tục
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
                        // Đăng nhập thành công -> Lấy Token ra
                        String accessToken = loginResponse.getData().getAccessToken();
                        String refreshToken = loginResponse.getData().getRefreshToken();

                        // Lưu Token vào SharedPreferences của thiết bị một cách an toàn
                        SharedPreferences sharedPref = getSharedPreferences("TaleXPref", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("ACCESS_TOKEN", accessToken);
                        editor.putString("REFRESH_TOKEN", refreshToken);
                        editor.apply();

                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        // Server trả về mã 200 nhưng logic xử lý thất bại (Ví dụ: Sai tài khoản)
                        Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Lỗi từ phía Server (Ví dụ: Mã lỗi 400, 401, 500)
                    Toast.makeText(LoginActivity.this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // Lỗi mất kết nối mạng, rớt mạng hoặc Server Backend bị sập nguồn
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