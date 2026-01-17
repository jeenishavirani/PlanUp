package com.example.planup;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.planup.utils.NotificationHelper;
import com.example.planup.utils.TaskCleanupHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_main);

        // 🧹 Cleanup old tasks (older than 1 week)
        TaskCleanupHelper.cleanOldTasks();

        // 🔔 Notifications
        ReminderScheduler.scheduleDailyMorningReminder(this);

        // 🔹 Show welcome notification only once when app opens
        if (savedInstanceState == null) {
            NotificationHelper.show(
                    this,
                    "PlanUp 💜",
                    "You’re doing great. One task at a time."
            );
        }

        bottomNav = findViewById(R.id.bottomNavigation);

        // Apply bottom insets to BottomNavigationView
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        // 🔥 KEYBOARD DETECTION (THIS FIXES FLOATING ISSUE)
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);

            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            // Keyboard is open if height > 15% of screen
            if (keypadHeight > screenHeight * 0.15) {
                bottomNav.setVisibility(View.GONE);
            } else {
                bottomNav.setVisibility(View.VISIBLE);
            }
        });

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
