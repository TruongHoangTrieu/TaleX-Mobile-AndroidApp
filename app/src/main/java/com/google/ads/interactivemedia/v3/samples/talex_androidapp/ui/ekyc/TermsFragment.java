package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiService;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.CreatorRegisterRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.CreatorRegisterResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.TermsResponse;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.security.GeneralSecurityException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TermsFragment extends Fragment {

    private static final String TAG = "TermsFragment";

    private TextView tvTitle, tvTermsContent;
    private CheckBox cbCheckpoint1, cbCheckpoint2;
    private MaterialButton btnAcceptAndContinue;
    private ProgressBar progressBar;

    private ApiService apiService;
    private String currentTermsId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_terms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ View
        tvTitle = view.findViewById(R.id.tvTitle);
        tvTermsContent = view.findViewById(R.id.tvTermsContent);
        cbCheckpoint1 = view.findViewById(R.id.cbCheckpoint1);
        cbCheckpoint2 = view.findViewById(R.id.cbCheckpoint2);
        btnAcceptAndContinue = view.findViewById(R.id.btnAcceptAndContinue);
        progressBar = view.findViewById(R.id.progressBar);

        // Khởi tạo ApiService chuẩn theo cấu trúc dự án của bạn
        apiService = ApiClient.getApiService();

        // 2. Lắng nghe sự kiện Checkbox
        View.OnClickListener checkboxListener = v -> {
            boolean isBothChecked = cbCheckpoint1.isChecked() && cbCheckpoint2.isChecked();
            btnAcceptAndContinue.setEnabled(isBothChecked);
        };
        cbCheckpoint1.setOnClickListener(checkboxListener);
        cbCheckpoint2.setOnClickListener(checkboxListener);

        // 3. Lắng nghe sự kiện Nút bấm
        btnAcceptAndContinue.setOnClickListener(v -> submitCreatorRegistration());

        // 4. Bắt đầu gọi API lấy dữ liệu điều khoản
        fetchActiveTerms();
    }

    /**
     * Hàm lấy JWT Token thực tế từ bộ nhớ mã hóa (Khớp 100% với LoginActivity)
     */
    private String getAuthToken() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    requireContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            String token = securePrefs.getString("ACCESS_TOKEN", "");

            // Backend Spring Boot yêu cầu JWT phải có chữ "Bearer " ở đầu
            if (!token.isEmpty() && !token.startsWith("Bearer ")) {
                token = "Bearer " + token;
            }
            return token;

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Lỗi khi lấy token bảo mật", e);
            return "";
        }
    }

    private void fetchActiveTerms() {
        showLoading(true);
        // Gọi API lấy điều khoản loại "CREATOR"
        apiService.getActiveTerms(getAuthToken(), "CREATOR").enqueue(new Callback<TermsResponse>() {
            @Override
            public void onResponse(Call<TermsResponse> call, Response<TermsResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    TermsResponse termsResponse = response.body();

                    if (termsResponse.getCode() == 200 && termsResponse.getData() != null) {
                        currentTermsId = termsResponse.getData().getId();
                        tvTermsContent.setText(termsResponse.getData().getContent());
                    } else if (termsResponse.getCode() == 4041) {
                        tvTermsContent.setText("Hệ thống đang cập nhật điều khoản. Vui lòng thử lại sau.");
                        cbCheckpoint1.setEnabled(false);
                        cbCheckpoint2.setEnabled(false);
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi tải điều khoản", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TermsResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitCreatorRegistration() {
        if (currentTermsId == null) {
            Toast.makeText(getContext(), "Chưa tải được Điều khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        CreatorRegisterRequest request = new CreatorRegisterRequest(currentTermsId);

        apiService.registerCreator(getAuthToken(), request).enqueue(new Callback<CreatorRegisterResponse>() {
            @Override
            public void onResponse(Call<CreatorRegisterResponse> call, Response<CreatorRegisterResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getCode() == 200) {
                        String kycSessionId = response.body().getKycSessionId();
                        Toast.makeText(getContext(), "Đăng ký thành công! Bắt đầu chụp ảnh...", Toast.LENGTH_SHORT).show();

                        // Chuyển sang màn hình Camera
                        navigateToCameraFragment(kycSessionId);

                    } else {
                        Toast.makeText(getContext(), "Lỗi: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<CreatorRegisterResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToCameraFragment(String kycSessionId) {
        // Lưu ý: Lớp CameraEkycFragment sẽ báo đỏ vì chúng ta chưa tạo nó.
        CameraEkycFragment cameraFragment = new CameraEkycFragment();

        Bundle bundle = new Bundle();
        bundle.putString("KYC_SESSION_ID", kycSessionId);
        cameraFragment.setArguments(bundle);

        // Tôi giả định FrameLayout chứa các fragment ở MainActivity của bạn có ID là fragment_container.
        // Nếu tên khác, hãy sửa lại R.id.fragment_container cho đúng.
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, cameraFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnAcceptAndContinue.setEnabled(!isLoading && cbCheckpoint1.isChecked() && cbCheckpoint2.isChecked());
    }
}