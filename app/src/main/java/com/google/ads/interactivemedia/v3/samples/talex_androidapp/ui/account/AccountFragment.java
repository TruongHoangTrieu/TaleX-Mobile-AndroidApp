package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;

public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Ánh xạ an toàn
        Button btnLogout = view.findViewById(R.id.btn_logout);
        Button btnUpgrade = view.findViewById(R.id.btn_upgrade);
        TextView btnChangePassword = view.findViewById(R.id.btn_change_password);
        TextView btnHistory = view.findViewById(R.id.btn_history);
        TextView btnFavorite = view.findViewById(R.id.btn_favorite);
        TextView btnPolicy = view.findViewById(R.id.btn_policy);

        // Đảm bảo không nút nào bị null trước khi gán sự kiện click
        if (btnUpgrade != null) {
            btnUpgrade.setOnClickListener(v -> Toast.makeText(getContext(), "Gia hạn Premium!", Toast.LENGTH_SHORT).show());
        }
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> Toast.makeText(getContext(), "Đổi mật khẩu!", Toast.LENGTH_SHORT).show());
        }
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Mở lịch sử!", Toast.LENGTH_SHORT).show());
        }
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> Toast.makeText(getContext(), "Mở yêu thích!", Toast.LENGTH_SHORT).show());
        }
        if (btnPolicy != null) {
            btnPolicy.setOnClickListener(v -> Toast.makeText(getContext(), "Mở chính sách!", Toast.LENGTH_SHORT).show());
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> Toast.makeText(getContext(), "Đăng xuất!", Toast.LENGTH_SHORT).show());
        }

        return view;
    }
}