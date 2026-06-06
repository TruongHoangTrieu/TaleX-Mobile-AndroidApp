package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.movies;

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
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.Movie;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.home.BannerAdapter;
import java.util.ArrayList;
import java.util.List;

public class MoviesFragment extends Fragment {

    private ViewPager2 viewPagerMovieBanner;
    private RecyclerView rvMoviesHot, rvMoviesNew, rvMoviesTrending;

    // Xử lý tự động chạy slide cho Banner Phim
    private Handler movieSliderHandler = new Handler();
    private Runnable movieSliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerMovieBanner != null && viewPagerMovieBanner.getAdapter() != null) {
                int currentItem = viewPagerMovieBanner.getCurrentItem();
                int totalItems = viewPagerMovieBanner.getAdapter().getItemCount();
                int nextItem = (currentItem + 1) % totalItems;
                viewPagerMovieBanner.setCurrentItem(nextItem, true);
                movieSliderHandler.postDelayed(this, 3000); // 3 giây chuyển hình 1 lần
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movies, container, false);

        // 1. Ánh xạ Banner Phim và nạp 3 ảnh slide (Dùng tạm ảnh comic để test)
        viewPagerMovieBanner = view.findViewById(R.id.view_pager_movie_banner);
        List<Integer> movieBanners = new ArrayList<>();
        movieBanners.add(R.drawable.movie6);
        movieBanners.add(R.drawable.movie2);
        movieBanners.add(R.drawable.movie3);

        BannerAdapter bannerAdapter = new BannerAdapter(movieBanners);
        viewPagerMovieBanner.setAdapter(bannerAdapter);
        movieSliderHandler.postDelayed(movieSliderRunnable, 3000); // Kích hoạt auto-slide

        // 2. Ánh xạ các RecyclerView danh sách phim
        rvMoviesHot = view.findViewById(R.id.rv_movies_hot);
        rvMoviesNew = view.findViewById(R.id.rv_movies_new);
        rvMoviesTrending= view.findViewById(R.id.rv_movies_trending);


        // 3. Chuẩn bị dữ liệu cho Phim Hot
        List<Movie> hotMovieList = new ArrayList<>();
        hotMovieList.add(new Movie("Ma Tôn Bản Truyền Hình", R.drawable.comic1, "FHD | Vietsub"));
        hotMovieList.add(new Movie("Võ Thần Chí Tôn (Movie)", R.drawable.comic2, "HD | Thuyết Minh"));
        hotMovieList.add(new Movie("Học Đường Nổi Loạn Mùa 1", R.drawable.comic3, "1080p"));

        // 4. Chuẩn bị dữ liệu cho Phim Mới
        List<Movie> newMovieList = new ArrayList<>();
        newMovieList.add(new Movie("Nữ Phụ Trở Lại Hoạt Hình", R.drawable.comic4, "Tập 12/12"));
        newMovieList.add(new Movie("Tiểu Thư Ác Độc Đại Chiến", R.drawable.comic5, "FHD"));
        newMovieList.add(new Movie("Hoàng Hậu Kiêu Kỳ", R.drawable.comic6, "Trailer"));

        //4.1.Chuẩn bị dữ liệu cho Phim Trending
        List<Movie> movieTrending = new ArrayList<>();

        movieTrending.add(new Movie(
                "Ma Tôn Bản Truyền Hình",
                R.drawable.movie1,
                "FHD | Vietsub"));

        movieTrending.add(new Movie(
                "Võ Thần Chí Tôn (Movie)",
                R.drawable.movie2,
                "HD | Thuyết Minh"));

        movieTrending.add(new Movie(
                "Học Đường Nổi Loạn Mùa 1",
                R.drawable.movie3,
                "1080p"));

        movieTrending.add(new Movie(
                "Bí Mật Dưới Ánh Trăng",
                R.drawable.movie4,
                "FHD | Vietsub"));

        movieTrending.add(new Movie(
                "Hành Trình Xuyên Không",
                R.drawable.movie5,
                "HD | Vietsub"));

        movieTrending.add(new Movie(
                "Đặc Vụ Số 07",
                R.drawable.movie6,
                "FHD | Thuyết Minh"));


        // 5. Khởi tạo và gắn Adapter theo CHIỀU NGANG (HORIZONTAL) cho cả 2 mục
        MovieAdapter hotMovieAdapter = new MovieAdapter(hotMovieList);
        MovieAdapter newMovieAdapter = new MovieAdapter(newMovieList);
        MovieAdapter trendingMovieAdapter = new MovieAdapter(movieTrending);


        rvMoviesHot.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMoviesNew.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMoviesTrending.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        rvMoviesHot.setAdapter(hotMovieAdapter);
        rvMoviesNew.setAdapter(newMovieAdapter);
        rvMoviesTrending.setAdapter(trendingMovieAdapter);
        return view;
    }

    // Quản lý vòng đời luồng chạy để tiết kiệm pin
    @Override
    public void onPause() {
        super.onPause();
        movieSliderHandler.removeCallbacks(movieSliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        movieSliderHandler.postDelayed(movieSliderRunnable, 3000);
    }
}