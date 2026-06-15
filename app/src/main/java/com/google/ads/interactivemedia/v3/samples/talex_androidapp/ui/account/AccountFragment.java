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

import com.bumptech.glide.Glide;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiService;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LogoutResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ProfileResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login.ChangePasswordActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login.LoginActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login.RegisterActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment {

    // 2 Layout đại diện cho 2 file include con
    private View includeLoggedIn, includeLoggedOut;

    private Button btnLogout, btnLoginNow, btnRegisterNow;
    private SharedPreferences securePrefs;
    private boolean hasPasswordFlag = true;

    private ImageView imgAvatar;
    private TextView tvFullName, tvRoleName, tvUsername, tvEmail, tvPhone, tvDob;

    // Đã sửa: Gom toàn bộ các nút dạng Khối/LinearLayout thành kiểu View chung để tránh ClassCastException
    private View btnHistory, btnFavorite, btnChangePassword, btnUpgrade, btnPolicy;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // 1. Ánh xạ 2 thẻ <include> từ file fragment_account.xml gốc
        includeLoggedIn = view.findViewById(R.id.include_logged_in);
        includeLoggedOut = view.findViewById(R.id.include_logged_out);

        // 2. Kỹ thuật ép tìm ID xuyên qua file include (Dùng layout con để tìm kiếm View bên trong nó)
        if (includeLoggedIn != null) {
            imgAvatar = includeLoggedIn.findViewById(R.id.img_avatar);
//            tvFullName = includeLoggedIn.findViewById(R.id.tv_full_name);
            tvRoleName = includeLoggedIn.findViewById(R.id.tv_role_name);
            tvUsername = includeLoggedIn.findViewById(R.id.tv_username);
            tvEmail = includeLoggedIn.findViewById(R.id.tv_email);
            tvPhone = includeLoggedIn.findViewById(R.id.tv_phone);
            tvDob = includeLoggedIn.findViewById(R.id.tv_dob);
            btnLogout = includeLoggedIn.findViewById(R.id.btn_logout);

            // Đã sửa: Ánh xạ trực tiếp vào các biến toàn cục (bỏ kiểu dữ liệu đứng trước)
            btnUpgrade = includeLoggedIn.findViewById(R.id.btn_upgrade);
//            btnHistory = includeLoggedIn.findViewById(R.id.btn_history);
//            btnFavorite = includeLoggedIn.findViewById(R.id.btn_favorite);
            btnChangePassword = includeLoggedIn.findViewById(R.id.btn_change_password);
            btnPolicy = includeLoggedIn.findViewById(R.id.btn_policy);
        }

        if (includeLoggedOut != null) {
            btnLoginNow = includeLoggedOut.findViewById(R.id.btn_login_now);
            btnRegisterNow = includeLoggedOut.findViewById(R.id.btn_register_now);
        }

        // 3. Khởi tạo EncryptedSharedPreferences để quản lý token an toàn
        initSecurePreferences();

        // 4. Quét trạng thái token để ẩn hiện giao diện tương ứng
        checkUserSessionStatus();

        // Xử lý sự kiện click cho màn hình Đã đăng nhập
        if (btnUpgrade != null) btnUpgrade.setOnClickListener(v -> Toast.makeText(getContext(), "Gia hạn Premium!", Toast.LENGTH_SHORT).show());
        if (btnChangePassword != null) btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            intent.putExtra("HAS_PASSWORD", hasPasswordFlag);
            startActivity(intent);
        });
        if (btnHistory != null) btnHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Mở lịch sử!", Toast.LENGTH_SHORT).show());
        if (btnFavorite != null) btnFavorite.setOnClickListener(v -> Toast.makeText(getContext(), "Mở yêu thích!", Toast.LENGTH_SHORT).show());
        if (btnPolicy != null) btnPolicy.setOnClickListener(v -> Toast.makeText(getContext(), "Mở chính sách!", Toast.LENGTH_SHORT).show());

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogoutWorkflow());
        }

        // Xử lý sự kiện click cho màn hình Chưa đăng nhập
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

    // Ẩn/Hiện nguyên cụm File Include tương ứng với trạng thái Session
    private void checkUserSessionStatus() {
        if (securePrefs == null) return;

        String accessToken = securePrefs.getString("ACCESS_TOKEN", null);

        if (accessToken != null && !accessToken.isEmpty()) {
            if (includeLoggedIn != null) includeLoggedIn.setVisibility(View.VISIBLE);
            if (includeLoggedOut != null) includeLoggedOut.setVisibility(View.GONE);

            fetchUserProfileFromBackend(accessToken);
        } else {
            if (includeLoggedIn != null) includeLoggedIn.setVisibility(View.GONE);
            if (includeLoggedOut != null) includeLoggedOut.setVisibility(View.VISIBLE);
        }
    }

    private void fetchUserProfileFromBackend(String token) {
        String authHeader = "Bearer " + token;

        ApiClient.getApiService().getCurrentProfile(authHeader).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ProfileResponse.UserData user = response.body().getData();

                    if (user != null) {
                        hasPasswordFlag = user.isHasPassword();

                        if (tvFullName != null) tvFullName.setText(user.getFullName() != null ? user.getFullName() : "Chưa cập nhật họ tên");
                        if (tvRoleName != null) tvRoleName.setText( (user.getRoleName() != null ? user.getRoleName() : "Thành viên"));
                        if (tvUsername != null) tvUsername.setText(user.getUsername());
                        if (tvEmail != null) tvEmail.setText(user.getEmail());

                        if (tvPhone != null) {
                            tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Chưa liên kết SĐT");
                        }
                        if (tvDob != null) {
                            if (user.getDateOfBirth() != null && !user.getDateOfBirth().isEmpty()) {
                                try {
                                    java.text.SimpleDateFormat formatInput = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                                    java.text.SimpleDateFormat formatOutput = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());

                                    java.util.Date date = formatInput.parse(user.getDateOfBirth());
                                    String formattedDate = formatOutput.format(date);

                                    tvDob.setText(formattedDate);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    tvDob.setText(user.getDateOfBirth());
                                }
                            } else {
                                tvDob.setText("Chưa cập nhật ngày sinh");
                            }
                        }

                        if (getContext() != null && imgAvatar != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            Glide.with(AccountFragment.this)
                                    .load(user.getAvatarUrl())
                                    .placeholder(R.drawable.ic_nav_account)
                                    .error(R.drawable.ic_nav_account)
                                    .into(imgAvatar);
                        }
                    }
                } else {
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

    private void clearSessionLocal() {
        if (securePrefs == null) return;

        SharedPreferences.Editor editor = securePrefs.edit();
        editor.clear();
        editor.apply();

        if (btnLogout != null) {
            btnLogout.setEnabled(true);
            btnLogout.setText("Đăng Xuất");
        }
        Toast.makeText(getContext(), "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();

        checkUserSessionStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkUserSessionStatus();
    }
}