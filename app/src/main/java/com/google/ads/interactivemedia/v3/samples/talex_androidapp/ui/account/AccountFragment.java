package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.account;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login.LoginActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login.RegisterActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment {

    private LinearLayout layoutLoggedIn, layoutLoggedOut;
    private TextView tvUsername;
    private Button btnLogout, btnLoginNow, btnRegisterNow;
    private SharedPreferences securePrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // 1. Ánh xạ các Layout bọc điều khiển trạng thái cấu trúc ẩn hiện
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        layoutLoggedOut = view.findViewById(R.id.layout_logged_out);

        // Ánh xạ các thành phần cũ trong trang Profile
        tvUsername = view.findViewById(R.id.tv_username);
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

    // 🆕 HÀM KIỂM TRA TRẠNG THÁI: TỰ ĐỘNG BIẾN ĐỔI GIAO DIỆN
    private void checkUserSessionStatus() {
        if (securePrefs == null) return;

        // Đọc thử Access Token mã hóa trên máy
        String accessToken = securePrefs.getString("ACCESS_TOKEN", null);

        if (accessToken != null) {
            // ĐÃ ĐĂNG NHẬP: Hiện toàn bộ Profile xịn mịn, ẩn phần nút Đăng nhập đi
            if (layoutLoggedIn != null) layoutLoggedIn.setVisibility(View.VISIBLE);
            if (layoutLoggedOut != null) layoutLoggedOut.setVisibility(View.GONE);

            if (tvUsername != null) {
                tvUsername.setText("User"); // Sau này Triệu dùng JWT giải mã Access Token lấy fullName gán vào đây nhé
            }
        } else {
            // CHƯA ĐĂNG NHẬP: Ẩn Profile, hiện giao diện trống kèm 2 nút Đăng nhập/Đăng ký
            if (layoutLoggedIn != null) layoutLoggedIn.setVisibility(View.GONE);
            if (layoutLoggedOut != null) layoutLoggedOut.setVisibility(View.VISIBLE);
        }
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

    // 🆕 HÀM XÓA TRẮNG BỘ NHỚ VÀ THAY ĐỔI GIAO DIỆN TẠI CHỖ
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