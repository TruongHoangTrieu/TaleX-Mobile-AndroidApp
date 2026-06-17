package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;

import java.io.File;

public class ReviewEkycDataFragment extends Fragment {

    public static final String REQUEST_REVIEW_CONFIRMED = "review_ekyc_confirmed";

    private static final String ARG_FRONT_IMAGE_PATH = "FRONT_IMAGE_PATH";
    private static final String ARG_ID_NUMBER = "ID_NUMBER";
    private static final String ARG_FULL_NAME = "FULL_NAME";
    private static final String ARG_DOB = "DOB";

    public static ReviewEkycDataFragment newInstance(String frontImagePath, String idNumber, String fullName, String dob) {
        ReviewEkycDataFragment fragment = new ReviewEkycDataFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FRONT_IMAGE_PATH, frontImagePath);
        args.putString(ARG_ID_NUMBER, idNumber);
        args.putString(ARG_FULL_NAME, fullName);
        args.putString(ARG_DOB, dob);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_ekyc_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivFrontId = view.findViewById(R.id.ivFrontId);
        TextView tvIdNumber = view.findViewById(R.id.tvIdNumber);
        TextView tvFullName = view.findViewById(R.id.tvFullName);
        TextView tvDob = view.findViewById(R.id.tvDob);
        View btnBack = view.findViewById(R.id.btnBack);
        View btnRetake = view.findViewById(R.id.btnRetake);
        View btnConfirm = view.findViewById(R.id.btnConfirm);

        Bundle args = getArguments();
        if (args != null) {
            String frontImagePath = args.getString(ARG_FRONT_IMAGE_PATH);
            if (frontImagePath != null) {
                ivFrontId.setImageURI(Uri.fromFile(new File(frontImagePath)));
            }

            tvIdNumber.setText(valueOrDash(args.getString(ARG_ID_NUMBER)));
            tvFullName.setText(valueOrDash(args.getString(ARG_FULL_NAME)));
            tvDob.setText(valueOrDash(args.getString(ARG_DOB)));
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnRetake.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnConfirm.setOnClickListener(v -> {
            getParentFragmentManager().setFragmentResult(REQUEST_REVIEW_CONFIRMED, new Bundle());
            getParentFragmentManager().popBackStack();
        });
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "--";
        }
        return value;
    }
}
