package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.app.Activity;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;

public class EkycActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ekyc);

        // Đăng ký OnBackPressedDispatcher chuẩn AndroidX thay cho onBackPressed() cũ
        // Xử lý sự kiện khi người dùng vuốt từ cạnh màn hình hoặc bấm nút Back cứng
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });

        // Ngay khi mở Activity, load TermsFragment vào fragment_container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TermsFragment())
                    .commit();
        }
    }

    /**
     * Xử lý nút Back thông minh:
     * Nếu đang ở màn hình Camera -> Lùi về màn hình Điều khoản.
     * Nếu đang ở màn hình Điều khoản -> Thoát Activity eKYC.
     */
    private void handleBackPress() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    /**
     * Hàm này được gọi khi luồng Liveness (quét khuôn mặt) báo thành công.
     * Nó sẽ gửi tín hiệu RESULT_OK về cho màn hình trước đó (Profile/Home) để tự động cập nhật UI.
     */
    public void finishWithSuccess() {
        setResult(Activity.RESULT_OK);
        finish();
    }
}