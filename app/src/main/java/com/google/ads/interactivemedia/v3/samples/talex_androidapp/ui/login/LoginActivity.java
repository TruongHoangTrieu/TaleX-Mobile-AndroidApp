package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View; // 🟢 Đã bảo đảm có import này
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.ProgressBar; // 🟢 Đã bảo đảm có import này
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.BuildConfig;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.MainActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.GoogleLoginRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.GoogleLoginResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginRequest;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.LoginResponse;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText edtEmail, edtPassword;
    private Button btnLoginSubmit;
    private TextView txtGotoRegister;
    private CardView cardGg;
    private ImageView btnBack;
    private ProgressBar pbLoginLoading;
    private TextView tvErrorMessage;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ thành phần giao diện
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnLoginSubmit = findViewById(R.id.btn_login_submit);
        pbLoginLoading = findViewById(R.id.pb_login_loading);
        cardGg = findViewById(R.id.card_social_gg);
        btnBack = findViewById(R.id.btn_login_back);
        txtGotoRegister = findViewById(R.id.txt_goto_register);
        tvErrorMessage = findViewById(R.id.tv_error_message);

        // 2. Cấu hình Google Sign-In
        setupGoogleSignIn();

        // 3. Xử lý sự kiện Đăng nhập email/password
        if (btnLoginSubmit != null) {
            btnLoginSubmit.setOnClickListener(v -> {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                boolean isEmailEmpty = TextUtils.isEmpty(email);
                boolean isPasswordEmpty = TextUtils.isEmpty(password);

                if (isEmailEmpty && isPasswordEmpty) {
                    // Trường hợp 1: Trống cả hai ô -> Cho đỏ cả hai
                    triggerErrorState("Vui lòng nhập tài khoản và mật khẩu!", true, true);
                } else if (isEmailEmpty) {
                    // Trường hợp 2: Chỉ trống Email -> Chỉ đỏ ô Email, ô mật khẩu bình thường
                    triggerErrorState("Vui lòng nhập email của bạn!", true, false);
                    if (edtEmail != null) edtEmail.requestFocus();
                } else if (isPasswordEmpty) {
                    // Trường hợp 3: Chỉ trống Mật khẩu -> Chỉ đỏ ô Mật khẩu, ô Email bình thường
                    triggerErrorState("Vui lòng nhập mật khẩu của bạn!", false, true);
                    if (edtPassword != null) edtPassword.requestFocus();
                } else {
                    // Đã điền đầy đủ -> Tiến hành gọi API
                    executeLoginApi(email, password);
                }
            });
        }

        // 4. Đăng nhập bằng Google
        if (cardGg != null) {
            cardGg.setOnClickListener(v -> {
                showLoadingState(true); // 🟢 Bật xoay Spinner khi bắt đầu chọn tài khoản Google
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        }

        // 5. Nút Back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // 6. Chuyển sang trang Đăng Ký
        if (txtGotoRegister != null) {
            String customText = "Chưa có tài khoản? <font color='#A52A2A'><b>Đăng ký</b></font>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                txtGotoRegister.setText(android.text.Html.fromHtml(customText, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                txtGotoRegister.setText(android.text.Html.fromHtml(customText));
            }

            txtGotoRegister.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }

        // 7. Logic ẩn/hiện mật khẩu
        final boolean[] isPasswordVisible = {false};
        ImageView imgEye = findViewById(R.id.img_eye);

        if (imgEye != null && edtPassword != null) {
            imgEye.setOnClickListener(v -> {
                if (isPasswordVisible[0]) {
                    edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgEye.setImageResource(R.drawable.ic_eye_close);
                    isPasswordVisible[0] = false;
                } else {
                    edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgEye.setImageResource(R.drawable.ic_eye_open);
                    isPasswordVisible[0] = true;
                }
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }
        // Tự động xóa trạng thái lỗi khi người dùng bắt đầu nhập liệu lại
        android.text.TextWatcher textWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Khi người dùng gõ chữ, ẩn dòng lỗi và trả lại viền xám mặc định
                if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
                if (edtEmail != null) edtEmail.setBackgroundResource(R.drawable.bg_input_rounded);
                if (edtPassword != null) {
                    View passwordWrapper = (View) edtPassword.getParent();
                    if (passwordWrapper != null) {
                        passwordWrapper.setBackgroundResource(R.drawable.bg_input_rounded);
                    }
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };

// Áp dụng bộ lắng nghe gõ chữ cho cả 2 ô
        if (edtEmail != null) edtEmail.addTextChangedListener(textWatcher);
        if (edtPassword != null) edtPassword.addTextChangedListener(textWatcher);
    }

    // ── Google Sign-In Setup ────────────────────────────────────

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut();

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        showLoadingState(false); // 🟢 Tắt xoay Spinner nếu hủy chọn tài khoản Google
                        Log.w(TAG, "Google sign-in cancelled or failed, resultCode=" + result.getResultCode());
                    }
                }
        );
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();

            if (idToken == null) {
                showLoadingState(false);
                Toast.makeText(this, "Không lấy được Google token!", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Google ID Token received, sending to backend...");
            executeGoogleLoginApi(idToken);

        } catch (ApiException e) {
            showLoadingState(false);
            Log.e(TAG, "Google sign-in failed, code=" + e.getStatusCode(), e);
            Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
        }
    }

    // ── API: Google Login ───────────────────────────────────────

    private void executeGoogleLoginApi(String idToken) {
        GoogleLoginRequest request = new GoogleLoginRequest(idToken);

        ApiClient.getApiService().googleLogin(request).enqueue(new Callback<GoogleLoginResponse>() {
            @Override
            public void onResponse(Call<GoogleLoginResponse> call, Response<GoogleLoginResponse> response) {
                showLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    GoogleLoginResponse googleResponse = response.body();

                    if (googleResponse.isSuccess() && googleResponse.getData() != null) {
                        GoogleLoginResponse.GoogleLoginData data = googleResponse.getData();
                        String status = data.getStatus();

                        if ("ACTIVE".equals(status)) {
                            saveTokens(data.getAccessToken(), data.getRefreshToken());
                            navigateToMain();
                        } else if ("ONBOARDING".equals(status)) {
                            Toast.makeText(LoginActivity.this, "Vui lòng hoàn tất thông tin cá nhân", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, CompleteProfileActivity.class);
                            intent.putExtra("VERIFICATION_TOKEN", data.getVerificationToken());
                            startActivity(intent);
                            finish();
                        } else if ("VERIFYING".equals(status)) {
                            Toast.makeText(LoginActivity.this, "Tài khoản đang chờ xác thực email", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, googleResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GoogleLoginResponse> call, Throwable t) {
                showLoadingState(false);
                Log.e(TAG, "Google login API error", t);
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── API: Email/Password Login ───────────────────────────────

    private void executeLoginApi(String email, String password) {
        showLoadingState(true);

        LoginRequest loginRequest = new LoginRequest(email, password);

        ApiClient.getApiService().loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {
                        saveTokens(loginResponse.getData().getAccessToken(), loginResponse.getData().getRefreshToken());
                        navigateToMain();
                    } else {
                        triggerErrorState(loginResponse.getMessage(), true, true);
                    }
                } else {
                    triggerErrorState("Tài khoản hoặc mật khẩu không chính xác!", true, true);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoadingState(false); // 🟢 Đã sửa: Đồng bộ tắt xoay Spinner thay vì dùng lệnh thủ công cũ
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── 🟢 HÀM BỔ TRỢ: QUẢN LÝ TRẠNG THÁI LOADING (ĐÃ BỔ SUNG TRỌN VẸN) ──
    private void showLoadingState(boolean isLoading) {
        if (isLoading) {
            if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
            if (edtEmail != null) edtEmail.setBackgroundResource(R.drawable.bg_input_rounded);
            if (edtPassword != null) {
                View passwordWrapper = (View) edtPassword.getParent();
                if (passwordWrapper != null) {
                    passwordWrapper.setBackgroundResource(R.drawable.bg_input_rounded);
                }
            }

            if (btnLoginSubmit != null) {
                btnLoginSubmit.setText("");
                btnLoginSubmit.setEnabled(false);
            }
            if (pbLoginLoading != null) {
                pbLoginLoading.setVisibility(View.VISIBLE);
            }
        } else {
            if (btnLoginSubmit != null) {
                btnLoginSubmit.setText("ĐĂNG NHẬP");
                btnLoginSubmit.setEnabled(true);
            }
            if (pbLoginLoading != null) {
                pbLoginLoading.setVisibility(View.GONE);
            }
        }
    }
    private void triggerErrorState(String errorMessage, boolean makeEmailRed, boolean makePasswordRed) {
        // 1. Hiển thị dòng chữ thông báo lỗi chung
        if (tvErrorMessage != null) {
            tvErrorMessage.setText(errorMessage);
            tvErrorMessage.setVisibility(View.VISIBLE);
        }

        // 2. Xử lý viền ô Email
        if (edtEmail != null) {
            if (makeEmailRed) {
                edtEmail.setBackgroundResource(R.drawable.bg_input_error); // Hiện viền đỏ
            } else {
                edtEmail.setBackgroundResource(R.drawable.bg_input_rounded); // Giữ viền xám/gold bình thường
            }
        }

        // 3. Xử lý viền khối bọc Mật khẩu
        if (edtPassword != null) {
            View passwordWrapper = (View) edtPassword.getParent();
            if (passwordWrapper != null) {
                if (makePasswordRed) {
                    passwordWrapper.setBackgroundResource(R.drawable.bg_input_error); // Hiện viền đỏ
                } else {
                    passwordWrapper.setBackgroundResource(R.drawable.bg_input_rounded); // Giữ viền xám/gold bình thường
                }
            }
        }
    }
    // ── Token Storage ───────────────────────────────────────────

    private void saveTokens(String accessToken, String refreshToken) {
        try {
            String masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(
                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
            );

            SharedPreferences securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    LoginActivity.this,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = securePrefs.edit();
            editor.putString("ACCESS_TOKEN", accessToken);
            editor.putString("REFRESH_TOKEN", refreshToken);
            editor.apply();

        } catch (java.security.GeneralSecurityException | java.io.IOException e) {
            Log.e(TAG, "Error saving tokens", e);
            Toast.makeText(this, "Lỗi mã hóa dữ liệu đăng nhập!", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Navigation ──────────────────────────────────────────────

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}