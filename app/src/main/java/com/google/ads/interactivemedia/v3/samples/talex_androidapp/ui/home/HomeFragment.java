package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.home;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.Comic;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.Movie;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.comic.ComicAdapter;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.movies.MovieAdapter;

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
                    // Nếu đang ở ảnh cuối cùng, nhảy dứt khoát về ảnh đầu tiên (Không cuộn lùi)
                    viewPagerBanner.setCurrentItem(0, false);
                } else {
                    // Nếu chưa tới cuối, cứ cuộn mượt sang ảnh tiếp theo bên phải
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

        // 1. Điều khiển Banner (Poster lớn đề xuất)
        // 1. Điều khiển Banner (Poster lớn đề xuất) và Đồng bộ chữ theo Slide
        // 1. Điều khiển Banner (Poster lớn đề xuất) và Đồng bộ chữ theo Slide
        viewPagerBanner = view.findViewById(R.id.view_pager_banner);

        // Ánh xạ 2 TextView hiển thị chữ của Banner ở ngoài XML vào code Java
        android.widget.TextView tvBannerTitle = view.findViewById(R.id.tv_banner_title);
        android.widget.TextView tvBannerSubtitle = view.findViewById(R.id.tv_banner_subtitle);

        // ĐÃ ĐỒNG BỘ: Sắp xếp lại thứ tự tên phim chính xác
        String[] bannerTitles = {"PAYBACK: Báo Thù", "Mùa Hè Nồng Nhiệt", "Nhớ Mãi Không Quên"};
        String[] bannerSubtitles = {"Thái Lan · Cập nhật tập 3", "Trung Quốc · Tập Mới", "Hàn Quốc · Trọn bộ 24 tập"};

        // ĐÃ ĐỒNG BỘ: Sắp xếp lại thứ tự ảnh tương ứng 100% với tên phim ở trên
        List<Integer> bannerImages = new ArrayList<>();
        bannerImages.add(R.drawable.comic4); // Vị trí 0: Ứng với PAYBACK
        bannerImages.add(R.drawable.comic5); // Vị trí 1: Ứng với Mùa Hè Nồng Nhiệt
        bannerImages.add(R.drawable.comic6); // Vị trí 2: Ứng với Nhớ Mãi Không Quên

        viewPagerBanner.setAdapter(new BannerAdapter(bannerImages));

        // Bộ lắng nghe sự kiện vuốt trang để đổi nội dung chữ ngay lập tức
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
        // 2. Ánh xạ các danh sách tổng hợp (ID chuẩn theo giao diện mới)
        rvTrendingComics = view.findViewById(R.id.rv_trending_comics);
        rvRecommendedMovies = view.findViewById(R.id.rv_recommended_movies);
        rvContinueWatching = view.findViewById(R.id.rv_continue_watching);

        // Quy đổi 12dp sang Pixel để làm khoảng cách giãn đều giữa các item cuộn ngang
        int spacingInPixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

        // ĐỔI TẤT CẢ SANG ĐỊNH DẠNG CUỘN NGANG (LinearLayoutManager - HORIZONTAL) giống trang kia của bạn
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
     * Bộ trang trí phân chia khoảng cách hoàn hảo cho các item hàng ngang (Không bao giờ lo dính ảnh)
     */
    public static class HorizontalSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public HorizontalSpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position != 0) {
                outRect.left = spacing; // Tạo khoảng trống 12dp sang bên trái từ item thứ 2 trở đi
            }
        }
    }
}