package com.example.planup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.planup.model.TaskModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class TaskDetailActivity extends AppCompatActivity {

    private static final String TAG = "TaskDetailActivity";

    private TextView tvTitle, tvTaskDesc, tvDate, tvTime, tvPriority, tvAlarm;
    private Button btnEditTask, btnDeleteTask;
    private ImageView btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration taskListener;

    private String taskId;
    private boolean isViewOnly = false;

    // Modern Activity Result API
    private final ActivityResultLauncher<Intent> editTaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // We don't strictly need loadTask() here anymore because of the snapshot listener,
                // but we keep the result handling for consistency.
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_task_detail);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        taskId = getIntent().getStringExtra("taskId");
        isViewOnly = getIntent().getBooleanExtra("isViewOnly", false);

        if (taskId == null) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle = findViewById(R.id.tvTaskTitle);
        tvTaskDesc = findViewById(R.id.tvTaskDesc);
        tvDate = findViewById(R.id.tvTaskDate);
        tvTime = findViewById(R.id.tvTaskTime);
        tvPriority = findViewById(R.id.tvTaskPriority);
        tvAlarm = findViewById(R.id.tvTaskAlarm);

        btnEditTask = findViewById(R.id.btnEditTask);
        btnDeleteTask = findViewById(R.id.btnDeleteTask);
        btnBack = findViewById(R.id.btnBack);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        // 🔹 Hide Edit/Delete if in View-Only mode
        if (isViewOnly) {
            btnEditTask.setVisibility(View.GONE);
            btnDeleteTask.setVisibility(View.GONE);
        }

        // 🔹 Attach Snapshot Listener for Real-time updates
        attachTaskListener();

        btnEditTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditTaskActivity.class);
            intent.putExtra("taskId", taskId);
            editTaskLauncher.launch(intent);
        });

        btnDeleteTask.setOnClickListener(v -> deleteTask());
        btnBack.setOnClickListener(v -> finish());
    }

    private void attachTaskListener() {

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        taskListener = db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(taskId)
                .addSnapshotListener((doc, e) -> {

                    if (e != null) {
                        Log.e(TAG, "Task listener error", e);
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        TaskModel task = doc.toObject(TaskModel.class);
                        if (task == null) return;

                        tvTitle.setText(task.getTitle());
                        tvTaskDesc.setText(task.getDescription());
                        tvDate.setText("Date: " + task.getFormattedDate());
                        tvTime.setText("Time: " + task.getFormattedTime());
                        tvPriority.setText("Priority: " + task.getPriority());
                        tvAlarm.setText("Alarm: " + (task.isAlarm() ? "ON" : "OFF"));
                    } else if (doc != null && !doc.exists()) {
                        // Task was deleted
                        Toast.makeText(this, "Task no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void deleteTask() {

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskListener != null) {
            taskListener.remove();
        }
    }
}
