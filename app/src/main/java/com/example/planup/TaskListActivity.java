package com.example.planup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planup.adapter.TaskAdapter;
import com.example.planup.model.TaskModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskListActivity extends AppCompatActivity
        implements TaskAdapter.OnTaskStatusChangeListener {

    private static final String TAG = "TaskListActivity";

    RecyclerView rvTasks;
    LinearLayout layoutEmpty;
    ImageView btnBack;
    TextView tvTitle, tvSubtitle;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    List<TaskModel> taskList;
    TaskAdapter taskAdapter;

    String filterType = "all"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_task_list);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);

        filterType = getIntent().getStringExtra("filterType");
        if (filterType == null) filterType = "all";

        rvTasks = findViewById(R.id.rvTasks);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        View mainLayout = findViewById(R.id.main_layout);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (btnBack != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) btnBack.getLayoutParams();
                lp.topMargin = systemBars.top + 12;
                btnBack.setLayoutParams(lp);
            }
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        taskList = new ArrayList<>();
        rvTasks.setLayoutManager(new LinearLayoutManager(this));

        taskAdapter = new TaskAdapter(
                this,
                taskList,
                task -> {
                    Intent intent = new Intent(this, TaskDetailActivity.class);
                    intent.putExtra("taskId", task.getId());
                    intent.putExtra("isViewOnly", true);
                    startActivity(intent);
                },
                this
        );
        rvTasks.setAdapter(taskAdapter);

        btnBack.setOnClickListener(v -> finish());

        setupHeader();
        fetchTasks();
    }

    private void setupHeader() {
        switch (filterType.toLowerCase()) {
            case "completed":
                tvTitle.setText("Completed Tasks");
                tvSubtitle.setText("Your achievements");
                break;
            case "missed":
                tvTitle.setText("Missed Tasks");
                tvSubtitle.setText("Catch up when you can");
                break;
            case "pending":
                tvTitle.setText("Pending Tasks");
                tvSubtitle.setText("Still to be done");
                break;
            default:
                tvTitle.setText("Total Tasks");
                tvSubtitle.setText("Everything you've planned");
                break;
        }
    }

    private void fetchTasks() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    Date now = new Date();
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TaskModel task = doc.toObject(TaskModel.class);
                        task.setId(doc.getId());

                        String status = task.getStatus();
                        if (status == null) status = "Pending";
                        
                        boolean isDone = task.isDone();
                        // Task is missed if status is "Missed" OR (status is "Pending" AND dueDate has passed)
                        boolean logicallyMissed = "Missed".equalsIgnoreCase(status) || 
                                ("Pending".equalsIgnoreCase(status) && task.getDueDate() != null && now.after(task.getDueDate()));

                        switch (filterType.toLowerCase()) {
                            case "completed":
                                if (isDone) taskList.add(task);
                                break;
                            case "missed":
                                if (logicallyMissed) taskList.add(task);
                                break;
                            case "pending":
                                // A task is truly pending only if it's "Pending" AND NOT missed yet
                                if ("Pending".equalsIgnoreCase(status) && !logicallyMissed) taskList.add(task);
                                break;
                            case "all":
                            default:
                                taskList.add(task);
                                break;
                        }
                    }

                    if (taskList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvTasks.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvTasks.setVisibility(View.VISIBLE);
                    }

                    taskAdapter.updateTasks(taskList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching tasks", e);
                    Toast.makeText(this, "Error loading tasks", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onTaskStatusChanged(TaskModel task, boolean isCompleted) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        String newStatus = isCompleted ? "Completed" : "Pending";

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(task.getId())
                .update("status", newStatus, "completedAt", isCompleted ? System.currentTimeMillis() : null)
                .addOnSuccessListener(unused -> {
                    fetchTasks(); // Refresh list to respect current filter
                });
    }
}
