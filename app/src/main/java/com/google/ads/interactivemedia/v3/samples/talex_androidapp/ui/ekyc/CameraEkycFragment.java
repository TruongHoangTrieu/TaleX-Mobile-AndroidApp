package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
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
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
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
    private ImageView ivFreezeFrame;
    private TextView tvStepInstruction, tvSubInstruction, tvTimer;
    private View btnCapture;
    private View frameIdCard;
    private ProgressBar progressBar;
    private ImageView btnBack;

    // CameraX Core
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    // CameraX UseCases
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording = null;
    private CountDownTimer countDownTimer;

    // Logic State
    private String kycSessionId;
    private ApiService apiService;
    private int currentStep = 1;
    private File frontCroppedFile = null;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (getArguments() != null) {
            kycSessionId = getArguments().getString("KYC_SESSION_ID");
            currentStep = getArguments().getInt("CURRENT_STEP", 1);
            String frontPath = getArguments().getString("FRONT_IMAGE_PATH");
            if (frontPath != null) {
                frontCroppedFile = new File(frontPath);
            }
        }

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean audioGranted = permissions.getOrDefault(Manifest.permission.RECORD_AUDIO, false);

                    if (cameraGranted && audioGranted) {
                        startCameraForCurrentStep();
                    } else {
                        Toast.makeText(requireContext(), "Cần cấp quyền Máy ảnh và Ghi âm để tiếp tục", Toast.LENGTH_LONG).show();
                        getParentFragmentManager().popBackStack();
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
        ivFreezeFrame = view.findViewById(R.id.ivFreezeFrame);
        tvStepInstruction = view.findViewById(R.id.tvStepInstruction);
        tvSubInstruction = view.findViewById(R.id.tvSubInstruction);
        tvTimer = view.findViewById(R.id.tvTimer);
        btnCapture = view.findViewById(R.id.btnCapture);
        frameIdCard = view.findViewById(R.id.frameIdCard);
        progressBar = view.findViewById(R.id.progressBar);
        btnBack = view.findViewById(R.id.btnBack);

        apiService = ApiClient.getApiService();

        setupUIForStep(currentStep);

        if (allPermissionsGranted()) {
            startCameraForCurrentStep();
        } else {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            });
        }

        btnCapture.setOnClickListener(v -> {
            if (currentStep == 1 || currentStep == 2) {
                takePhoto();
            } else if (currentStep == 3) {
                captureVideo();
            }
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupUIForStep(int step) {
        ivFreezeFrame.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);

        if (step == 1) {
            tvStepInstruction.setText("Mặt trước CCCD");
            tvSubInstruction.setText("Vui lòng căn chỉnh giấy tờ vào trong khung hình.");

            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_id_frame);
            ViewGroup.LayoutParams params = frameIdCard.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = (int) (220 * getResources().getDisplayMetrics().density);
            frameIdCard.setLayoutParams(params);

        } else if (step == 2) {
            tvStepInstruction.setText("Mặt sau CCCD");
            tvSubInstruction.setText("Lật mặt sau giấy tờ và giữ cố định thiết bị.");

            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_id_frame);
            ViewGroup.LayoutParams params = frameIdCard.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = (int) (220 * getResources().getDisplayMetrics().density);
            frameIdCard.setLayoutParams(params);

        } else if (step == 3) {
            tvStepInstruction.setText("Xác thực Khuôn mặt");
            tvSubInstruction.setText("Nhấn chụp để tự động ghi hình trong 5 giây.");

            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_face_oval);
            ViewGroup.LayoutParams params = frameIdCard.getLayoutParams();
            int width = (int) (240 * getResources().getDisplayMetrics().density);
            int height = (int) (320 * getResources().getDisplayMetrics().density);
            params.width = width;
            params.height = height;
            frameIdCard.setLayoutParams(params);
        }
    }

    private void startCameraForCurrentStep() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                if (currentStep == 1 || currentStep == 2) {
                    imageCapture = new ImageCapture.Builder().build();
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                    cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

                } else if (currentStep == 3) {
                    Recorder recorder = new Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.FHD))
                            .build();
                    videoCapture = VideoCapture.withOutput(recorder);
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, videoCapture);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Lỗi khởi tạo camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

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
                    uploadFrontId(savedUri);
                } else if (currentStep == 2) {
                    uploadBackId(savedUri);
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (!isAdded()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void captureVideo() {
        if (videoCapture == null || currentRecording != null) return;

        File videoFile = new File(requireContext().getCacheDir(), "liveness_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();

        try {
            currentRecording = videoCapture.getOutput()
                    .prepareRecording(requireContext(), outputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(requireContext()), videoRecordEvent -> {
                        if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                            tvSubInstruction.setText("Vui lòng nhìn thẳng và quay mặt sang trái/phải.");
                            btnCapture.setVisibility(View.INVISIBLE);
                            tvTimer.setVisibility(View.VISIBLE);
                            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_face_oval_success);

                            countDownTimer = new CountDownTimer(6000, 1000) {
                                public void onTick(long millisUntilFinished) {
                                    tvTimer.setText("00:0" + (millisUntilFinished / 1000));
                                }
                                public void onFinish() {
                                    if (currentRecording != null) {
                                        currentRecording.stop();
                                        currentRecording = null;
                                    }
                                }
                            }.start();

                        } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                            VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;

                            btnCapture.setVisibility(View.VISIBLE);
                            tvTimer.setVisibility(View.GONE);
                            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_face_oval);
                            tvSubInstruction.setText("Đang phân tích dữ liệu khuôn mặt...");

                            if (!finalizeEvent.hasError()) {
                                uploadLiveness(videoFile);
                            } else {
                                currentRecording = null;
                                Toast.makeText(requireContext(), "Lỗi ghi hình, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Lỗi quyền truy cập Microphone", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFrontId(Uri uri) {
        int pW = viewFinder.getWidth();
        int pH = viewFinder.getHeight();
        float fX = frameIdCard.getX();
        float fY = frameIdCard.getY();
        float fW = frameIdCard.getWidth();
        float fH = frameIdCard.getHeight();

        frontCroppedFile = ImageCompressor.processAndCropImage(requireContext(), uri, "frontImage", pW, pH, fX, fY, fW, fH);
        MultipartBody.Part imagePart = ImageCompressor.buildMultipart("frontImage", frontCroppedFile);

        if (imagePart == null) {
            showLoading(false);
            ivFreezeFrame.setVisibility(View.GONE);
            return;
        }

        apiService.uploadFrontId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (!isAdded()) return;
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    EKycResultResponse result = response.body();
                    EKycResultResponse.EKycData data = result.getData();

                    if (data != null && data.isSuccess()) {
                        String idNum = "N/A", name = "N/A", dob = "N/A";
                        try {
                            if (data.getRawResponse() != null && data.getRawResponse().isJsonObject()) {
                                JsonObject json = data.getRawResponse().getAsJsonObject();
                                if (json.has("id")) idNum = json.get("id").getAsString();
                                if (json.has("name")) name = json.get("name").getAsString();
                                if (json.has("dob")) dob = json.get("dob").getAsString();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "OCR Parse Error", e);
                        }

                        // Hiển thị Review Fragment
                        Bitmap bitmap = BitmapFactory.decodeFile(frontCroppedFile.getAbsolutePath());
                        ReviewEkycDataFragment reviewFragment = new ReviewEkycDataFragment(
                                bitmap, idNum, name, dob, kycSessionId, frontCroppedFile.getAbsolutePath()
                        );

                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, reviewFragment)
                                .addToBackStack(null)
                                .commit();

                    } else {
                        ivFreezeFrame.setVisibility(View.GONE);
                        String errorMsg = (data != null && data.getMessage() != null) ? data.getMessage() : result.getMessage();
                        Toast.makeText(getContext(), "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    ivFreezeFrame.setVisibility(View.GONE);
                    Toast.makeText(getContext(), parseErrorBody(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadBackId(Uri uri) {
        int pW = viewFinder.getWidth();
        int pH = viewFinder.getHeight();
        float fX = frameIdCard.getX();
        float fY = frameIdCard.getY();
        float fW = frameIdCard.getWidth();
        float fH = frameIdCard.getHeight();

        File backCroppedFile = ImageCompressor.processAndCropImage(requireContext(), uri, "backImage", pW, pH, fX, fY, fW, fH);
        MultipartBody.Part imagePart = ImageCompressor.buildMultipart("backImage", backCroppedFile);

        if (imagePart == null) {
            showLoading(false);
            ivFreezeFrame.setVisibility(View.GONE);
            return;
        }

        apiService.uploadBackId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (!isAdded()) return;
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    EKycResultResponse result = response.body();
                    EKycResultResponse.EKycData data = result.getData();

                    if (data != null && data.isSuccess()) {
                        currentStep = 3;
                        setupUIForStep(currentStep);
                        startCameraForCurrentStep();
                        Toast.makeText(getContext(), "Xong mặt sau! Chuẩn bị quay video.", Toast.LENGTH_SHORT).show();
                    } else {
                        ivFreezeFrame.setVisibility(View.GONE);
                        String errorMsg = (data != null && data.getMessage() != null) ? data.getMessage() : result.getMessage();
                        Toast.makeText(getContext(), "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    ivFreezeFrame.setVisibility(View.GONE);
                    Toast.makeText(getContext(), parseErrorBody(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadLiveness(File videoFile) {
        showLoading(true);

        MultipartBody.Part cmndPart = ImageCompressor.buildMultipart("cmnd", frontCroppedFile);
        RequestBody reqFile = RequestBody.create(MediaType.parse("video/mp4"), videoFile);
        MultipartBody.Part videoPart = MultipartBody.Part.createFormData("video", videoFile.getName(), reqFile);

        if (cmndPart == null || videoPart == null) {
            showLoading(false);
            Toast.makeText(requireContext(), "Lỗi trích xuất file Media", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.verifyLiveness(getAuthToken(), kycSessionId, videoPart, cmndPart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (!isAdded()) return;
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    EKycResultResponse result = response.body();
                    EKycResultResponse.EKycData data = result.getData();

                    if (data != null && data.isSuccess()) {
                        Toast.makeText(getContext(), "eKYC THÀNH CÔNG TỐT ĐẸP!", Toast.LENGTH_LONG).show();
                        if (getActivity() != null) ((EkycActivity) getActivity()).finishWithSuccess();
                    } else {
                        String errorMsg = (data != null && data.getMessage() != null) ? data.getMessage() : result.getMessage();
                        Toast.makeText(getContext(), "Lỗi Khuôn mặt: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getContext(), parseErrorBody(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

    private String parseErrorBody(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                JSONObject jsonObject = new JSONObject(errorStr);
                if (jsonObject.has("message")) return jsonObject.getString("message");
            }
        } catch (Exception ignored) {}
        return "Dữ liệu không hợp lệ. Vui lòng thực hiện lại.";
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCapture.setEnabled(!isLoading);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (currentRecording != null) currentRecording.stop();
        cameraExecutor.shutdown();
    }
}
