package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.util.Calendar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName, edtUsername, edtEmail, edtPhone, edtDob, edtPassword, edtConfirmPassword;
    private String dobApi = "";
    private Button btnRegisterSubmit;
    private TextView txtGotoLogin;
    private ImageView btnBack;
    private ImageView imgRegisterEye1, imgRegisterEye2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFullName = findViewById(R.id.edt_register_fullname);
        edtUsername = findViewById(R.id.edt_register_username);
        edtEmail = findViewById(R.id.edt_register_email);
        edtPhone = findViewById(R.id.edt_register_phone);
        edtDob = findViewById(R.id.edt_register_dob);
        edtDob.setFocusable(false);
        edtDob.setClickable(true);
        edtDob.setOnClickListener(v -> showDatePicker());
        edtPassword = findViewById(R.id.edt_register_password);
        edtConfirmPassword = findViewById(R.id.edt_register_confirm_password);

        btnRegisterSubmit = findViewById(R.id.btn_register_submit);
        txtGotoLogin = findViewById(R.id.txt_goto_login);
        btnBack = findViewById(R.id.btn_register_back);
        imgRegisterEye1 = findViewById(R.id.img_register_eye1);
        imgRegisterEye2 = findViewById(R.id.img_register_eye2);

        if (btnRegisterSubmit != null) {
            btnRegisterSubmit.setOnClickListener(v -> {
                String fullName = edtFullName.getText().toString().trim();
                String username = edtUsername.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();
                String dob = dobApi;
                String pass = edtPassword.getText().toString().trim();
                String confirmPass = edtConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(username) ||
                        TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) ||
                        TextUtils.isEmpty(dob) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(confirmPass)) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin, không được bỏ trống!", Toast.LENGTH_SHORT).show();
                } else if (!pass.equals(confirmPass)) {
                    Toast.makeText(this, "Mật khẩu xác nhận không trùng khớp!", Toast.LENGTH_SHORT).show();
                } else {
                    executeRegisterApi(username, email, pass, fullName, dob, phone);
                }
            });
        }

        if (txtGotoLogin != null) {
            txtGotoLogin.setOnClickListener(v -> {
                // Chủ động mở màn hình Đăng nhập
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                // Cờ này giúp dọn dẹp các Activity trùng lặp nếu có
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Đóng màn hình đăng ký lại
            });

            String customText = "Đã có tài khoản? <font color='#A52A2A'><b>Đăng nhập</b></font>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                txtGotoLogin.setText(android.text.Html.fromHtml(customText, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                txtGotoLogin.setText(android.text.Html.fromHtml(customText));
            }
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupPasswordVisibilityToggles();
    }

    private void executeRegisterApi(String username, String email, String password, String fullName, String dob, String phone) {
        btnRegisterSubmit.setEnabled(false);
        btnRegisterSubmit.setText("ĐANG XỬ LÝ...");

        RegisterRequest request = new RegisterRequest(username, email, password, fullName, dob, phone);

        ApiClient.getApiService().registerUser(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                btnRegisterSubmit.setEnabled(true);
                btnRegisterSubmit.setText("ĐĂNG KÝ");

                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse regResponse = response.body();
                    if (regResponse.isSuccess() && regResponse.getData() != null) {

                        // 📍 ĐÃ SỬA TẠI ĐÂY: Chuyển hẳn sang màn hình VerifyOtpActivity kèm Token
                        String verificationToken = regResponse.getData();

                        Intent intent = new Intent(
                                RegisterActivity.this,
                                VerifyOtpActivity.class
                        );

                        intent.putExtra(
                                "VERIFICATION_TOKEN",
                                verificationToken
                        );

// Truyền email sang màn hình OTP
                        intent.putExtra(
                                "EMAIL",
                                email
                        );

                        startActivity(intent);

                    } else {
                        Toast.makeText(RegisterActivity.this, regResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thất bại! Email hoặc Username đã tồn tại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                btnRegisterSubmit.setEnabled(true);
                btnRegisterSubmit.setText("ĐĂNG KÝ");
                Toast.makeText(RegisterActivity.this, "Kết nối thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPasswordVisibilityToggles() {
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
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }

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
                edtConfirmPassword.setSelection(edtConfirmPassword.getText().length());
            });
        }
    }
    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {

                    // Hiển thị trên giao diện
                    String displayDate = String.format(
                            "%02d-%02d-%04d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                    );

                    edtDob.setText(displayDate);

                    // Format gửi API
                    dobApi = String.format(
                            "%04d-%02d-%02d",
                            selectedYear,
                            selectedMonth + 1,
                            selectedDay
                    );
                },
                year,
                month,
                day
        );

        // Không cho chọn ngày tương lai
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();

    }
}