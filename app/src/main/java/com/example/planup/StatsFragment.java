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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StatsFragment extends Fragment {

    private LineChart lineChart;
    private TextView tvTotalTasks, tvCompleted, tvMissed, tvStreak;

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
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks);
        tvCompleted = view.findViewById(R.id.tvCompleted);
        tvMissed = view.findViewById(R.id.tvMissed);
        tvStreak = view.findViewById(R.id.tvStreak);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadStats();

        return view;
    }

    private void loadStats() {

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        Date startDate = cal.getTime();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .get()
                .addOnSuccessListener(snapshot -> {

                    int total = 0;
                    int completedCount = 0;
                    int missedCount = 0;
                    int streak = 0;

                    int[] weeklyCompleted = new int[7];
                    int[] weeklyMissed = new int[7];

                    List<com.google.firebase.firestore.DocumentSnapshot> docs = snapshot.getDocuments();



                    // 🔥 SORT BY COMPLETION TIME (latest first)
                    docs.sort((a, b) -> {
                        Date da = a.getDate("dueDate");
                        Date db = b.getDate("dueDate");
                        if (da == null || db == null) return 0;
                        return db.compareTo(da);
                    });

                    boolean streakBroken = false;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {


                        total++;

                        String status = doc.getString("status");
                        Date dueDate = doc.getDate("dueDate");

                        // ----- STREAK LOGIC -----
                        if (!streakBroken) {
                            if ("Completed".equalsIgnoreCase(status)
                                    || "Completed Late".equalsIgnoreCase(status)) {
                                streak++;
                            } else if ("Missed".equalsIgnoreCase(status)) {
                                streakBroken = true;
                            }
                        }

                        // ----- COUNTS -----
                        if ("Completed".equalsIgnoreCase(status)
                                || "Completed Late".equalsIgnoreCase(status)) {
                            completedCount++;
                        } else if ("Missed".equalsIgnoreCase(status)) {
                            missedCount++;
                        }

                        // ----- WEEKLY CHART -----
                        if (dueDate == null || dueDate.before(startDate)) continue;

                        int dayIndex = 6 - (int) ((new Date().getTime() - dueDate.getTime())
                                / (1000 * 60 * 60 * 24));

                        if (dayIndex < 0 || dayIndex > 6) continue;

                        if ("Completed".equalsIgnoreCase(status)
                                || "Completed Late".equalsIgnoreCase(status)) {
                            weeklyCompleted[dayIndex]++;
                        } else if ("Missed".equalsIgnoreCase(status)) {
                            weeklyMissed[dayIndex]++;
                        }
                    }

                    // 🔹 UPDATE UI
                    tvTotalTasks.setText("Total Tasks\n" + total);
                    tvCompleted.setText("Completed\n" + completedCount);
                    tvMissed.setText("Missed\n" + missedCount);
                    tvStreak.setText("Streak 🔥\n" + streak + " tasks");

                    drawChart(weeklyCompleted, weeklyMissed);
                });
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
