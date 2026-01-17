package com.example.planup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.planup.model.TaskModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";

    private PieChart pieChart;
    private BarChart barChart, priorityBarChart;
    private MaterialButtonToggleGroup toggleChartType;
    
    private TextView tvTotalCount, tvCompletedCount, tvMissedCount, tvStreakCount, tvNoData;
    private TextView tvAnalysisTitle, tvAnalysisText;
    private View cardTotal, cardCompleted, cardMissed;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration statsListener;

    private int lastTotal, lastCompleted, lastMissed;
    private int[] weeklyCompleted = new int[7];
    private Map<String, Integer> priorityCompleted = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        priorityBarChart = view.findViewById(R.id.priorityBarChart);
        
        toggleChartType = view.findViewById(R.id.toggleChartType);

        tvTotalCount = view.findViewById(R.id.tvTotalCount);
        tvCompletedCount = view.findViewById(R.id.tvCompletedCount);
        tvMissedCount = view.findViewById(R.id.tvMissedCount);
        tvStreakCount = view.findViewById(R.id.tvStreakCount);
        tvNoData = view.findViewById(R.id.tvNoData);
        
        tvAnalysisTitle = view.findViewById(R.id.tvAnalysisTitle);
        tvAnalysisText = view.findViewById(R.id.tvAnalysisText);
        
        cardTotal = view.findViewById(R.id.cardTotal);
        cardCompleted = view.findViewById(R.id.cardCompleted);
        cardMissed = view.findViewById(R.id.cardMissed);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupClickListeners();
        return view;
    }

    private void setupClickListeners() {
        cardTotal.setOnClickListener(v -> openTaskList("all"));
        cardCompleted.setOnClickListener(v -> openTaskList("completed"));
        cardMissed.setOnClickListener(v -> openTaskList("missed"));

        toggleChartType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                showChart(checkedId);
            }
        });
    }

    private void showChart(int checkedId) {
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);
        priorityBarChart.setVisibility(View.GONE);

        if (checkedId == R.id.btnPie) {
            pieChart.setVisibility(View.VISIBLE);
            drawPieChart(lastTotal, lastCompleted, lastMissed);
            updateAnalysis("Overall Summary", "This circle shows how you are doing. Green is what you finished, Red is what you missed, and Purple is what's left.");
        } else if (checkedId == R.id.btnBar) {
            barChart.setVisibility(View.VISIBLE);
            drawWeeklyChart();
            updateAnalysis("Weekly Progress", "This shows how many tasks you finished each day for the last week. Taller bars mean you were very busy!");
        } else if (checkedId == R.id.btnRadar) {
            priorityBarChart.setVisibility(View.VISIBLE);
            drawPriorityChart();
            updateAnalysis("Important Tasks", "This shows which types of tasks you finish most. It helps you see if you are focusing on High, Medium, or Low priority work.");
        }
    }

    private void updateAnalysis(String title, String analysis) {
        if (tvAnalysisTitle != null) tvAnalysisTitle.setText(title);
        if (tvAnalysisText != null) tvAnalysisText.setText(analysis);
    }

    private void openTaskList(String filter) {
        Intent intent = new Intent(requireContext(), TaskListActivity.class);
        intent.putExtra("filterType", filter);
        startActivity(intent);
    }

    private void attachStatsListener() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        if (statsListener != null) statsListener.remove();

        statsListener = db.collection("users").document(uid).collection("tasks")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) return;

                    lastTotal = querySnapshot.size();
                    lastCompleted = 0;
                    lastMissed = 0;
                    weeklyCompleted = new int[7];
                    priorityCompleted.clear();
                    priorityCompleted.put("High", 0);
                    priorityCompleted.put("Medium", 0);
                    priorityCompleted.put("Low", 0);

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0); 
                    cal.set(Calendar.MINUTE, 0); 
                    cal.set(Calendar.SECOND, 0); 
                    cal.set(Calendar.MILLISECOND, 0);
                    long todayStart = cal.getTimeInMillis();

                    Calendar taskCal = Calendar.getInstance();
                    Date now = new Date();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        TaskModel task = doc.toObject(TaskModel.class);
                        task.setId(doc.getId());

                        String status = task.getStatus();
                        String priority = task.getPriority();
                        Date dueDate = task.getDueDate();
                        
                        boolean isDone = "Completed".equalsIgnoreCase(status) || "Completed Late".equalsIgnoreCase(status);
                        boolean isMissed = "Missed".equalsIgnoreCase(status) || 
                                (!isDone && dueDate != null && now.after(dueDate));
                        
                        if (isDone) {
                            lastCompleted++;
                            if (priority != null && priorityCompleted.containsKey(priority)) {
                                priorityCompleted.put(priority, priorityCompleted.get(priority) + 1);
                            }
                        } else if (isMissed) {
                            lastMissed++;
                        }

                        if (dueDate != null && isDone) {
                            taskCal.setTime(dueDate);
                            taskCal.set(Calendar.HOUR_OF_DAY, 0);
                            taskCal.set(Calendar.MINUTE, 0);
                            taskCal.set(Calendar.SECOND, 0);
                            taskCal.set(Calendar.MILLISECOND, 0);
                            long taskStart = taskCal.getTimeInMillis();

                            long diff = todayStart - taskStart;
                            int daysAgo = (int) Math.round((double) diff / (1000 * 60 * 60 * 24));

                            if (daysAgo >= 0 && daysAgo < 7) {
                                int index = 6 - daysAgo;
                                weeklyCompleted[index]++;
                            }
                        }
                    }

                    tvTotalCount.setText(String.valueOf(lastTotal));
                    tvCompletedCount.setText(String.valueOf(lastCompleted));
                    tvMissedCount.setText(String.valueOf(lastMissed));
                    
                    if (lastTotal > 0) {
                        tvNoData.setVisibility(View.GONE);
                        showChart(toggleChartType.getCheckedButtonId());
                    } else {
                        tvNoData.setVisibility(View.VISIBLE);
                        pieChart.setVisibility(View.GONE);
                        barChart.setVisibility(View.GONE);
                        priorityBarChart.setVisibility(View.GONE);
                    }
                });

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("streak")) {
                tvStreakCount.setText(String.valueOf(doc.getLong("streak")));
            }
        });
    }

    private void drawPieChart(int total, int completed, int missed) {
        List<PieEntry> entries = new ArrayList<>();
        int pending = total - (completed + missed);
        if (pending < 0) pending = 0;

        if (completed > 0) entries.add(new PieEntry(completed, "Done"));
        if (missed > 0) entries.add(new PieEntry(missed, "Missed"));
        if (pending > 0) entries.add(new PieEntry(pending, "Left"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#66BB6A"), Color.parseColor("#EF5350"), Color.parseColor("#9575CD")});
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        pieChart.setData(new PieData(dataSet));
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Task Mix");
        pieChart.setCenterTextSize(16f);
        pieChart.getLegend().setEnabled(true);
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private void drawWeeklyChart() {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, weeklyCompleted[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Tasks Finished");
        dataSet.setColor(Color.parseColor("#66BB6A"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        
        final String[] days = new String[]{"6d ago", "5d ago", "4d ago", "3d ago", "2d ago", "Yesterday", "Today"};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < days.length) return days[index];
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-30f);
        xAxis.setTextSize(9f);
        
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(800);
        barChart.invalidate();
    }

    private void drawPriorityChart() {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, priorityCompleted.get("High")));
        entries.add(new BarEntry(1, priorityCompleted.get("Medium")));
        entries.add(new BarEntry(2, priorityCompleted.get("Low")));

        BarDataSet dataSet = new BarDataSet(entries, "Done by Priority");
        dataSet.setColors(new int[]{Color.parseColor("#EF5350"), Color.parseColor("#FFCA28"), Color.parseColor("#66BB6A")});
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData data = new BarData(dataSet);
        priorityBarChart.setData(data);
        
        final String[] labels = new String[]{"High", "Med", "Low"};
        XAxis xAxis = priorityBarChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.length) return labels[index];
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        priorityBarChart.getAxisRight().setEnabled(false);
        priorityBarChart.getAxisLeft().setGranularity(1f);
        priorityBarChart.getAxisLeft().setAxisMinimum(0f);
        priorityBarChart.getDescription().setEnabled(false);
        priorityBarChart.animateY(800);
        priorityBarChart.invalidate();
    }

    @Override
    public void onStart() {
        super.onStart();
        attachStatsListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (statsListener != null) statsListener.remove();
    }
}
