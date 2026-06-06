package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.home;

import android.os.Bundle;
import android.os.Handler;
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
    private RecyclerView rvComicCombo, rvHomeMoviesHot, rvHomeMoviesNew;

    private Handler sliderHandler = new Handler();
    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerBanner != null && viewPagerBanner.getAdapter() != null) {
                int nextItem = (viewPagerBanner.getCurrentItem() + 1) % viewPagerBanner.getAdapter().getItemCount();
                viewPagerBanner.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(this, 3000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Điều khiển Banner
        viewPagerBanner = view.findViewById(R.id.view_pager_banner);
        List<Integer> bannerImages = new ArrayList<>();
        bannerImages.add(R.drawable.comic4);
        bannerImages.add(R.drawable.comic5);
        bannerImages.add(R.drawable.comic6);
        viewPagerBanner.setAdapter(new BannerAdapter(bannerImages));
        sliderHandler.postDelayed(sliderRunnable, 3000);

        // 2. Ánh xạ các danh sách tổng hợp
        rvComicCombo = view.findViewById(R.id.rv_comic_combo);
        rvHomeMoviesHot = view.findViewById(R.id.rv_home_movies_hot);
        rvHomeMoviesNew = view.findViewById(R.id.rv_home_movies_new);

        // ĐỊNH DẠNG QUẢN LÝ LAYOUT THEO CHIỀU NGANG CHO TẤT CẢ
        rvComicCombo.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvHomeMoviesHot.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvHomeMoviesNew.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 3. Đổ dữ liệu TRUYỆN TRANH
        List<Comic> comboList = new ArrayList<>();
        comboList.add(new Comic("Combo Full Mùa 2 Ma Tôn Khốn Khổ", R.drawable.comic1, "-30%"));
        comboList.add(new Comic("Vương Gia Thất Sủng Nuôi Vợ Béo", R.drawable.comic2, "-20%"));
        comboList.add(new Comic("Độc Phi Muốn Bỏ Chồng", R.drawable.comic3, "-25%"));
        rvComicCombo.setAdapter(new ComicAdapter(comboList));

        // 4. Đổ dữ liệu PHIM HOT & PHIM MỚI
        List<Movie> hotMovieList = new ArrayList<>();
        hotMovieList.add(new Movie("Ma Tôn Bản Truyền Hình", R.drawable.comic4, "FHD | Vietsub"));
        hotMovieList.add(new Movie("Võ Thần Chí Tôn (Movie)", R.drawable.comic5, "HD | Thuyết Minh"));
        rvHomeMoviesHot.setAdapter(new MovieAdapter(hotMovieList));

        List<Movie> newMovieList = new ArrayList<>();
        newMovieList.add(new Movie("Nữ Phụ Trở Lại Hoạt Hình", R.drawable.comic6, "Tập 12/12"));
        newMovieList.add(new Movie("Tiểu Thư Ác Độc Đại Chiến", R.drawable.comic1, "FHD"));
        rvHomeMoviesNew.setAdapter(new MovieAdapter(newMovieList));

        return view;
    }

    @Override
    public void onPause() { super.onPause(); sliderHandler.removeCallbacks(sliderRunnable); }
    @Override
    public void onResume() { super.onResume(); sliderHandler.postDelayed(sliderRunnable, 3000); }
}