package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.dailyreward;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;

public class MissionCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.daily_reward);

        ImageButton btnBack = findViewById(R.id.btn_back_mission);
        Button btnClaim = findViewById(R.id.btn_claim_daily);

        btnBack.setOnClickListener(v -> finish());

        btnClaim.setOnClickListener(v -> {
            Toast.makeText(this, "Điểm danh thành công! +10 xu đã được cộng.", Toast.LENGTH_SHORT).show();
        });
    }
}