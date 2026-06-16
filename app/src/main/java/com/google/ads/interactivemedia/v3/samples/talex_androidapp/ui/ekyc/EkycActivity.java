package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;

public class EkycActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ekyc);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Ngay khi mở Activity, nhét TermsFragment vào cái khung fragment_container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TermsFragment())
                    .commit();
        }
    }
}