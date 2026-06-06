// java
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
    private TextView btnSkipLogin;
    private CardView cardFb, cardGg, cardApple;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnLoginSubmit = findViewById(R.id.btn_login_submit);
        btnSkipLogin = findViewById(R.id.btn_skip_login);
        cardFb = findViewById(R.id.card_social_fb);
        cardGg = findViewById(R.id.card_social_gg);
        cardApple = findViewById(R.id.card_social_apple);
        btnBack = findViewById(R.id.btn_login_back);

        // Đăng nhập
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

        // Bỏ qua
        if (btnSkipLogin != null) {
            btnSkipLogin.setOnClickListener(v -> navigateToMain());
        }

        // Mạng xã hội
        if (cardFb != null) cardFb.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Facebook...", Toast.LENGTH_SHORT).show());
        if (cardGg != null) cardGg.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Google...", Toast.LENGTH_SHORT).show());
        if (cardApple != null) cardApple.setOnClickListener(v -> Toast.makeText(this, "Đăng nhập bằng Apple...", Toast.LENGTH_SHORT).show());

        // Back button: navigate to home (MainActivity)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    // Khi ấn nút Back của máy: cũng đưa về MainActivity
//    @Override
//    public void onBackPressed() {
//        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        startActivity(intent);
//        finish();
//    }

    // Chuyển an toàn sang MainActivity
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
