package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.movies;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {

    private final List<String> list;
    private int selectedPosition = 0; // Mặc định tập 1 được chọn

    public EpisodeAdapter(List<String> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        holder.tvEpisodeNumber.setText(list.get(position));

        if (selectedPosition == position) {
            holder.tvEpisodeNumber.setTextColor(Color.parseColor("#D4AF37"));
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.tvEpisodeNumber.setTextColor(Color.parseColor("#E5E0D8"));
            holder.itemView.setAlpha(0.6f);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            Toast.makeText(v.getContext(), "Đang chuẩn bị phát Tập " + list.get(selectedPosition), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        TextView tvEpisodeNumber;

        public EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEpisodeNumber = itemView.findViewById(R.id.tv_episode_number);
        }
    }
}