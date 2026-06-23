package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.home;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.comic.Comic;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie.Movie;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.comic.ComicAdapter;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.movies.MovieAdapter;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.dailyreward.MissionCenterActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ViewPager2 viewPagerBanner;
    private RecyclerView rvTrendingComics, rvRecommendedMovies, rvContinueWatching;

    private Handler sliderHandler = new Handler();
    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerBanner != null && viewPagerBanner.getAdapter() != null) {
                int currentItem = viewPagerBanner.getCurrentItem();
                int totalItems = viewPagerBanner.getAdapter().getItemCount();

                if (currentItem == totalItems - 1) {
                    viewPagerBanner.setCurrentItem(0, false);
                } else {
                    viewPagerBanner.setCurrentItem(currentItem + 1, true);
                }
                sliderHandler.postDelayed(this, 3000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Điều khiển Banner (Poster lớn đề xuất) và Đồng bộ chữ theo Slide
        viewPagerBanner = view.findViewById(R.id.view_pager_banner);
        TextView tvBannerTitle = view.findViewById(R.id.tv_banner_title);
        TextView tvBannerSubtitle = view.findViewById(R.id.tv_banner_subtitle);

        String[] bannerTitles = {"PAYBACK: Báo Thù", "Mùa Hè Nồng Nhiệt", "Nhớ Mãi Không Quên"};
        String[] bannerSubtitles = {"Thái Lan · Cập nhật tập 3", "Trung Quốc · Tập Mới", "Hàn Quốc · Trọn bộ 24 tập"};

        List<Integer> bannerImages = new ArrayList<>();
        bannerImages.add(R.drawable.comic4);
        bannerImages.add(R.drawable.comic5);
        bannerImages.add(R.drawable.comic6);

        // Đã sửa lỗi: Nạp Adapter được định nghĩa ở cuối file
        viewPagerBanner.setAdapter(new BannerAdapter(bannerImages));

        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position < bannerTitles.length) {
                    tvBannerTitle.setText(bannerTitles[position]);
                    tvBannerSubtitle.setText(bannerSubtitles[position]);
                }
            }
        });

        sliderHandler.postDelayed(sliderRunnable, 3000);

        // 2. Ánh xạ các danh sách tổng hợp
        rvTrendingComics = view.findViewById(R.id.rv_trending_comics);
        rvRecommendedMovies = view.findViewById(R.id.rv_recommended_movies);
        rvContinueWatching = view.findViewById(R.id.rv_continue_watching);

        int spacingInPixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

        rvTrendingComics.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTrendingComics.addItemDecoration(new HorizontalSpacingItemDecoration(spacingInPixels));

        rvRecommendedMovies.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecommendedMovies.addItemDecoration(new HorizontalSpacingItemDecoration(spacingInPixels));

        rvContinueWatching.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvContinueWatching.addItemDecoration(new HorizontalSpacingItemDecoration(spacingInPixels));

        // 3. Đổ dữ liệu TRUYỆN TRANH XU HƯỚNG
        List<Comic> comboList = new ArrayList<>();
        comboList.add(new Comic("Combo Full Mùa 2 Ma Tôn Khốn Khổ", R.drawable.comic1, "-30%"));
        comboList.add(new Comic("Vương Gia Thất Sủng Nuôi Vợ Béo", R.drawable.comic2, "-20%"));
        comboList.add(new Comic("Độc Phi Muốn Bỏ Chồng", R.drawable.comic3, "-25%"));
        rvTrendingComics.setAdapter(new ComicAdapter(comboList));

        // 4. Đổ dữ liệu PHIM BỘ CHỌN LỌC
        List<Movie> hotMovieList = new ArrayList<>();
        hotMovieList.add(new Movie("Ma Tôn Bản Truyền Hình", R.drawable.comic4, "FHD | Vietsub"));
        hotMovieList.add(new Movie("Võ Thần Chí Tôn (Movie)", R.drawable.comic5, "HD | Thuyết Minh"));
        rvRecommendedMovies.setAdapter(new MovieAdapter(hotMovieList));

        // 5. Đổ dữ liệu MỤC XEM TIẾP
        List<Movie> newMovieList = new ArrayList<>();
        newMovieList.add(new Movie("Nữ Phụ Trở Lại Hoạt Hình", R.drawable.comic6, "Tập 12/12"));
        newMovieList.add(new Movie("Tiểu Thư Ác Độc Đại Chiến", R.drawable.comic1, "FHD"));
        rvContinueWatching.setAdapter(new MovieAdapter(newMovieList));

        // 6. XỬ LÝ CLICK NÚT VÍ XU SANG TRUNG TÂM NHIỆM VỤ
        LinearLayout btnCoinWallet = view.findViewById(R.id.btn_coin_wallet);
        if (btnCoinWallet != null) {
            btnCoinWallet.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), MissionCenterActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    /**
     * LỚP INNER CLASS BANNER ADAPTER ĐỂ FIX LỖI BIÊN DỊCH CHƯA KHAI BÁO
     */
    private static class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
        private final List<Integer> imageList;

        public BannerAdapter(List<Integer> imageList) {
            this.imageList = imageList;
        }

        @NonNull
        @Override
        public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new BannerViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
            holder.imageView.setImageResource(imageList.get(position));
        }

        @Override
        public int getItemCount() {
            return imageList.size();
        }

        static class BannerViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            public BannerViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }
        }
    }

    public static class HorizontalSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;
        public HorizontalSpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position != 0) {
                outRect.left = spacing;
            }
        }
    }
}