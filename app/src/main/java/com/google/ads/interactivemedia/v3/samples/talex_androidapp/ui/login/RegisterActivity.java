package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegisterSubmit;
    private TextView txtGotoLogin;
    private ImageView btnBack;
    private ImageView imgRegisterEye1, imgRegisterEye2; // ◄ Đã khai báo thêm biến cho 2 con mắt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ thành phần từ XML sang Java
        edtEmail = findViewById(R.id.edt_register_email);
        edtPassword = findViewById(R.id.edt_register_password);
        edtConfirmPassword = findViewById(R.id.edt_register_confirm_password);
        btnRegisterSubmit = findViewById(R.id.btn_register_submit);
        txtGotoLogin = findViewById(R.id.txt_goto_login);
        btnBack = findViewById(R.id.btn_register_back);
        imgRegisterEye1 = findViewById(R.id.img_register_eye1); // ◄ Ánh xạ mắt 1
        imgRegisterEye2 = findViewById(R.id.img_register_eye2); // ◄ Ánh xạ mắt 2

        // Gán hình mắt nhắm mặc định ban đầu cho cả 2 ô
        if (imgRegisterEye1 != null) imgRegisterEye1.setImageResource(R.drawable.ic_eye_close);
        if (imgRegisterEye2 != null) imgRegisterEye2.setImageResource(R.drawable.ic_eye_close);

        // Xử lý nút Đăng Ký
        if (btnRegisterSubmit != null) {
            btnRegisterSubmit.setOnClickListener(v -> {
                String email = edtEmail.getText().toString().trim();
                String pass = edtPassword.getText().toString().trim();
                String confirmPass = edtConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(confirmPass)) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                } else if (!pass.equals(confirmPass)) {
                    Toast.makeText(this, "Mật khẩu xác nhận không trùng khớp!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Trở về trang Login
                }
            });
        }

        // Xử lý click chuyển về trang Login & Đổi màu chữ Đăng nhập bằng HTML
        if (txtGotoLogin != null) {
            txtGotoLogin.setOnClickListener(v -> finish());

            // Nhuộm màu đỏ đô (#A52A2A) riêng cho chữ Đăng nhập
            String customText = "Đã có tài khoản? <font color='#A52A2A'><b>Đăng nhập</b></font>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                txtGotoLogin.setText(android.text.Html.fromHtml(customText, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                txtGotoLogin.setText(android.text.Html.fromHtml(customText));
            }
        }

        // Nút back trên đỉnh đầu
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 🆕 1. Logic ẩn/hiện mật khẩu cho Ô MẬT KHẨU 1
        final boolean[] isPasswordVisible1 = {false};
        if (imgRegisterEye1 != null && edtPassword != null) {
            imgRegisterEye1.setOnClickListener(v -> {
                if (isPasswordVisible1[0]) {
                    edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgRegisterEye1.setImageResource(R.drawable.ic_eye_close);
                    isPasswordVisible1[0] = false;
                } else {
                    edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgRegisterEye1.setImageResource(R.drawable.ic_eye_open);
                    isPasswordVisible1[0] = true;
                }
                // Giữ con trỏ ở cuối dòng
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }

        // 🆕 2. Logic ẩn/hiện mật khẩu cho Ô XÁC NHẬN MẬT KHẨU 2
        final boolean[] isPasswordVisible2 = {false};
        if (imgRegisterEye2 != null && edtConfirmPassword != null) {
            imgRegisterEye2.setOnClickListener(v -> {
                if (isPasswordVisible2[0]) {
                    edtConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgRegisterEye2.setImageResource(R.drawable.ic_eye_close);
                    isPasswordVisible2[0] = false;
                } else {
                    edtConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgRegisterEye2.setImageResource(R.drawable.ic_eye_open);
                    isPasswordVisible2[0] = true;
                }
                // Giữ con trỏ ở cuối dòng
                edtConfirmPassword.setSelection(edtConfirmPassword.getText().length());
            });
        }
    }
}