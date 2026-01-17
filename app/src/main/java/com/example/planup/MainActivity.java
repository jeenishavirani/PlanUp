package com.example.planup;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // 🔔 Notifications (unchanged)
        ReminderScheduler.scheduleDailyMorningReminder(this);

        bottomNav = findViewById(R.id.bottomNavigation);

        // 🔥 KEYBOARD DETECTION (CORRECT APPROACH)
//        View rootView = findViewById(android.R.id.content);
//        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
//
//            Rect r = new Rect();
//            rootView.getWindowVisibleDisplayFrame(r);
//
//            int screenHeight = rootView.getRootView().getHeight();
//            int keypadHeight = screenHeight - r.bottom;
//
//            // Keyboard open → hide bottom nav
//            if (keypadHeight > screenHeight * 0.15) {
//                bottomNav.setVisibility(View.GONE);
//            } else {
//                bottomNav.setVisibility(View.VISIBLE);
//            }
//        });

        // 🔹 Default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        bottomNav.setOnItemSelectedListener(item -> {

            Fragment fragment = null;

            if (item.getItemId() == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_tasks) {
                fragment = new TasksFragment();
            } else if (item.getItemId() == R.id.nav_stats) {
                fragment = new StatsFragment();
            } else if (item.getItemId() == R.id.nav_ai) {
                fragment = new AIAssistantFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
