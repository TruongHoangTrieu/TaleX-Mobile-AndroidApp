package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.home.HomeFragment;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.comic.ComicFragment;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.account.AccountFragment;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.movies.MoviesFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Ép giữ nguyên màu gốc của icon (như bước trước chúng ta đã sửa)
        bottomNav.setItemIconTintList(null);

        // 1. Mặc định vừa mở app lên thì hiển thị màn hình Trang Chủ đầu tiên
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // 2. Lắng nghe sự kiện click chọn qua lại giữa các tab trên Navbar
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Đối chiếu đúng cái ID bạn đã đặt trong file bottom_nav_menu.xml
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_comic) {
                selectedFragment = new ComicFragment();
            } else if (itemId == R.id.nav_movies) {
                selectedFragment = new MoviesFragment();
            } else if (itemId == R.id.nav_account) {
                selectedFragment = new AccountFragment();
            }

            // Gỡ trang cũ, đắp trang mới vào vùng trống
            return loadFragment(selectedFragment);
        });
    }

    // Hàm trung chuyển có nhiệm vụ thay thế Component (y hệt Router bên Web)
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}