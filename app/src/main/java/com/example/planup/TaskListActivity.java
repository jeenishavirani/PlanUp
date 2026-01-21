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
    View headerBg;
    
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    
    List<TaskModel> taskList;
    TaskAdapter taskAdapter;
    
    String filterType = "all"; // all, pending, completed, missed

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
        headerBg = findViewById(R.id.headerBg);

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
        
        // Corrected constructor call for ViewOnly mode
        taskAdapter = new TaskAdapter(
                this, 
                new ArrayList<>(), 
                task -> {
                    Intent intent = new Intent(this, TaskDetailActivity.class);
                    intent.putExtra("taskId", task.getId());
                    intent.putExtra("isViewOnly", true);
                    startActivity(intent);
                }, 
                true 
        );
        rvTasks.setAdapter(taskAdapter);

        btnBack.setOnClickListener(v -> finish());

        setupHeader();
        fetchTasks();
    }

    private void setupHeader() {
        switch (filterType) {
            case "pending":
                tvTitle.setText(R.string.pending_tasks_title);
                tvSubtitle.setText(R.string.pending_tasks_subtitle);
                break;
            case "completed":
                tvTitle.setText(R.string.completed_tasks_title);
                tvSubtitle.setText(R.string.completed_tasks_subtitle);
                break;
            case "missed":
                tvTitle.setText(R.string.missed_tasks_title);
                tvSubtitle.setText(R.string.missed_tasks_subtitle);
                break;
            default:
                tvTitle.setText(R.string.total_tasks_title);
                tvSubtitle.setText(R.string.total_tasks_subtitle);
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
                    List<TaskModel> newTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TaskModel task = doc.toObject(TaskModel.class);
                        task.setId(doc.getId());
                        
                        boolean isDone = task.isDone();
                        boolean isMissed = task.isMissed();
                        
                        if ("pending".equals(filterType)) {
                            if (!isDone && !isMissed) newTasks.add(task);
                        } else if ("completed".equals(filterType)) {
                            if (isDone) newTasks.add(task);
                        } else if ("missed".equals(filterType)) {
                            if (isMissed) newTasks.add(task);
                        } else {
                            newTasks.add(task);
                        }
                    }
                    
                    if (newTasks.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvTasks.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvTasks.setVisibility(View.VISIBLE);
                    }
                    
                    taskAdapter.updateTasks(newTasks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching tasks", e);
                    Toast.makeText(this, R.string.error_loading_tasks, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onTaskStatusChanged(TaskModel task, boolean isCompleted) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", isCompleted ? "Completed" : "Pending");
        updates.put("completedAt", isCompleted ? System.currentTimeMillis() : null);

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(task.getId())
                .update(updates);
    }
}
