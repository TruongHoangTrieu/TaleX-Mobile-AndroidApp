package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
    private ScrollView scrollViewTerms;
    private MaterialButton btnAcceptAndContinue;
    private ProgressBar progressBar;
    private ImageView btnBack;

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
        scrollViewTerms = view.findViewById(R.id.scrollViewTerms); // Đã thêm ScrollView
        btnAcceptAndContinue = view.findViewById(R.id.btnAcceptAndContinue);
        progressBar = view.findViewById(R.id.progressBar);
        btnBack = view.findViewById(R.id.btnBack);

        // Khởi tạo ApiService
        apiService = ApiClient.getApiService();

        // 2. Lắng nghe sự kiện Cuộn (Scroll to bottom) thay cho Checkbox
        scrollViewTerms.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View child = scrollViewTerms.getChildAt(0);
            if (child != null) {
                // Tính toán khoảng cách từ đáy view con đến đáy của ScrollView
                int diff = (child.getBottom() - (scrollViewTerms.getHeight() + scrollViewTerms.getScrollY()));

                // Nếu diff <= 0 nghĩa là đã cuộn đến đáy
                if (diff <= 0 && currentTermsId != null) {
                    btnAcceptAndContinue.setEnabled(true);
                }
            }
        });

        // 3. Lắng nghe sự kiện Nút bấm
        btnAcceptAndContinue.setOnClickListener(v -> submitCreatorRegistration());

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }

        // 4. Bắt đầu gọi API lấy dữ liệu điều khoản
        fetchActiveTerms();
    }

    /**
     * Hàm lấy JWT Token thực tế từ bộ nhớ mã hóa
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
        apiService.getActiveTerms(getAuthToken(), "CREATOR").enqueue(new Callback<TermsResponse>() {
            @Override
            public void onResponse(Call<TermsResponse> call, Response<TermsResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    TermsResponse termsResponse = response.body();
                    int code = termsResponse.getCode();

                    if ((code == 200 || code == 0) && termsResponse.getData() != null) {
                        currentTermsId = termsResponse.getData().getId();
                        tvTermsContent.setText(termsResponse.getData().getContent());

                        // Kiểm tra an toàn: Nếu nội dung quá ngắn, không cần scroll vẫn bật nút
                        scrollViewTerms.post(() -> {
                            View child = scrollViewTerms.getChildAt(0);
                            if (child != null && child.getMeasuredHeight() <= scrollViewTerms.getHeight()) {
                                btnAcceptAndContinue.setEnabled(true);
                            }
                        });

                    } else if (code == 4041) {
                        tvTermsContent.setText("Hệ thống đang cập nhật điều khoản. Vui lòng thử lại sau.");
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
                    int code = response.body().getCode();

                    if (code == 200 || code == 201 || code == 0) {
                        String kycSessionId = response.body().getKycSessionId();
                        // Ẩn Toast "Đăng ký thành công" để trải nghiệm mượt hơn
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
        CameraEkycFragment cameraFragment = new CameraEkycFragment();

        Bundle bundle = new Bundle();
        bundle.putString("KYC_SESSION_ID", kycSessionId);
        cameraFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, cameraFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            btnAcceptAndContinue.setEnabled(false);
        }
        if (btnBack != null) btnBack.setEnabled(!isLoading);
    }
}