package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Intent;
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

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLoginSubmit;
    private TextView  txtGotoRegister;
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

        // 2. Xử lý sự kiện Đăng nhập
        if (btnLoginSubmit != null) {
            btnLoginSubmit.setOnClickListener(v -> {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ tài khoản & mật khẩu!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                }
            });
        }


        // 4. Các nút Đăng nhập Mạng xã hội
        if (cardFb != null) cardFb.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Facebook...", Toast.LENGTH_SHORT).show());
        if (cardGg != null) cardGg.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Google...", Toast.LENGTH_SHORT).show());
        if (cardApple != null) cardApple.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Apple...", Toast.LENGTH_SHORT).show());

        // 5. Nút Back trên thanh công cụ: điều hướng về sảnh chính MainActivity
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // 6. Chuyển sang trang Đăng Ký (RegisterActivity) và nhuộm sắc Đỏ Đô (#A52A2A)
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

        // 7. Logic ẩn/hiện mật khẩu bằng con mắt
        final boolean[] isPasswordVisible = {false};
        ImageView imgEye = findViewById(R.id.img_eye);

        if (imgEye != null && edtPassword != null) {
            imgEye.setOnClickListener(v -> {
                if (isPasswordVisible[0]) {
                    // Nếu đang hiện -> Bấm vào sẽ ẨN ĐI
                    edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgEye.setImageResource(R.drawable.ic_eye_close);
                    isPasswordVisible[0] = false;
                } else {
                    // Nếu đang ẩn -> Bấm vào sẽ HIỆN LÊN
                    edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgEye.setImageResource(R.drawable.ic_eye_open);
                    isPasswordVisible[0] = true;
                }
                // Đưa con trỏ chuột về cuối dòng chữ
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }
    }

    // Hàm điều hướng phụ trợ chuyển an toàn sang MainActivity
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}