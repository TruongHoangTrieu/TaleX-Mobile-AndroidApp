package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.comic;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.comic.Chapter;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie.Episode;
import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    private final List<Chapter> chapterList;

    public ChapterAdapter(List<Chapter> chapterList) {
        this.chapterList = chapterList;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter_group, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);

        // 1. Gán text tên Chương sạch emoji
        holder.tvChapterTitle.setText(chapter.getChapterName());

        // 2. Cài đặt danh sách Tập con xếp hàng dọc
        holder.rvEpisodes.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));

        // --- 🌟 XỬ LÝ TEXT TẬP TRONG ADAPTER CON ---
        // Sửa lại cách hiển thị Tập để bỏ emoji vương miện
        EpisodeAdapter episodeAdapter = new EpisodeAdapter(chapter.getEpisodeList());
        episodeAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });

        holder.rvEpisodes.setAdapter(episodeAdapter);


        holder.rvEpisodes.setVisibility(View.VISIBLE);
        holder.imgArrow.setRotation(0);
        holder.imgArrow.setColorFilter(Color.parseColor("#D4AF37"), PorterDuff.Mode.SRC_IN);

        // 3. Logic click Đóng/Mở mượt mà và nhuộm màu icon động
        holder.itemView.setOnClickListener(v -> {
            if (holder.rvEpisodes.getVisibility() == View.VISIBLE) {
                holder.rvEpisodes.setVisibility(View.GONE);
                // Đóng: Xoay ngang, Nhuộm màu xám
                holder.imgArrow.setRotation(270);
                holder.imgArrow.setColorFilter(Color.parseColor("#7C766B"), PorterDuff.Mode.SRC_IN);
            } else {
                holder.rvEpisodes.setVisibility(View.VISIBLE);

                holder.imgArrow.setRotation(0);
                holder.imgArrow.setColorFilter(Color.parseColor("#D4AF37"), PorterDuff.Mode.SRC_IN);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapterList != null ? chapterList.size() : 0;
    }

    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterTitle; // 🌟 Tên biến ở đây đang để là Title
        RecyclerView rvEpisodes;
        ImageView imgArrow;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            // SỬA TẠI ĐÂY: Đổi tvChapterName thành tvChapterTitle cho khớp với khai báo ở trên
            tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
            rvEpisodes = itemView.findViewById(R.id.rv_child_episodes);
            imgArrow = itemView.findViewById(R.id.img_arrow);
        }
    }
}
