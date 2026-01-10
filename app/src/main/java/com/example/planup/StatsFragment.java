package com.example.planup;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadWeeklyStats();

        return view;
    }

    private void loadWeeklyStats() {

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // Last 7 days
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        Date startDate = cal.getTime();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int[] completed = new int[7];
                    int[] missed = new int[7];

                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        Date dueDate = doc.getDate("dueDate");
                        String status = doc.getString("status");

                        if (dueDate == null || status == null) continue;
                        if (dueDate.before(startDate)) continue;

                        Calendar taskCal = Calendar.getInstance();
                        taskCal.setTime(dueDate);

                        int dayIndex = 6 - (int) ((new Date().getTime() - dueDate.getTime()) / (1000 * 60 * 60 * 24));
                        if (dayIndex < 0 || dayIndex > 6) continue;

                        if ("Completed".equalsIgnoreCase(status)
                                || "Completed Late".equalsIgnoreCase(status)) {
                            completed[dayIndex]++;
                        } else if ("Missed".equalsIgnoreCase(status)) {
                            missed[dayIndex]++;
                        }
                    }

                    drawChart(completed, missed);
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
