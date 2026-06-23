package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.home;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.movies.MovieDetailActivity;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<Integer> bannerImages;

    // Giả lập danh sách tên phim tương ứng với các vị trí ảnh banner để làm demo động
    private final String[] dummyTitles = {
            "PAYBACK: Báo Thù",
            "Mùa Hè Nồng Nhiệt",
            "Nhớ Mãi Không Quên"
    };

    public BannerAdapter(List<Integer> bannerImages) {
        this.bannerImages = bannerImages;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.imageView.setImageResource(bannerImages.get(position));

        // 🌟 XỬ LÝ SỰ KIỆN CLICK VÀO BANNER 🌟
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MovieDetailActivity.class);

            // Lấy tên phim dựa theo vị trí banner đang đứng, nếu vượt quá mảng thì lấy tên mặc định
            String movieTitle = (position < dummyTitles.length) ? dummyTitles[position] : "Phim Hot iQIYI";

            // Gửi tiêu đề phim sang màn hình chi tiết
            intent.putExtra("EXTRA_MOVIE_TITLE", movieTitle);

            // Thực hiện chuyển màn hình
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bannerImages != null ? bannerImages.size() : 0;
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_banner_item);
        }
    }
}