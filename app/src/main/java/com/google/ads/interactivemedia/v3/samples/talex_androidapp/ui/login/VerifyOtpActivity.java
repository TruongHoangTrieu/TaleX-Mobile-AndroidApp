package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;

import android.text.TextWatcher;
import android.view.KeyEvent;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.VerifyEmailRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.ResendOtpRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.RegisterResponse; // ◄ Đã bổ sung dòng import chí mạng này
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.MainActivity;

import java.io.IOException;
import java.security.GeneralSecurityException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button btnVerify;
    private TextView txtResend, txtEmail;
    private String verificationToken;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        verificationToken = getIntent().getStringExtra("VERIFICATION_TOKEN");
        String email = getIntent().getStringExtra("EMAIL");

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);

        setupOtpInputLogic(otp1, otp2, null);
        setupOtpInputLogic(otp2, otp3, otp1);
        setupOtpInputLogic(otp3, otp4, otp2);
        setupOtpInputLogic(otp4, otp5, otp3);
        setupOtpInputLogic(otp5, otp6, otp4);
        setupOtpInputLogic(otp6, null, otp5);

        btnVerify = findViewById(R.id.btnVerify);
        txtResend = findViewById(R.id.txtResend);
        txtEmail = findViewById(R.id.txtEmail);
        ImageView btnBack = findViewById(R.id.btnBack);

        if (email != null) {
            txtEmail.setText(email);
        }
        otp1.requestFocus();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        startCountdown();

        btnVerify.setOnClickListener(v -> {
            String otp = otp1.getText().toString()
                    + otp2.getText().toString()
                    + otp3.getText().toString()
                    + otp4.getText().toString()
                    + otp5.getText().toString()
                    + otp6.getText().toString();

            if (otp.length() == 6) {
                executeVerifyEmailApi(verificationToken, otp);
            } else {
                Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP!", Toast.LENGTH_SHORT).show();
            }
        });

        txtResend.setOnClickListener(v -> {
            if (!txtResend.isEnabled()) return;
            executeResendOtpApi();
        });
    }

    private void executeVerifyEmailApi(String token, String otp) {
        btnVerify.setEnabled(false);
        btnVerify.setText("ĐANG KIỂM TRA...");

        VerifyEmailRequest request = new VerifyEmailRequest(token, otp);

        ApiClient.getApiService().verifyEmail(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnVerify.setEnabled(true);
                btnVerify.setText("XÁC MINH NGAY");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {
                        saveTokens(loginResponse.getData().getAccessToken(), loginResponse.getData().getRefreshToken());
                        Toast.makeText(VerifyOtpActivity.this, "Kích hoạt tài khoản thành công!", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(VerifyOtpActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(VerifyOtpActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(VerifyOtpActivity.this, "Mã OTP không đúng hoặc đã hết hạn!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                btnVerify.setText("XÁC MINH NGAY");
                Toast.makeText(VerifyOtpActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executeResendOtpApi() {
        if (verificationToken == null) return;

        txtResend.setEnabled(false);
        ResendOtpRequest request = new ResendOtpRequest(verificationToken);

        ApiClient.getApiService().resendOtp(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(VerifyOtpActivity.this, "Mã OTP mới đã được gửi về Email!", Toast.LENGTH_SHORT).show();
                    startCountdown();
                } else {
                    txtResend.setEnabled(true);
                    Toast.makeText(VerifyOtpActivity.this, "Gửi lại OTP thất bại! Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                txtResend.setEnabled(true);
                Toast.makeText(VerifyOtpActivity.this, "Lỗi mạng, không thể kết nối hệ thống!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCountdown() {
        txtResend.setEnabled(false);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                txtResend.setText(String.format("Gửi lại sau 00:%02d", seconds));
            }

            @Override
            public void onFinish() {
                txtResend.setEnabled(true);
                txtResend.setText("Gửi lại mã");
            }
        };
        countDownTimer.start();
    }

    private void saveTokens(String access, String refresh) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("ACCESS_TOKEN", access);
            editor.putString("REFRESH_TOKEN", refresh);
            editor.apply();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi bảo mật khi lưu Token!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupOtpInputLogic(EditText current, EditText next, EditText previous) {
        current.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) {
                    next.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        current.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (current.getText().toString().isEmpty() && previous != null) {
                    previous.requestFocus();
                    previous.setText("");
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}