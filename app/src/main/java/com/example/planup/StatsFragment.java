package com.example.planup;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StatsFragment extends Fragment {

    private LineChart lineChart;
    private TextView tvTotalCount, tvCompletedCount, tvMissedCount, tvStreakCount;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        lineChart = view.findViewById(R.id.lineChart);
        tvTotalCount = view.findViewById(R.id.tvTotalCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvMissedCount = view.findViewById(R.id.tvMissedCount);
        tvStreakCount = view.findViewById(R.id.tvStreakCount);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadStats();

        return view;
    }

    private void loadStats() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, -6);
        Date chartStartDate = cal.getTime();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int totalTasks = querySnapshot.size();
                    int totalCompleted = 0;
                    int totalMissed = 0;

                    int[] completedByDay = new int[7];
                    int[] missedByDay = new int[7];
                    
                    Set<String> completedDates = new HashSet<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Date dueDate = doc.getDate("dueDate");
                        String status = doc.getString("status");

                        if (status == null) continue;

                        boolean isDone = "Completed".equalsIgnoreCase(status) || "Completed Late".equalsIgnoreCase(status);
                        
                        if (isDone) {
                            totalCompleted++;
                            if (dueDate != null) {
                                completedDates.add(sdf.format(dueDate));
                            }
                        } else if ("Missed".equalsIgnoreCase(status)) {
                            totalMissed++;
                        }

                        if (dueDate == null) continue;

                        // Chart logic (last 7 days)
                        if (!dueDate.before(chartStartDate) && !dueDate.after(new Date())) {
                            long diff = dueDate.getTime() - chartStartDate.getTime();
                            int dayIndex = (int) (diff / (1000 * 60 * 60 * 24));
                            
                            if (dayIndex >= 0 && dayIndex < 7) {
                                if (isDone) {
                                    completedByDay[dayIndex]++;
                                } else if ("Missed".equalsIgnoreCase(status)) {
                                    missedByDay[dayIndex]++;
                                }
                            }
                        }
                    }

                    // Calculate Streak
                    int streak = calculateStreak(completedDates);

                    // Update UI
                    tvTotalCount.setText(String.valueOf(totalTasks));
                    tvCompletedCount.setText(String.valueOf(totalCompleted));
                    tvMissedCount.setText(String.valueOf(totalMissed));
                    tvStreakCount.setText(String.valueOf(streak));

                    drawChart(completedByDay, missedByDay);
                });
    }

    private int calculateStreak(Set<String> completedDates) {
        int streak = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        
        // Start checking from today
        String todayStr = sdf.format(cal.getTime());
        
        // If nothing today, check if streak continued from yesterday
        if (!completedDates.contains(todayStr)) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String yesterdayStr = sdf.format(cal.getTime());
            if (!completedDates.contains(yesterdayStr)) {
                return 0; // Streak broken
            }
        } else {
            // Today has completions, count it and then check previous days
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Count backwards
        while (completedDates.contains(sdf.format(cal.getTime()))) {
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    private void drawChart(int[] completed, int[] missed) {
        List<Entry> completedEntries = new ArrayList<>();
        List<Entry> missedEntries = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            completedEntries.add(new Entry(i, completed[i]));
            missedEntries.add(new Entry(i, missed[i]));
        }

        LineDataSet completedSet = new LineDataSet(completedEntries, "Completed");
        completedSet.setColor(Color.parseColor("#4CAF50"));
        completedSet.setCircleColor(Color.parseColor("#4CAF50"));
        completedSet.setLineWidth(2f);
        completedSet.setDrawFilled(true);
        completedSet.setFillAlpha(60);

        LineDataSet missedSet = new LineDataSet(missedEntries, "Missed");
        missedSet.setColor(Color.parseColor("#F44336"));
        missedSet.setCircleColor(Color.parseColor("#F44336"));
        missedSet.setLineWidth(2f);
        missedSet.setDrawFilled(true);
        missedSet.setFillAlpha(60);

        LineData data = new LineData(completedSet, missedSet);
        lineChart.setData(data);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(800);
        lineChart.invalidate();
    }
}
