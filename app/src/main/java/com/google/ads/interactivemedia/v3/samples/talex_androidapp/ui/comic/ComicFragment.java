package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.comic;

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
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.comic.Comic;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.home.BannerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ComicFragment extends Fragment {

    private ViewPager2 viewPagerComicBanner;
    private RecyclerView rvComicComboTab, rvNewContentTab, rvRecommendTab;

    // Quản lý tự động lướt Banner Truyện
    private Handler comicSliderHandler = new Handler();
    private Runnable comicSliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerComicBanner != null && viewPagerComicBanner.getAdapter() != null) {
                int nextItem = (viewPagerComicBanner.getCurrentItem() + 1) % viewPagerComicBanner.getAdapter().getItemCount();
                viewPagerComicBanner.setCurrentItem(nextItem, true);
                comicSliderHandler.postDelayed(this, 3000); // 3 giây trượt 1 lần
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comic, container, false);

        // 1. Kích hoạt Carousel Banner chạy tự động cho Truyện
        viewPagerComicBanner = view.findViewById(R.id.view_pager_comic_banner);
        List<Integer> comicBanners = new ArrayList<>();
        comicBanners.add(R.drawable.comic1);
        comicBanners.add(R.drawable.comic2);
        comicBanners.add(R.drawable.comic3);
        viewPagerComicBanner.setAdapter(new BannerAdapter(comicBanners));
        comicSliderHandler.postDelayed(comicSliderRunnable, 3000);

        // 2. Ánh xạ 3 RecyclerView lướt ngang
        rvComicComboTab = view.findViewById(R.id.rv_comic_combo_tab);
        rvNewContentTab = view.findViewById(R.id.rv_new_content_tab);
        rvRecommendTab = view.findViewById(R.id.rv_recommend_tab);

        // Cấu hình LayoutManager chiều ngang (Horizontal) cho cả 3 hàng
        rvComicComboTab.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvNewContentTab.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecommendTab.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 3. Đổ dữ liệu vào hàng 1: Combo Tiết Kiệm
        List<Comic> comboList = new ArrayList<>();
        comboList.add(new Comic("Combo Full Mùa 2 Ma Tôn Khốn Khổ", R.drawable.comic1, "-30%"));
        comboList.add(new Comic("Vương Gia Thất Sủng Nuôi Vợ Béo", R.drawable.comic2, "-20%"));
        comboList.add(new Comic("Độc Phi Muốn Bỏ Chồng", R.drawable.comic3, "-25%"));
        rvComicComboTab.setAdapter(new ComicAdapter(comboList));

        // 4. Đổ dữ liệu vào hàng 2: Nội Dung Mới
        List<Comic> newContentList = new ArrayList<>();
        newContentList.add(new Comic("Ta Có 5 Trực Giác Siêu Phàm", R.drawable.comic4, "NEW"));
        newContentList.add(new Comic("Xuyên Không Làm Nữ Phụ Phản Diện", R.drawable.comic5, "NEW"));
        rvNewContentTab.setAdapter(new ComicAdapter(newContentList));

        // 5. Đổ dữ liệu vào hàng 3: Đề Xuất
        List<Comic> recommendList = new ArrayList<>();
        recommendList.add(new Comic("Đại Ca Học Đường Hoàn Lương", R.drawable.comic6, "HOT"));
        recommendList.add(new Comic("Ta Là Võ Thần", R.drawable.comic1, "HOT"));
        rvRecommendTab.setAdapter(new ComicAdapter(recommendList));

        return view;
    }

    @Override
    public void onPause() { super.onPause(); comicSliderHandler.removeCallbacks(comicSliderRunnable); }
    @Override
    public void onResume() { super.onResume(); comicSliderHandler.postDelayed(comicSliderRunnable, 3000); }
}
