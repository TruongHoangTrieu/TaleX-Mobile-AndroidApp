package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View; // Đã bảo đảm có import View để xử lý VISIBLE/GONE
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar; // Đã thêm import cho vòng xoay tải
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.MainActivity;
import java.util.Calendar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName, edtUsername, edtEmail, edtPhone, edtDob, edtPassword, edtConfirmPassword;
    private String dobApi = "";
    private Button btnRegisterSubmit;
    private ProgressBar pbRegisterLoading; // Thêm biến vòng xoay tải dữ liệu
    private TextView tvErrorMessage;       // Thêm biến dòng chữ báo lỗi thực tế
    private TextView txtGotoLogin;
    private ImageView btnBack;
    private ImageView imgRegisterEye1, imgRegisterEye2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Ánh xạ thành phần giao diện
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
        pbRegisterLoading = findViewById(R.id.pb_register_loading); // Ánh xạ vòng xoay
        tvErrorMessage = findViewById(R.id.tv_register_error_message);   // Ánh xạ dòng lỗi
        txtGotoLogin = findViewById(R.id.txt_goto_login);
        btnBack = findViewById(R.id.btn_register_back);
        imgRegisterEye1 = findViewById(R.id.img_register_eye1);
        imgRegisterEye2 = findViewById(R.id.img_register_eye2);

        // 2. Logic Đăng ký và phân loại chặn rỗng thông minh (Bỏ Toast)
        if (btnRegisterSubmit != null) {
            btnRegisterSubmit.setOnClickListener(v -> {
                String fullName = edtFullName.getText().toString().trim();
                String username = edtUsername.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();
                String dob = dobApi;
                String pass = edtPassword.getText().toString().trim();
                String confirmPass = edtConfirmPassword.getText().toString().trim();

                // 1. Tạo các biến cờ hiệu kiểm tra từng ô một cách độc lập
                boolean isFullnameEmpty = TextUtils.isEmpty(fullName);
                boolean isUsernameEmpty = TextUtils.isEmpty(username);
                boolean isEmailEmpty = TextUtils.isEmpty(email);
                boolean isPhoneEmpty = TextUtils.isEmpty(phone);
                boolean isDobEmpty = TextUtils.isEmpty(dob);
                boolean isPasswordEmpty = TextUtils.isEmpty(pass);
                boolean isConfirmPasswordEmpty = TextUtils.isEmpty(confirmPass);

                // 2. Nếu CÓ ÍT NHẤT một ô bị trống
                if (isFullnameEmpty || isUsernameEmpty || isEmailEmpty || isPhoneEmpty || isDobEmpty || isPasswordEmpty || isConfirmPasswordEmpty) {

                    // Hiện dòng chữ thông báo tổng quát
                    triggerRegisterError("Vui lòng nhập đầy đủ thông tin, không được bỏ trống!",
                            isFullnameEmpty, isUsernameEmpty, isEmailEmpty, isPhoneEmpty, isDobEmpty, isPasswordEmpty, isConfirmPasswordEmpty);

                    // Tự động nhảy con trỏ chuột vào ô trống đầu tiên tìm thấy từ trên xuống để người dùng gõ liền
                    if (isFullnameEmpty) edtFullName.requestFocus();
                    else if (isUsernameEmpty) edtUsername.requestFocus();
                    else if (isEmailEmpty) edtEmail.requestFocus();
                    else if (isPhoneEmpty) edtPhone.requestFocus();
                    else if (isPasswordEmpty) edtPassword.requestFocus();
                    else if (isConfirmPasswordEmpty) edtConfirmPassword.requestFocus();

                } else if (!pass.equals(confirmPass)) {
                    // 3. Trường hợp điền đủ hết rồi nhưng mật khẩu không khớp -> Chỉ phạt đỏ 2 ô mật khẩu
                    triggerRegisterError("Mật khẩu xác nhận không trùng khớp!", false, false, false, false, false, true, true);
                } else {
                    // 4. Mọi thứ hoàn hảo sạch lỗi -> Tiến hành gọi API
                    executeRegisterApi(username, email, pass, fullName, dob, phone);
                }
            });
        }

        // 3. Nút điều hướng sang trang Đăng nhập
        if (txtGotoLogin != null) {
            txtGotoLogin.setOnClickListener(v -> {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
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

        // 4. Kích hoạt nút ẩn hiện mật khẩu và bộ lắng nghe dọn lỗi
        setupPasswordVisibilityToggles();
        setupTextChangeListeners();
    }

    // ── API: Thực hiện Đăng Ký Người Dùng ──────────────────────────

    private void executeRegisterApi(String username, String email, String password, String fullName, String dob, String phone) {
        showRegisterLoading(true); // 🟢 Bật hiệu ứng loading ẩn chữ, xoay Spinner

        RegisterRequest request = new RegisterRequest(username, email, password, fullName, dob, phone);

        ApiClient.getApiService().registerUser(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                showRegisterLoading(false); // Tắt loading khi nhận dữ liệu từ máy chủ

                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse regResponse = response.body();
                    if (regResponse.isSuccess() && regResponse.getData() != null) {

                        String verificationToken = regResponse.getData();
                        Intent intent = new Intent(RegisterActivity.this, VerifyOtpActivity.class);
                        intent.putExtra("VERIFICATION_TOKEN", verificationToken);
                        intent.putExtra("EMAIL", email);
                        startActivity(intent);
                        finish(); // Đóng trang đăng ký sau khi qua OTP thành công

                    } else {
                        // API trả về lỗi logic (Ví dụ: Email trùng lặp) -> Đỏ ô username/email cảnh báo
                        triggerRegisterError(regResponse.getMessage(), false, true, true, false, false, false, false);
                    }
                } else {
                    // Phản hồi mã lỗi HTTP -> Đỏ cả 2 vùng tài khoản để kiểm tra lại
                    triggerRegisterError("Đăng ký thất bại! Email hoặc Username đã tồn tại.", false, true, true, false, false, false, false);
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                showRegisterLoading(false); // Tắt hiệu ứng xoay tải khi mất mạng
                triggerRegisterError("Kết nối thất bại: " + t.getMessage(), false, false, false, false, false, false, false);
            }
        });
    }

    // ── Hàm Bổ Trợ 1: Phân phối trạng thái lỗi lên từng ô cụ thể ──────

    private void triggerRegisterError(String message, boolean f, boolean u, boolean e, boolean p, boolean d, boolean pwd, boolean cpwd) {
        if (tvErrorMessage != null) {
            tvErrorMessage.setText(message);
            tvErrorMessage.setVisibility(View.VISIBLE);
        }

        // Phạt viền đỏ cụ thể theo từng tham số điều kiện truyền vào
        if (edtFullName != null) edtFullName.setBackgroundResource(f ? R.drawable.bg_input_error : R.drawable.bg_input_rounded);
        if (edtUsername != null) edtUsername.setBackgroundResource(u ? R.drawable.bg_input_error : R.drawable.bg_input_rounded);
        if (edtEmail != null) edtEmail.setBackgroundResource(e ? R.drawable.bg_input_error : R.drawable.bg_input_rounded);
        if (edtPhone != null) edtPhone.setBackgroundResource(p ? R.drawable.bg_input_error : R.drawable.bg_input_rounded);
        if (edtDob != null) edtDob.setBackgroundResource(d ? R.drawable.bg_input_error : R.drawable.bg_input_rounded);

        if (edtPassword != null) {
            View w1 = (View) edtPassword.getParent();
            if (w1 != null) w1.setBackgroundResource(pwd ? R.drawable.bg_input_error : R.drawable.bg_input_rounded);
        }

        if (edtConfirmPassword != null) {
            View w2 = (View) edtConfirmPassword.getParent();
            if (w2 != null) w2.setBackgroundResource(cpwd ? R.drawable.bg_input_error : R.drawable.bg_input_rounded);
        }
    }

    // ── Hàm Bổ Trợ 2: Quản lý ẩn/hiện chữ nút và Spinner Loading ──────

    private void showRegisterLoading(boolean isLoading) {
        if (isLoading) {
            if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
            if (btnRegisterSubmit != null) {
                btnRegisterSubmit.setText("");
                btnRegisterSubmit.setEnabled(false);
            }
            if (pbRegisterLoading != null) pbRegisterLoading.setVisibility(View.VISIBLE);
        } else {
            if (btnRegisterSubmit != null) {
                btnRegisterSubmit.setText("ĐĂNG KÝ");
                btnRegisterSubmit.setEnabled(true);
            }
            if (pbRegisterLoading != null) pbRegisterLoading.setVisibility(View.GONE);
        }
    }

    // ── Hàm Bổ Trợ 3: Tự động dọn dẹp viền lỗi khi người dùng gõ chữ ──

    private void setupTextChangeListeners() {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {} // Bảo đảm không lỗi cú pháp after

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Người dùng bắt đầu nhập lại -> Ẩn dòng lỗi chữ, chuyển viền về xám mặc định
                if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
                triggerRegisterError("", false, false, false, false, false, false, false);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };

        if (edtFullName != null) edtFullName.addTextChangedListener(watcher);
        if (edtUsername != null) edtUsername.addTextChangedListener(watcher);
        if (edtEmail != null) edtEmail.addTextChangedListener(watcher);
        if (edtPhone != null) edtPhone.addTextChangedListener(watcher);
        if (edtPassword != null) edtPassword.addTextChangedListener(watcher);
        if (edtConfirmPassword != null) edtConfirmPassword.addTextChangedListener(watcher);
    }

    // ── Hàm Bổ Trợ 4: Logic ẩn/hiện văn bản mật khẩu gốc ──────────────

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

    // ── Hàm Bổ Trợ 5: Hiển thị bộ chọn ngày tháng năm sinh ──────────────

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.TaleXDatePickerTheme, // ◄ CHỈ CẦN THÊM DÒNG STYLE NÀY VÀO ĐÂY
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String displayDate = String.format("%02d-%02d-%04d", selectedDay, selectedMonth + 1, selectedYear);
                    edtDob.setText(displayDate);
                    dobApi = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);

                    if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
                    if (edtDob != null) edtDob.setBackgroundResource(R.drawable.bg_input_rounded);
                },
                year,
                month,
                day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
}