package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog; // Bổ sung thư viện tạo Hộp thoại
import androidx.appcompat.app.AppCompatActivity;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.CompleteProfileRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.MainActivity;
// Import EkycActivity mà chúng ta vừa tạo ở bước trước
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc.EkycActivity;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompleteProfileActivity extends AppCompatActivity {

    private static final String TAG = "CompleteProfileActivity";

    private EditText edtPhone, edtDob;
    private Button btnSubmit;
    private String verificationToken;
    private String selectedDateForApi = ""; // yyyy-MM-dd

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        // Nhận verification token từ LoginActivity
        verificationToken = getIntent().getStringExtra("VERIFICATION_TOKEN");

        if (TextUtils.isEmpty(verificationToken)) {
            Toast.makeText(this, "Thiếu token xác minh. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Ánh xạ giao diện
        edtPhone = findViewById(R.id.edt_complete_phone);
        edtDob = findViewById(R.id.edt_complete_dob);
        btnSubmit = findViewById(R.id.btn_complete_submit);

        // DatePicker cho ngày sinh
        edtDob.setOnClickListener(v -> showDatePicker());

        // Nút Hoàn tất
        btnSubmit.setOnClickListener(v -> {
            String phone = edtPhone.getText().toString().trim();

            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(selectedDateForApi)) {
                Toast.makeText(this, "Vui lòng chọn ngày sinh!", Toast.LENGTH_SHORT).show();
                return;
            }

            executeCompleteProfile(phone, selectedDateForApi);
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 18; // Mặc định 18 tuổi
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            // Hiển thị dd-MM-yyyy
            String displayDate = String.format(Locale.getDefault(), "%02d-%02d-%04d", d, m + 1, y);
            edtDob.setText(displayDate);
            // Gửi API yyyy-MM-dd
            selectedDateForApi = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
        }, year, month, day);

        dialog.show();
    }

    private void executeCompleteProfile(String phone, String dateOfBirth) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("ĐANG XỬ LÝ...");

        CompleteProfileRequest request = new CompleteProfileRequest(verificationToken, dateOfBirth, phone);

        ApiClient.getApiService().completeProfile(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("HOÀN TẤT");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {
                        // BƯỚC QUAN TRỌNG: Lưu token XONG mới được hiển thị hộp thoại hỏi eKYC
                        saveTokens(loginResponse.getData().getAccessToken(), loginResponse.getData().getRefreshToken());

                        // Gọi hàm hiển thị hộp thoại điều hướng
                        showEkycPromptDialog();

                    } else {
                        Toast.makeText(CompleteProfileActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(CompleteProfileActivity.this, "Không thể hoàn tất hồ sơ!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("HOÀN TẤT");
                Log.e(TAG, "Complete profile API error", t);
                Toast.makeText(CompleteProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * CHIẾC CẦU NỐI: Hộp thoại hỏi người dùng có muốn làm eKYC ngay không
     */
    private void showEkycPromptDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật thành công!")
                .setMessage("Tài khoản của bạn đã được kích hoạt. Bạn có muốn xác thực danh tính (eKYC) ngay bây giờ để trở thành Nhà sáng tạo (Creator) và mở khóa toàn bộ tính năng không?")
                .setCancelable(false) // Bắt buộc user phải chọn 1 trong 2 nút
                .setPositiveButton("Xác minh ngay", (dialog, which) -> {
                    // Chuyển sang luồng eKYC
                    Intent intent = new Intent(CompleteProfileActivity.this, EkycActivity.class);
                    startActivity(intent);
                    finish(); // Đóng màn hình Complete Profile
                })
                .setNegativeButton("Để sau", (dialog, which) -> {
                    // Chuyển về trang chủ như logic cũ
                    navigateToMain();
                })
                .show();
    }

    private void saveTokens(String accessToken, String refreshToken) {
        try {
            String masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(
                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
            );

            SharedPreferences securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    CompleteProfileActivity.this,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = securePrefs.edit();
            editor.putString("ACCESS_TOKEN", accessToken);
            editor.putString("REFRESH_TOKEN", refreshToken);
            editor.apply();

        } catch (java.security.GeneralSecurityException | java.io.IOException e) {
            Log.e(TAG, "Error saving tokens", e);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(CompleteProfileActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}