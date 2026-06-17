package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiService;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.EKycResultResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.utils.ImageCompressor;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraEkycFragment extends Fragment {

    private static final String TAG = "CameraEkycFragment";

    // UI
    private PreviewView viewFinder;
    private ImageView ivFreezeFrame; // Biến mới: Chứa ảnh đóng băng
    private TextView tvStepInstruction, tvSubInstruction;
    private View btnCapture;
    private View frameIdCard;
    private ProgressBar progressBar;
    private ImageView btnBack;

    // CameraX
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    // Logic State
    private String kycSessionId;
    private ApiService apiService;
    private int currentStep = 1;
    private Uri frontImageUri = null;

    // Launchers
    private ActivityResultLauncher<Intent> videoCaptureLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (getArguments() != null) {
            kycSessionId = getArguments().getString("KYC_SESSION_ID");
        }

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(requireContext(), "Cần cấp quyền Máy ảnh để thực hiện eKYC", Toast.LENGTH_LONG).show();
                        getParentFragmentManager().popBackStack();
                    }
                }
        );

        videoCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (ActivityResult result) -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        uploadLiveness(videoUri);
                    } else {
                        Toast.makeText(requireContext(), "Đã hủy quay video", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_ekyc, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewFinder = view.findViewById(R.id.viewFinder);
        ivFreezeFrame = view.findViewById(R.id.ivFreezeFrame); // Ánh xạ màn đóng băng
        tvStepInstruction = view.findViewById(R.id.tvStepInstruction);
        tvSubInstruction = view.findViewById(R.id.tvSubInstruction);
        btnCapture = view.findViewById(R.id.btnCapture);
        frameIdCard = view.findViewById(R.id.frameIdCard);
        progressBar = view.findViewById(R.id.progressBar);
        btnBack = view.findViewById(R.id.btnBack);

        apiService = ApiClient.getApiService();

        setupUIForStep(currentStep);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        btnCapture.setOnClickListener(v -> {
            if (currentStep == 1 || currentStep == 2) {
                takePhoto();
            } else if (currentStep == 3) {
                startVideoRecording();
            }
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void setupUIForStep(int step) {
        // Luôn giấu màn đóng băng khi bước sang một trạng thái mới để camera quay tiếp
        ivFreezeFrame.setVisibility(View.GONE);

        if (step == 1) {
            tvStepInstruction.setText("Bước 1/3: Mặt trước CCCD");
            tvSubInstruction.setText("Vui lòng căn chỉnh giấy tờ vào trong khung hình.");

            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_id_frame);
            ViewGroup.LayoutParams params = frameIdCard.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = (int) (220 * getResources().getDisplayMetrics().density);
            frameIdCard.setLayoutParams(params);

        } else if (step == 2) {
            tvStepInstruction.setText("Bước 2/3: Mặt sau CCCD");
            tvSubInstruction.setText("Lật mặt sau giấy tờ và giữ cố định thiết bị.");

        } else if (step == 3) {
            tvStepInstruction.setText("Bước 3/3: Xác thực khuôn mặt");
            tvSubInstruction.setText("Nhấn nút để quay video khuôn mặt (3-5 giây).");

            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_face_frame);
            ViewGroup.LayoutParams params = frameIdCard.getLayoutParams();
            int size = (int) (300 * getResources().getDisplayMetrics().density);
            params.width = size;
            params.height = size;
            frameIdCard.setLayoutParams(params);

            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Lỗi khởi tạo camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // ĐÓNG BĂNG HÌNH ẢNH MÀN HÌNH CAMERA NGAY LẬP TỨC
        Bitmap frozenBitmap = viewFinder.getBitmap();
        if (frozenBitmap != null) {
            ivFreezeFrame.setImageBitmap(frozenBitmap);
            ivFreezeFrame.setVisibility(View.VISIBLE);
        }

        File photoFile = new File(requireContext().getCacheDir(), "capture_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        showLoading(true);

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                if (currentStep == 1) {
                    frontImageUri = savedUri;
                    uploadFrontId(savedUri);
                } else if (currentStep == 2) {
                    uploadBackId(savedUri);
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE); // Gỡ đóng băng để user chụp lại
                Toast.makeText(requireContext(), "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startVideoRecording() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 5);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        videoCaptureLauncher.launch(intent);
    }

    private void uploadFrontId(Uri uri) {
        MultipartBody.Part imagePart = ImageCompressor.getMultipartFromUri(requireContext(), uri, "frontImage");
        if (imagePart == null) {
            showLoading(false);
            ivFreezeFrame.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "--> Đang gửi POST request tới FPT.AI (Upload Mặt Trước)");
        Log.d(TAG, "Session ID: " + kycSessionId);
        String token = getAuthToken();
        Log.d(TAG, "Token Length: " + (token != null ? token.length() : "NULL"));

        apiService.uploadFrontId(token, kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    int code = response.body().getCode();
                    if (code == 200 || code == 201 || code == 0) {
                        currentStep = 2;
                        setupUIForStep(currentStep); // Gọi hàm này sẽ tự động giấu ivFreezeFrame đi
                        Toast.makeText(requireContext(), "Xong mặt trước!", Toast.LENGTH_SHORT).show();
                    } else {
                        ivFreezeFrame.setVisibility(View.GONE); // Thất bại -> gỡ đóng băng
                        Toast.makeText(requireContext(), "Lỗi: " + response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    ivFreezeFrame.setVisibility(View.GONE); // Thất bại -> gỡ đóng băng
                    Toast.makeText(requireContext(), "CCCD không hợp lệ hoặc đã tồn tại.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE); // Lỗi mạng -> gỡ đóng băng để thử lại
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadBackId(Uri uri) {
        MultipartBody.Part imagePart = ImageCompressor.getMultipartFromUri(requireContext(), uri, "backImage");
        if (imagePart == null) {
            showLoading(false);
            ivFreezeFrame.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "--> Đang gửi POST request tới FPT.AI (Upload Mặt Sau)");
        Log.d(TAG, "Session ID: " + kycSessionId);

        apiService.uploadBackId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    int code = response.body().getCode();
                    if (code == 200 || code == 201 || code == 0) {
                        currentStep = 3;
                        setupUIForStep(currentStep);
                        Toast.makeText(requireContext(), "Xong mặt sau! Chuẩn bị quay video.", Toast.LENGTH_SHORT).show();
                    } else {
                        ivFreezeFrame.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Lỗi: " + response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    ivFreezeFrame.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ảnh mặt sau không hợp lệ.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadLiveness(Uri videoUri) {
        showLoading(true);

        MultipartBody.Part cmndPart = ImageCompressor.getMultipartFromUri(requireContext(), frontImageUri, "cmnd");
        MultipartBody.Part videoPart = getVideoMultipart(videoUri);

        if (cmndPart == null || videoPart == null) {
            showLoading(false);
            Toast.makeText(requireContext(), "Lỗi trích xuất file Media", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "--> Đang gửi POST request tới FPT.AI (Upload Liveness Video)");
        Log.d(TAG, "Session ID: " + kycSessionId);

        apiService.verifyLiveness(getAuthToken(), kycSessionId, videoPart, cmndPart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    int code = response.body().getCode();
                    if (code == 200 || code == 201 || code == 0) {
                        Toast.makeText(requireContext(), "eKYC THÀNH CÔNG!", Toast.LENGTH_LONG).show();
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Lỗi: " + response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Xác thực khuôn mặt thất bại.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private MultipartBody.Part getVideoMultipart(Uri videoUri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(videoUri);
            File tempFile = new File(requireContext().getCacheDir(), "liveness_video.mp4");
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();
            RequestBody reqFile = RequestBody.create(MediaType.parse("video/mp4"), tempFile);
            return MultipartBody.Part.createFormData("video", tempFile.getName(), reqFile);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi sao chép video", e);
            return null;
        }
    }

    private String getAuthToken() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref", masterKeyAlias, requireContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            String token = securePrefs.getString("ACCESS_TOKEN", "");
            return (!token.isEmpty() && !token.startsWith("Bearer ")) ? "Bearer " + token : token;
        } catch (GeneralSecurityException | IOException e) {
            return "";
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCapture.setEnabled(!isLoading);
        btnBack.setEnabled(!isLoading);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}