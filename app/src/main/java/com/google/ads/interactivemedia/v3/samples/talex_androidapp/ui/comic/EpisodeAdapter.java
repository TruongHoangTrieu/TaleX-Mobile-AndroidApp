package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.comic;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.Episode;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {

    private final List<Episode> episodeList;

    public EpisodeAdapter(List<Episode> episodeList) {
        this.episodeList = episodeList;
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout hệ thống công cộng
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.activity_list_item, parent, false);

        // Ép kích thước View con luôn luôn chiếm hết chiều ngang (Match Parent) của hàng dọc
        view.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        Episode episode = episodeList.get(position);

        String text = episode.getEpisodeName();
        if (episode.isVip()) {
            // ĐÃ THAY THẾ: Bỏ emoji vương miện, chỉ giữ lại tag [VIP] viết hoa cứng cáp
            text += "  [VIP]";
            holder.textView.setTextColor(Color.parseColor("#D4AF37")); // Màu vàng nghệ thương hiệu cho tập VIP
        } else {
            holder.textView.setTextColor(Color.parseColor("#E5E0D8")); // Màu xám trắng cho tập thường
        }

        holder.textView.setText(text);
        holder.textView.setTextSize(14); // Tăng nhẹ lên 14sp nhìn cho rõ nét
        holder.textView.setPadding(32, 16, 32, 16);

        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Đang mở: " + episode.getEpisodeName(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return episodeList != null ? episodeList.size() : 0;
    }

    public static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}