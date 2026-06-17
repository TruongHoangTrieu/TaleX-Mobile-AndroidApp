package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;

public class ReviewEkycDataFragment extends Fragment {

    private Bitmap capturedImage;
    private String idNumber, fullName, dob;
    private String kycSessionId;
    private String frontImagePath;

    // Constructor nhận dữ liệu (từ CameraEkycFragment truyền sang)
    public ReviewEkycDataFragment(Bitmap image, String idNumber, String fullName, String dob, String kycSessionId, String frontImagePath) {
        this.capturedImage = image;
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.dob = dob;
        this.kycSessionId = kycSessionId;
        this.frontImagePath = frontImagePath;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review_ekyc_data, container, false);

        // Ánh xạ View
        ImageView ivFrontId = view.findViewById(R.id.ivFrontId);
        TextView tvIdNumber = view.findViewById(R.id.tvIdNumber);
        TextView tvFullName = view.findViewById(R.id.tvFullName);
        TextView tvDob = view.findViewById(R.id.tvDob);
        View btnBack = view.findViewById(R.id.btnBack);
        View btnRetake = view.findViewById(R.id.btnRetake);
        View btnConfirm = view.findViewById(R.id.btnConfirm);

        // Hiển thị dữ liệu
        if (capturedImage != null) ivFrontId.setImageBitmap(capturedImage);
        tvIdNumber.setText(idNumber);
        tvFullName.setText(fullName);
        tvDob.setText(dob);

        // Xử lý nút bấm
        btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnRetake.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        
        btnConfirm.setOnClickListener(v -> {
            // Chuyển sang bước chụp mặt sau
            CameraEkycFragment cameraFragment = new CameraEkycFragment();
            Bundle args = new Bundle();
            args.putString("KYC_SESSION_ID", kycSessionId);
            args.putInt("CURRENT_STEP", 2);
            args.putString("FRONT_IMAGE_PATH", frontImagePath);
            cameraFragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, cameraFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}
