package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.comic;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.movie.Episode;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.comic.Chapter;
import java.util.ArrayList;
import java.util.List;

public class ComicDetailActivity extends AppCompatActivity {

    // 1. KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN (VIEW)
    private TextView tvComicTitle;
    private RecyclerView rvChapters;
    private ImageButton btnBack;
    private Button btnStartReading;
    private MaterialButton btnFollow;

    // Khai báo thêm các biến phục vụ cho tính năng phân cấp Mùa
    private TextView tabSeason1, tabSeason2, tabSeason3;
    private TextView tvTotalChapters;
    private ImageView imgComicCover;

    // 2. KHAI BÁO BIẾN DỮ LIỆU & ADAPTER
    private boolean isFollowed = false; // Biến trạng thái theo dõi truyện
    private final List<Chapter> allChaptersList = new ArrayList<>();
    private final List<Chapter> filteredList = new ArrayList<>();
    private ChapterAdapter chapterAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comic_activity_detail);

        // 3. ÁNH XẠ CÁC VIEW TỪ FILE XML SANG JAVA
        tvComicTitle = findViewById(R.id.tv_comic_detail_title);
        rvChapters = findViewById(R.id.rv_comic_chapters);
        btnBack = findViewById(R.id.btn_back);
        btnStartReading = findViewById(R.id.btn_start_reading);
        btnFollow = findViewById(R.id.btn_follow);

        // Ánh xạ các nút Tab Mùa và text thống kê mới
        tabSeason1 = findViewById(R.id.tab_season_1);
        tabSeason2 = findViewById(R.id.tab_season_2);
        tabSeason3 = findViewById(R.id.tab_season_3);
        tvTotalChapters = findViewById(R.id.tv_total_chapters);
        imgComicCover = findViewById(R.id.img_comic_detail_cover);

        // 4. XỬ LÝ NÚT QUAY LẠI (BACK)
        btnBack.setOnClickListener(v -> finish());

        // 5. NHẬN DỮ LIỆU ĐỘNG TỪ TRANG HOME SANG
        if (getIntent() != null) {
            // Nhận tên truyện
            String title = getIntent().getStringExtra("EXTRA_COMIC_TITLE");
            if (title != null) tvComicTitle.setText(title);

            // Nhận ID ảnh và ép ImageView nạp ảnh động tương ứng
            int imageRes = getIntent().getIntExtra("EXTRA_COMIC_IMAGE", R.drawable.comic1);
            imgComicCover.setImageResource(imageRes);
        }

        // 6. XỬ LÝ SỰ KIỆN CLICK NÚT THEO DÕI ĐỘNG
        btnFollow.setOnClickListener(v -> {
            isFollowed = !isFollowed;
            if (isFollowed) {
                btnFollow.setText("Đang theo dõi");
                btnFollow.setIconResource(android.R.drawable.btn_star_big_off);
                btnFollow.setBackgroundColor(Color.parseColor("#454B54")); // Màu xám tối khi đã theo dõi
                Toast.makeText(this, "Đã thêm vào tủ truyện yêu thích!", Toast.LENGTH_SHORT).show();
            } else {
                btnFollow.setText("Theo dõi");
                btnFollow.setIconResource(android.R.drawable.btn_star_big_on);
                btnFollow.setBackgroundColor(Color.parseColor("#252830")); // Trở về màu nền gốc
            }
        });

        // 7. KHỞI TẠO MOCK DATA PHÂN CẤP SÂU: MÙA -> CHƯƠNG -> CÁC TẬP CON
        allChaptersList.clear();

        // --- DỮ LIỆU CHO MÙA 1 ---
        List<Episode> eListMùa1Chương1 = new ArrayList<>();
        eListMùa1Chương1.add(new Episode("Tập 01: Thức tỉnh", "2026-06-15", false));
        eListMùa1Chương1.add(new Episode("Tập 02: Sức mạnh mới", "2026-06-16", false));
        allChaptersList.add(new Chapter(1, "Chương 01: Khởi đầu sóng gió", eListMùa1Chương1));

        List<Episode> eListMùa1Chương2 = new ArrayList<>();
        eListMùa1Chương2.add(new Episode("Tập 03: Đồng minh xuất hiện", "2026-06-17", false));
        eListMùa1Chương2.add(new Episode("Tập 04: Cạm bẫy", "2026-06-18", true));
        allChaptersList.add(new Chapter(1, "Chương 02: Thập diện mai phục", eListMùa1Chương2));

        // --- DỮ LIỆU CHO MÙA 2 ---
        List<Episode> eListMùa2Chương1 = new ArrayList<>();
        eListMùa2Chương1.add(new Episode("Tập 05: Chuyển giới", "2026-06-19", false));
        eListMùa2Chương1.add(new Episode("Tập 06: Trả thù ", "2026-06-20", true));
        allChaptersList.add(new Chapter(2, "Chương 01: Trỗi dậy từ tro tàn", eListMùa2Chương1));

        // 8. CẤU HÌNH DANH SÁCH RECYCLERVIEW
        rvChapters.setLayoutManager(new LinearLayoutManager(this));
        rvChapters.setNestedScrollingEnabled(false);

        // Gắn danh sách đã lọc (filteredList) vào adapter để quản lý hiển thị
        chapterAdapter = new ChapterAdapter(filteredList);
        rvChapters.setAdapter(chapterAdapter);

        // 9. CÀI ĐẶT CÁC TAB MÙA LÁNG MỊN VÀ CĂN CHỈNH BẰNG NHAU
        // Mặc định khi vừa mở trang sẽ lọc và hiển thị ngay các tập của Mùa 1
        filterChaptersBySeason(1);

        // Bắt sự kiện click mượt mà cho 3 tab
        tabSeason1.setOnClickListener(v -> filterChaptersBySeason(1));
        tabSeason2.setOnClickListener(v -> filterChaptersBySeason(2));
        tabSeason3.setOnClickListener(v -> filterChaptersBySeason(3));

        // Ép 3 tab có kích thước và chữ giống nhau bằng code Java
        setupSeasonTabsAlignment();

        // Xử lý nút Đọc ngay dưới chân trang
        btnStartReading.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng đọc truyện đang được phát triển!", Toast.LENGTH_SHORT).show();
        });
    } // 🌟 ĐÃ ĐÓNG NGOẶC HÀM onCreate CHUẨN XÁC TẠI ĐÂY

    /**
     * 🌟 ĐÃ ĐƯA RA NGOÀI ĐỘC LẬP: Hàm hỗ trợ căn chỉnh các TAB MÙA đều nhau tăm tắp
     */
    private void setupSeasonTabsAlignment() {
        TextView[] tabs = {tabSeason1, tabSeason2, tabSeason3};

        for (TextView tab : tabs) {
            tab.setTextSize(14);
            tab.setTypeface(null, android.graphics.Typeface.BOLD);
            tab.setTextColor(Color.parseColor("#E5E0D8"));

            // Ép padding ngang rộng ra để các tab vuông vức và bằng nhau
            tab.setPadding(30, 16, 30, 16);

            // Cập nhật thông số layout ổn định trong ViewGroup cha
            android.view.ViewGroup.LayoutParams lp = tab.getLayoutParams();
            if (lp != null) {
                lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                tab.setLayoutParams(lp);
            }
        }
    }

    /**
     * Hàm hỗ trợ lọc dữ liệu chương truyện động và highlight màu sắc Tab theo Mùa được chọn
     * @param season Số thứ tự mùa cần lọc (1, 2, hoặc 3)
     */
    private void filterChaptersBySeason(int season) {
        filteredList.clear();
        for (Chapter c : allChaptersList) {
            if (c.getSeasonNumber() == season) {
                filteredList.add(c);
            }
        }
        chapterAdapter.notifyDataSetChanged();

        // Cập nhật text thống kê dưới thanh tab
        tvTotalChapters.setText("Mùa " + season + " hiện có " + filteredList.size() + " tập nội dung");

        // TAB MÙA 1
        tabSeason1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(season == 1 ? "#D4AF37" : "#252830")
        ));
        tabSeason1.setTextColor(Color.parseColor(season == 1 ? "#141210" : "#CCCCCC"));

        // TAB MÙA 2
        tabSeason2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(season == 2 ? "#D4AF37" : "#252830")
        ));
        tabSeason2.setTextColor(Color.parseColor(season == 2 ? "#141210" : "#CCCCCC"));

        // TAB MÙA 3
        tabSeason3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(season == 3 ? "#D4AF37" : "#252830")
        ));
        tabSeason3.setTextColor(Color.parseColor(season == 3 ? "#141210" : "#CCCCCC"));
    }
}
