package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.account;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.profile.ProfileResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.profile.UpdateProfileRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileDetailActivity extends AppCompatActivity {

    private ProgressBar pbLoading;
    private ScrollView scrollContent;
    private TextView tvFullnameTitle, tvEmail, tvGoogleStatus;
    private EditText edtFullname, edtUsername, edtPhone, edtDob;
    private MaterialButton btnEditProfile;

    private FrameLayout layoutAvatarClick;
    private ImageView imgDetailAvatar, imgCameraIcon;

    private boolean isEditMode = false;
    private String currentAvatarUrl = "";
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // 🟢 BIẾN CHỐNG BUG 409: Lưu trữ tên tài khoản gốc từ Server đổ về để đối chiếu
    private String originalUsername = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_detail);

        // 1. Ánh xạ toàn bộ View từ file XML
        pbLoading = findViewById(R.id.pb_profile_detail_loading);
        scrollContent = findViewById(R.id.scroll_profile_content);
        tvFullnameTitle = findViewById(R.id.tv_detail_fullname);
        edtFullname = findViewById(R.id.edt_detail_fullname);
        edtUsername = findViewById(R.id.tv_detail_username);
        tvEmail = findViewById(R.id.tv_detail_email);
        edtPhone = findViewById(R.id.tv_detail_phone);
        edtDob = findViewById(R.id.tv_detail_dob);
        tvGoogleStatus = findViewById(R.id.tv_detail_google_status);
        btnEditProfile = findViewById(R.id.btn_edit_profile);

        layoutAvatarClick = findViewById(R.id.layout_avatar_click);
        imgDetailAvatar = findViewById(R.id.img_detail_avatar);
        imgCameraIcon = findViewById(R.id.img_camera_icon);

        ImageView btnBack = findViewById(R.id.btn_profile_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // 2. Đăng ký nhận kết quả chọn hình ảnh từ Gallery thiết bị
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null && imgDetailAvatar != null) {
                            imgDetailAvatar.setImageURI(selectedImageUri);
                        }
                    }
                }
        );

        if (layoutAvatarClick != null) {
            layoutAvatarClick.setOnClickListener(v -> {
                if (isEditMode) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    imagePickerLauncher.launch(intent);
                }
            });
        }

        // 3. Tải thông tin người dùng ban đầu
        String token = getAccessToken();
        if (token != null) {
            loadUserProfileData("Bearer " + token);
        } else {
            Toast.makeText(this, "Phiên đăng nhập hết hạn!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 4. Bắt sự kiện click nút Chỉnh sửa / Lưu thay đổi
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                if (!isEditMode) {
                    toggleEditMode(true);
                } else {
                    executeJsonPutApi();
                }
            });
        }

        // 5. Mở DatePicker khi nhấn vào trường ngày sinh (chỉ khi đang mở chế độ sửa)
        if (edtDob != null) {
            edtDob.setOnClickListener(v -> {
                if (isEditMode) showDatePickerPopup();
            });
        }
    }

    // ── Điều phối trạng thái Khóa / Mở các ô nhập liệu ──────────────────
    private void toggleEditMode(boolean enable) {
        isEditMode = enable;
        if (edtUsername != null) edtUsername.setEnabled(enable);
        if (edtPhone != null) edtPhone.setEnabled(enable);
        if (edtFullname != null) edtFullname.setEnabled(enable);

        // 🟢 SỬA LỖI CHÍ MẠNG: Bật/Tắt quyền click chạm vào ô Ngày sinh khi đổi chế độ
        if (edtDob != null) {
            edtDob.setClickable(enable);
            edtDob.setFocusable(false); // Giữ false để không hiện bàn phím chữ rác
        }

        if (layoutAvatarClick != null) {
            layoutAvatarClick.setClickable(enable);
            layoutAvatarClick.setFocusable(enable);
        }
        if (imgCameraIcon != null) {
            imgCameraIcon.setVisibility(enable ? View.VISIBLE : View.GONE);
        }

        if (enable) {
            btnEditProfile.setText("Lưu thay đổi");
            btnEditProfile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D4AF37")));
            btnEditProfile.setTextColor(Color.parseColor("#0F0F0F"));
            if (edtFullname != null) edtFullname.requestFocus();
        } else {
            btnEditProfile.setText("Chỉnh sửa hồ sơ");
            btnEditProfile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#222224")));
            btnEditProfile.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    // ── XỬ LÝ API PUT: Gom thông tin chữ và nén ảnh Base64 gửi lên hệ thống ──
    private void executeJsonPutApi() {
        String token = getAccessToken();
        if (token == null) return;

        pbLoading.setVisibility(View.VISIBLE);
        btnEditProfile.setEnabled(false);

        String inputUsername = edtUsername.getText().toString().trim();
        String fullName = edtFullname.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String dobFromUi= edtDob.getText().toString().trim();
        String serverDobPayload = dobFromUi;
        if (!dobFromUi.isEmpty() && dobFromUi.contains("-")) {
            try {
                java.text.SimpleDateFormat uiFormat = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
                java.text.SimpleDateFormat serverFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.util.Date parsedDate = uiFormat.parse(dobFromUi);
                if (parsedDate != null) {
                    serverDobPayload = serverFormat.format(parsedDate);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 🟢 GIẢI PHÁP SỬA BUG 409: Nếu username giữ nguyên, gán null để Backend bỏ qua kiểm tra trùng Database
        String finalUsernamePayload = inputUsername;
        if (inputUsername.equals(originalUsername)) {
            finalUsernamePayload = null;
        }

        String finalAvatarPayload = currentAvatarUrl;
        if (selectedImageUri != null) {
            String base64String = convertUriToBase64(selectedImageUri);
            if (base64String != null) {
                finalAvatarPayload = "data:image/jpeg;base64," + base64String;
            }
        }

        UpdateProfileRequest request = new UpdateProfileRequest(finalUsernamePayload, fullName, phone, serverDobPayload, finalAvatarPayload);

        ApiClient.getApiService().updateCurrentUserProfile("Bearer " + token, request)
                .enqueue(new Callback<ProfileResponse>() {
                    @Override
                    public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                        pbLoading.setVisibility(View.GONE);
                        btnEditProfile.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(ProfileDetailActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                            selectedImageUri = null;
                            toggleEditMode(false);

                            if (response.body().getData() != null) {
                                ProfileResponse.UserData updatedUser = response.body().getData();
                                if (tvFullnameTitle != null) tvFullnameTitle.setText(updatedUser.getFullName());
                                currentAvatarUrl = updatedUser.getAvatarUrl();
                                // 🟢 Cập nhật lại cờ gốc để đối chiếu cho lần bấm sửa sau
                                originalUsername = updatedUser.getUsername();
                            }
                        } else {
                            String backendErrorMessage = "Cập nhật thất bại (Lỗi hệ thống)!";
                            try {
                                if (response.errorBody() != null) {
                                    backendErrorMessage = response.errorBody().string();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(ProfileDetailActivity.this, backendErrorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ProfileResponse> call, Throwable t) {
                        pbLoading.setVisibility(View.GONE);
                        btnEditProfile.setEnabled(true);
                        Toast.makeText(ProfileDetailActivity.this, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── API GET: Nạp thông tin ban đầu khi vừa vào trang ─────────────────────
    private void loadUserProfileData(String authToken) {
        pbLoading.setVisibility(View.VISIBLE);
        scrollContent.setVisibility(View.GONE);

        ApiClient.getApiService().getCurrentProfile(authToken).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                pbLoading.setVisibility(View.GONE);
                scrollContent.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ProfileResponse.UserData data = response.body().getData();
                    if (data != null) {
                        if (tvFullnameTitle != null) tvFullnameTitle.setText(data.getFullName());
                        if (edtFullname != null) edtFullname.setText(data.getFullName());
                        if (edtUsername != null) edtUsername.setText(data.getUsername());

                        // 🟢 LƯU GIÁ TRỊ GỐC: Đóng vai trò cực kỳ quan trọng để lách bẫy check trùng
                        originalUsername = data.getUsername();

                        if (tvEmail != null) tvEmail.setText(data.getEmail());
                        if (edtPhone != null) edtPhone.setText(data.getPhone());
                        if (edtDob != null && data.getDateOfBirth() != null && !data.getDateOfBirth().isEmpty()) {
                            try {
                                java.text.SimpleDateFormat serverFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                                java.text.SimpleDateFormat uiFormat = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
                                java.util.Date date = serverFormat.parse(data.getDateOfBirth());
                                if (date != null) {
                                    edtDob.setText(uiFormat.format(date));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                edtDob.setText(data.getDateOfBirth()); // Phòng hờ nếu lỗi thì giữ nguyên
                            }
                        }
                        currentAvatarUrl = data.getAvatarUrl();

                        if (imgDetailAvatar != null && currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                            Glide.with(ProfileDetailActivity.this)
                                    .load(currentAvatarUrl)
                                    .placeholder(R.drawable.ic_nav_account)
                                    .into(imgDetailAvatar);
                        }
                        if (tvGoogleStatus != null) {
                            tvGoogleStatus.setText(data.isGoogleLinked() ? "Đã liên kết ✓" : "Chưa liên kết");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
            }
        });
    }

    // ── Hàm chuyển đổi và nén tối đa ảnh sang chuỗi Base64 ──────────────────
    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return null;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // Nén ảnh xuống chất lượng 25% để chuỗi JSON siêu ngắn, tránh lỗi Database tràn cột VARCHAR
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            if (inputStream != null) inputStream.close();
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Hộp thoại lịch đồng bộ tông Đỏ - Gold của hệ thống TaleX
    private void showDatePickerPopup() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, R.style.TaleXDatePickerTheme,
                (view, year, month, dayOfMonth) -> {
                    // 🟢 ĐÃ SỬA: Hiển thị định dạng dd-MM-yyyy ngay khi vừa chọn xong từ Calendar
                    String selectedDate = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year);
                    if (edtDob != null) edtDob.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private String getAccessToken() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref", masterKeyAlias, this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            return securePrefs.getString("ACCESS_TOKEN", null);
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }
    }
}
