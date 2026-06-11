package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.account;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.bumptech.glide.Glide; // ◄ 1. BẮT BUỘC CẦN DÒNG NÀY ĐỂ LOAD ẢNH URL
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiService;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ProfileResponse; // ◄ Thêm model hứng dữ liệu
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login.LoginActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login.RegisterActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment {

    private LinearLayout layoutLoggedIn, layoutLoggedOut;
    private Button btnLogout, btnLoginNow, btnRegisterNow;
    private SharedPreferences securePrefs;

    // 📍 2. BỔ SUNG: Khai báo toàn bộ các View chứa thông tin cá nhân từ API
    private ImageView imgAvatar;
    private TextView tvFullName, tvRoleName, tvUsername, tvEmail, tvPhone, tvDob;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // 1. Ánh xạ các Layout bọc điều khiển trạng thái cấu trúc ẩn hiện
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        layoutLoggedOut = view.findViewById(R.id.layout_logged_out);

        // 📍 3. CẬP NHẬT: Ánh xạ chuẩn các ID mới từ file XML đã nâng cấp
        imgAvatar = view.findViewById(R.id.img_avatar);
        tvFullName = view.findViewById(R.id.tv_full_name);
        tvRoleName = view.findViewById(R.id.tv_role_name);
        tvUsername = view.findViewById(R.id.tv_username);
        tvEmail = view.findViewById(R.id.tv_email);
        tvPhone = view.findViewById(R.id.tv_phone);
        tvDob = view.findViewById(R.id.tv_dob);

        btnLogout = view.findViewById(R.id.btn_logout);
        Button btnUpgrade = view.findViewById(R.id.btn_upgrade);
        TextView btnChangePassword = view.findViewById(R.id.btn_change_password);
        TextView btnHistory = view.findViewById(R.id.btn_history);
        TextView btnFavorite = view.findViewById(R.id.btn_favorite);
        TextView btnPolicy = view.findViewById(R.id.btn_policy);

        // Ánh xạ 2 nút mới của giao diện Khách ẩn danh
        btnLoginNow = view.findViewById(R.id.btn_login_now);
        btnRegisterNow = view.findViewById(R.id.btn_register_now);

        // 2. Khởi tạo EncryptedSharedPreferences để quản lý token an toàn
        initSecurePreferences();

        // 3. Quét trạng thái token để ẩn hiện giao diện tương ứng
        checkUserSessionStatus();

        // Các sự kiện thông báo cũ
        if (btnUpgrade != null) btnUpgrade.setOnClickListener(v -> Toast.makeText(getContext(), "Gia hạn Premium!", Toast.LENGTH_SHORT).show());
        if (btnChangePassword != null) btnChangePassword.setOnClickListener(v -> Toast.makeText(getContext(), "Đổi mật khẩu!", Toast.LENGTH_SHORT).show());
        if (btnHistory != null) btnHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Mở lịch sử!", Toast.LENGTH_SHORT).show());
        if (btnFavorite != null) btnFavorite.setOnClickListener(v -> Toast.makeText(getContext(), "Mở yêu thích!", Toast.LENGTH_SHORT).show());
        if (btnPolicy != null) btnPolicy.setOnClickListener(v -> Toast.makeText(getContext(), "Mở chính sách!", Toast.LENGTH_SHORT).show());

        // Sự kiện click nút đăng xuất
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogoutWorkflow());
        }

        // 4. Xử lý sự kiện khi Khách bấm vào ĐĂNG NHẬP / ĐĂNG KÝ
        if (btnLoginNow != null) {
            btnLoginNow.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });
        }

        if (btnRegisterNow != null) {
            btnRegisterNow.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), RegisterActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }

    private void initSecurePreferences() {
        if (getContext() == null) return;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    getContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🆕 📍 4. NÂNG CẤP TOÀN DIỆN: Kiểm tra token kết hợp gọi API profile thời gian thực
    private void checkUserSessionStatus() {
        if (securePrefs == null) return;

        // Đọc thử Access Token mã hóa bảo mật trên máy
        String accessToken = securePrefs.getString("ACCESS_TOKEN", null);

        if (accessToken != null && !accessToken.isEmpty()) {
            // ĐÃ ĐĂNG NHẬP: Hiện layout profile và kích hoạt lấy data thật từ Server
            if (layoutLoggedIn != null) layoutLoggedIn.setVisibility(View.VISIBLE);
            if (layoutLoggedOut != null) layoutLoggedOut.setVisibility(View.GONE);

            fetchUserProfileFromBackend(accessToken);
        } else {
            // CHƯA ĐĂNG NHẬP: Ẩn Profile, hiện giao diện trống kèm 2 nút mời gọi
            if (layoutLoggedIn != null) layoutLoggedIn.setVisibility(View.GONE);
            if (layoutLoggedOut != null) layoutLoggedOut.setVisibility(View.VISIBLE);
        }
    }

    // 🆕 📍 5. BỔ SUNG: Luồng đóng gói Bearer Header bắn API lấy thông tin người dùng
    private void fetchUserProfileFromBackend(String token) {
        String authHeader = "Bearer " + token;

        ApiClient.getApiService().getCurrentProfile(authHeader).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ProfileResponse.UserData user = response.body().getData();

                    if (user != null) {
                        // Đổ dữ liệu từ API vào các ID tương ứng
                        if (tvFullName != null) tvFullName.setText(user.getFullName() != null ? user.getFullName() : "Chưa cập nhật họ tên");
                        if (tvRoleName != null) tvRoleName.setText("👑 Cấp bậc: " + (user.getRoleName() != null ? user.getRoleName() : "Thành viên"));
                        if (tvUsername != null) tvUsername.setText(user.getUsername());
                        if (tvEmail != null) tvEmail.setText(user.getEmail());

                        if (tvPhone != null) {
                            tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Chưa liên kết SĐT");
                        }
                        if (tvDob != null) {
                            if (user.getDateOfBirth() != null && !user.getDateOfBirth().isEmpty()) {
                                try {
                                    // 1. Khai báo định dạng gốc mà Server Backend đang trả về (yyyy-MM-dd)
                                    java.text.SimpleDateFormat formatInput = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

                                    // 2. Khai báo định dạng Việt Nam mượt mà mà Triệu muốn hiển thị (dd-MM-yyyy)
                                    java.text.SimpleDateFormat formatOutput = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());

                                    // 3. Tiến hành bóc tách và hoán đổi cấu trúc chuỗi ngày sinh
                                    java.util.Date date = formatInput.parse(user.getDateOfBirth());
                                    String formattedDate = formatOutput.format(date);

                                    tvDob.setText(formattedDate); // Sẽ hiển thị chuẩn đét dạng "23-03-2004"

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    tvDob.setText(user.getDateOfBirth()); // Nếu lỡ lỗi thì hiện tạm chuỗi gốc của server
                                }
                            } else {
                                tvDob.setText("Chưa cập nhật ngày sinh");
                            }
                        }

                        // Sử dụng thư viện Glide thông minh để nạp ảnh đại diện từ URL
                        if (getContext() != null && imgAvatar != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            Glide.with(AccountFragment.this)
                                    .load(user.getAvatarUrl())
                                    .placeholder(R.drawable.ic_nav_account) // Hiện tạm ảnh mặc định trong lúc tải
                                    .error(R.drawable.ic_nav_account)       // Hiện ảnh mặc định nếu link URL hỏng
                                    .into(imgAvatar);
                        }
                    }
                } else {
                    // Nếu token hết hạn hoặc lỗi xác thực từ backend, tự động clear session
                    Toast.makeText(getContext(), "Phiên đăng nhập hết hạn!", Toast.LENGTH_SHORT).show();
                    clearSessionLocal();
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể kết nối máy chủ để đồng bộ dữ liệu!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // LUỒNG NGHIỆP VỤ ĐĂNG XUẤT AN TOÀN
    private void handleLogoutWorkflow() {
        if (securePrefs == null || getContext() == null) return;

        btnLogout.setEnabled(false);
        btnLogout.setText("ĐANG ĐĂNG XUẤT...");

        String refreshToken = securePrefs.getString("REFRESH_TOKEN", null);

        if (refreshToken != null) {
            LogoutRequest request = new LogoutRequest(refreshToken);
            ApiClient.getApiService().logoutUser(request).enqueue(new Callback<LogoutResponse>() {
                @Override
                public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                    clearSessionLocal();
                }

                @Override
                public void onFailure(Call<LogoutResponse> call, Throwable t) {
                    clearSessionLocal();
                }
            });
        } else {
            clearSessionLocal();
        }
    }

    // HÀM XÓA TRẮNG BỘ NHỚ VÀ THAY ĐỔI GIAO DIỆN TẠI CHỖ
    private void clearSessionLocal() {
        if (securePrefs == null) return;

        // Xóa sạch dữ liệu Token mật mã trong file "TaleXSecurePref"
        SharedPreferences.Editor editor = securePrefs.edit();
        editor.clear();
        editor.apply();

        if (btnLogout != null) {
            btnLogout.setEnabled(true);
            btnLogout.setText("Đăng Xuất");
        }
        Toast.makeText(getContext(), "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();

        // Báo hệ thống vẽ lại giao diện trạng thái "Chưa đăng nhập" ngay tại chỗ cực mượt
        checkUserSessionStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Luôn làm tươi (refresh) trạng thái tài khoản mỗi khi người dùng quay trở lại màn hình này
        checkUserSessionStatus();
    }
}