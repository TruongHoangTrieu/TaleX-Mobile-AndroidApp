package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
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
        cardGg = findViewById(R.id.card_social_gg);
        btnBack = findViewById(R.id.btn_login_back);
        txtGotoRegister = findViewById(R.id.txt_goto_register);

        // 2. Cấu hình Google Sign-In
        setupGoogleSignIn();

        // 3. Xử lý sự kiện Đăng nhập email/password
        if (btnLoginSubmit != null) {
            btnLoginSubmit.setOnClickListener(v -> {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ tài khoản & mật khẩu!", Toast.LENGTH_SHORT).show();
                } else {
                    executeLoginApi(email, password);
                }
            });
        }

        // 4. Đăng nhập bằng Google
        if (cardGg != null) {
            cardGg.setOnClickListener(v -> {
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
    }

    // ── Google Sign-In Setup ────────────────────────────────────

    private void setupGoogleSignIn() {
        // serverClientId = Web Client ID (BE dùng để verify ID token)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Sign out trước để luôn hiện popup chọn account
        googleSignInClient.signOut();

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
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
                Toast.makeText(this, "Không lấy được Google token!", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Google ID Token received, sending to backend...");
            executeGoogleLoginApi(idToken);

        } catch (ApiException e) {
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
                if (response.isSuccessful() && response.body() != null) {
                    GoogleLoginResponse googleResponse = response.body();

                    if (googleResponse.isSuccess() && googleResponse.getData() != null) {
                        GoogleLoginResponse.GoogleLoginData data = googleResponse.getData();
                        String status = data.getStatus();

                        if ("ACTIVE".equals(status)) {
                            // User đã có account → lưu token → vào app
                            saveTokens(data.getAccessToken(), data.getRefreshToken());
                            Toast.makeText(LoginActivity.this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else if ("ONBOARDING".equals(status)) {
                            // User mới → cần complete profile (phone + dateOfBirth)
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
                Log.e(TAG, "Google login API error", t);
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── API: Email/Password Login ───────────────────────────────

    private void executeLoginApi(String email, String password) {
        btnLoginSubmit.setEnabled(false);
        btnLoginSubmit.setText("ĐANG XỬ LÝ...");

        LoginRequest loginRequest = new LoginRequest(email, password);

        ApiClient.getApiService().loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLoginSubmit.setEnabled(true);
                btnLoginSubmit.setText("ĐĂNG NHẬP");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {
                        saveTokens(loginResponse.getData().getAccessToken(), loginResponse.getData().getRefreshToken());
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLoginSubmit.setEnabled(true);
                btnLoginSubmit.setText("ĐĂNG NHẬP");
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
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
