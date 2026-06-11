package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.account;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment {

    private TextView tvUsername;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Ánh xạ an toàn
        tvUsername = view.findViewById(R.id.tv_username);
        btnLogout = view.findViewById(R.id.btn_logout);
        Button btnUpgrade = view.findViewById(R.id.btn_upgrade);
        TextView btnChangePassword = view.findViewById(R.id.btn_change_password);
        TextView btnHistory = view.findViewById(R.id.btn_history);
        TextView btnFavorite = view.findViewById(R.id.btn_favorite);
        TextView btnPolicy = view.findViewById(R.id.btn_policy);

        // Các sự kiện Toast thông báo cũ
        if (btnUpgrade != null) btnUpgrade.setOnClickListener(v -> Toast.makeText(getContext(), "Gia hạn Premium!", Toast.LENGTH_SHORT).show());
        if (btnChangePassword != null) btnChangePassword.setOnClickListener(v -> Toast.makeText(getContext(), "Đổi mật khẩu!", Toast.LENGTH_SHORT).show());
        if (btnHistory != null) btnHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Mở lịch sử!", Toast.LENGTH_SHORT).show());
        if (btnFavorite != null) btnFavorite.setOnClickListener(v -> Toast.makeText(getContext(), "Mở yêu thích!", Toast.LENGTH_SHORT).show());
        if (btnPolicy != null) btnPolicy.setOnClickListener(v -> Toast.makeText(getContext(), "Mở chính sách!", Toast.LENGTH_SHORT).show());

        // XỬ LÝ SỰ KIỆN NÚT ĐĂNG XUẤT CALL API
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogoutWorkflow());
        }

        return view;
    }

    // LUỒNG NGHIỆP VỤ ĐĂNG XUẤT AN TOÀN
    private void handleLogoutWorkflow() {
        if (getContext() == null) return;

        btnLogout.setEnabled(false);
        btnLogout.setText("ĐANG ĐĂNG XUẤT...");

        try {
            // 1. Mở file mã hóa bảo mật ra để lấy Refresh Token
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    getContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            String refreshToken = securePrefs.getString("REFRESH_TOKEN", null);

            if (refreshToken != null) {
                // 2. Tiến hành bắn API lên Backend để hủy Token dưới Database
                LogoutRequest request = new LogoutRequest(refreshToken);
                ApiClient.getApiService().logoutUser(request).enqueue(new Callback<LogoutResponse>() {
                    @Override
                    public void onResponse(Call<LogoutResponse> call, Response<LogoutResponse> response) {
                        // Bất kể Backend trả về thành công hay lỗi (hết hạn), App vẫn sẽ xóa Token cục bộ để đảm bảo an toàn
                        clearSessionAndNavigateToLogin(securePrefs);
                    }

                    @Override
                    public void onFailure(Call<LogoutResponse> call, Throwable t) {
                        // Khi mất mạng, vẫn cho xóa session ở App để người dùng thoát được tài khoản
                        clearSessionAndNavigateToLogin(securePrefs);
                    }
                });
            } else {
                clearSessionAndNavigateToLogin(securePrefs);
            }

        } catch (Exception e) {
            e.printStackTrace();
            btnLogout.setEnabled(true);
            btnLogout.setText("ĐĂNG XUẤT");
            Toast.makeText(getContext(), "Lỗi hệ thống khi đăng xuất!", Toast.LENGTH_SHORT).show();
        }
    }

    // HÀM XÓA TRẮNG BỘ NHỚ VÀ ĐẨY NGƯỜI DÙNG RA TRANG LOGIN
    private void clearSessionAndNavigateToLogin(SharedPreferences securePrefs) {
        if (getActivity() == null) return;

        // Xóa sạch bách dữ liệu Token mã hóa trên máy
        SharedPreferences.Editor editor = securePrefs.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(getContext(), "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();

        // Di chuyển về màn hình LoginActivity, dọn sạch hàng chờ các Activity cũ
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}