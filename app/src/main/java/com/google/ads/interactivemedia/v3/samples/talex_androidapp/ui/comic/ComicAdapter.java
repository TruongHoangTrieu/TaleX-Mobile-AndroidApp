package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.comic;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.comic.Comic;
import java.util.List;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

    private List<Comic> comicList;

    // Hàm khởi tạo nhận vào danh sách truyện tranh
    public ComicAdapter(List<Comic> comicList) {
        this.comicList = comicList;
    }

    @NonNull
    @Override
    public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp cái giao diện ô truyện thô xml vào code Java
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comic_card, parent, false);
        return new ComicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
        // Lấy dữ liệu truyện ở vị trí hiện tại
        Comic comic = comicList.get(position);

        // Đổ dữ liệu vào các thẻ giao diện tương ứng
        holder.tvTitle.setText(comic.getTitle());
        holder.tvSale.setText(comic.getSalePercent());
        holder.imgCover.setImageResource(comic.getImageResource()); // Đổ ảnh bìa

        // 🌟 BỔ SUNG: Xử lý sự kiện click vào ô truyện tranh để sang màn hình chi tiết
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ComicDetailActivity.class);

            // Gửi tên truyện động sang màn hình chi tiết truyện
            intent.putExtra("EXTRA_COMIC_TITLE", comic.getTitle());
            intent.putExtra("EXTRA_COMIC_IMAGE", comic.getImageResource());
            // Thực hiện chuyển màn hình
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return comicList != null ? comicList.size() : 0;
    }

    // Lớp giữ các thành phần giao diện của 1 ô truyện để tái sử dụng (giúp lướt mượt, không giật lag)
    public static class ComicViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvSale;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_comic_cover);
            tvTitle = itemView.findViewById(R.id.tv_comic_title);
            tvSale = itemView.findViewById(R.id.tv_sale_badge);
        }
    }
}
