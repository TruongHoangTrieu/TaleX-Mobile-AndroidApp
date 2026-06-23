package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.movies;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie.Actor;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie.Movie;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.home.HomeFragment.HorizontalSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class MovieDetailActivity extends AppCompatActivity {

    private TextView tvMovieTitle;
    private RecyclerView rvEpisodes;
    private RecyclerView rvMovieActors;
    private RecyclerView rvDetailRecommendations;

    // ĐÃ THÊM: Quản lý trình phát video Media3 trực tuyến
    private PlayerView playerView;
    private ExoPlayer player;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movie_activity_detail);

        // 1. ÁNH XẠ CÁC THÀNH PHẦN VIEW TRÊN GIAO DIỆN
        playerView = findViewById(R.id.player_view);
        tvMovieTitle = findViewById(R.id.tv_movie_title);
        rvEpisodes = findViewById(R.id.rv_episodes);
        rvMovieActors = findViewById(R.id.rv_movie_actors);
        rvDetailRecommendations = findViewById(R.id.rv_detail_recommendations);

        // 2. KHỞI TẠO EXOPLAYER CHO HOẠT ĐỘNG DEMO / QUẢNG CÁO IMA SDK
        initializePlayer();

        // 3. NHẬN DỮ LIỆU ĐỘNG TRUYỀN SANG TỪ DANH SÁCH NGOÀI
        if (getIntent() != null) {
            String title = getIntent().getStringExtra("EXTRA_MOVIE_TITLE");
            if (title != null) tvMovieTitle.setText(title);
            // Lưu ý: poster Resource tạm thời không dùng cho ImageView tĩnh nữa vì đã có PlayerView phát video
        }

        // Quy đổi 12dp sang Pixel để làm khoảng cách giãn đều hàng ngang
        int spacingInPixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

        // 4. CẤU HÌNH DANH SÁCH 24 TẬP PHIM
        List<String> episodeList = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            episodeList.add(String.valueOf(i));
        }
        rvEpisodes.setLayoutManager(new GridLayoutManager(this, 5));
        rvEpisodes.setNestedScrollingEnabled(false);

        EpisodeAdapter episodeAdapter = new EpisodeAdapter(episodeList);
        rvEpisodes.setAdapter(episodeAdapter);

        // 5. CẤU HÌNH DANH SÁCH DIỄN VIÊN
        rvMovieActors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMovieActors.addItemDecoration(new HorizontalSpacingItemDecoration(spacingInPixels));

        List<Actor> actorList = new ArrayList<>();
        actorList.add(new Actor("Yuanyina Xu", "Diễn viên chính", R.drawable.movie1));
        actorList.add(new Actor("Phó Vỹ Luân", "Diễn viên chính", R.drawable.movie2));
        actorList.add(new Actor("Lý Viễn", "Đạo diễn", R.drawable.movie3));
        actorList.add(new Actor("Bao Thượng Ân", "Biên kịch", R.drawable.movie4));

        ActorAdapter actorAdapter = new ActorAdapter(actorList);
        rvMovieActors.setAdapter(actorAdapter);

        // 6. CẤU HÌNH DANH SÁCH LƯỚI PHIM ĐỀ XUẤT (ĐÃ TÁCH FILE ITEM DECORATION)
        rvDetailRecommendations.setLayoutManager(new GridLayoutManager(this, 3));
        rvDetailRecommendations.addItemDecoration(new GridSpacingItemDecoration(3, spacingInPixels, false));

        List<Movie> recommendMovieList = new ArrayList<>();
        recommendMovieList.add(new Movie("Nhớ Mãi Không Quên", R.drawable.movie1, "Trọn bộ 24 tập"));
        recommendMovieList.add(new Movie("Người Yêu Thú Cưng", R.drawable.movie2, "Trọn bộ 24 tập"));
        recommendMovieList.add(new Movie("Mùa Hè Nồng Nhiệt", R.drawable.movie3, "Tập Mới"));
        recommendMovieList.add(new Movie("Lời Nói Dối Không Thể", R.drawable.movie4, "Trọn bộ 24 tập"));
        recommendMovieList.add(new Movie("Meeting Myself", R.drawable.movie5, "Trọn bộ 16 tập"));
        recommendMovieList.add(new Movie("Thanh Xuân Đón Gió", R.drawable.movie6, "Trọn bộ 30 tập"));

        MovieAdapter recommendAdapter = new MovieAdapter(recommendMovieList);
        rvDetailRecommendations.setAdapter(recommendAdapter);

        // 7. KHỞI TẠO LOGIC HIỆU ỨNG TABS
        LinearLayout tabEpisodes = findViewById(R.id.tab_episodes);
        LinearLayout tabRecommendations = findViewById(R.id.tab_recommendations);
        TextView tvTabEpisodes = findViewById(R.id.tv_tab_episodes);
        TextView tvTabRecommendations = findViewById(R.id.tv_tab_recommendations);
        View indicatorEpisodes = findViewById(R.id.indicator_episodes);
        View indicatorRecommendations = findViewById(R.id.indicator_recommendations);

        androidx.core.widget.NestedScrollView nestedScrollView = findViewById(R.id.nested_scroll_view);
        LinearLayout layoutSectionEpisodes = findViewById(R.id.layout_section_episodes);
        LinearLayout layoutSectionRecommendations = findViewById(R.id.layout_section_recommendations);

        tabEpisodes.setOnClickListener(v -> {
            tvTabEpisodes.setTextColor(Color.parseColor("#FFFFFF"));
            tvTabEpisodes.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            indicatorEpisodes.setBackgroundColor(Color.parseColor("#D4AF37"));

            tvTabRecommendations.setTextColor(Color.parseColor("#7C766B"));
            tvTabRecommendations.setTypeface(android.graphics.Typeface.DEFAULT);
            indicatorRecommendations.setBackgroundColor(Color.TRANSPARENT);

            int targetY = 0;
            View currentView = layoutSectionEpisodes;
            while (currentView != null && currentView != nestedScrollView) {
                targetY += currentView.getTop();
                if (currentView.getParent() instanceof View) {
                    currentView = (View) currentView.getParent();
                } else {
                    break;
                }
            }
            nestedScrollView.smoothScrollTo(0, targetY);
        });

        tabRecommendations.setOnClickListener(v -> {
            tvTabRecommendations.setTextColor(Color.parseColor("#FFFFFF"));
            tvTabRecommendations.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            indicatorRecommendations.setBackgroundColor(Color.parseColor("#D4AF37"));

            tvTabEpisodes.setTextColor(Color.parseColor("#7C766B"));
            tvTabEpisodes.setTypeface(android.graphics.Typeface.DEFAULT);
            indicatorEpisodes.setBackgroundColor(Color.TRANSPARENT);

            int targetY = 0;
            View currentView = layoutSectionRecommendations;
            while (currentView != null && currentView != nestedScrollView) {
                targetY += currentView.getTop();
                if (currentView.getParent() instanceof View) {
                    currentView = (View) currentView.getParent();
                } else {
                    break;
                }
            }
            nestedScrollView.smoothScrollTo(0, targetY);
        });
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // URL phim test định dạng MP4 trực tuyến công cộng
        String videoUrl = "https://vjs.zencdn.net/v/oceans.mp4";
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);

        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(false); // Để tạm false để vào màn hình không bị tự phát giật mình
    }

    // --- QUẢN LÝ VÒNG ĐỜI PLAYER ĐỂ TRÁNH TRÀN BỘ NHỚ ---
    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
