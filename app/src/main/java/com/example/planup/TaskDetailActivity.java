package com.example.planup;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;


import androidx.appcompat.app.AppCompatActivity;

import com.example.planup.model.TaskModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TaskDetailActivity extends AppCompatActivity {

    // UI
    private TextView tvTitle, tvTaskDesc, tvDate, tvTime, tvPriority, tvAlarm;
    private Button btnEditTask, btnDeleteTask;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        taskId = getIntent().getStringExtra("taskId");
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

        loadTask();

        btnEditTask.setOnClickListener(v -> {
            Intent intent = new Intent(TaskDetailActivity.this, EditTaskActivity.class);
            intent.putExtra("taskId", taskId);
            startActivity(intent);
        });

        btnDeleteTask.setOnClickListener(v -> deleteTask());
    }


    private void loadTask() {

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(taskId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    TaskModel task = doc.toObject(TaskModel.class);
                    if (task == null) return;

                    tvTitle.setText(task.getTitle());
                    tvTaskDesc.setText(task.getDescription());
                    tvDate.setText("Date: " + task.getFormattedDate());
                    tvTime.setText("Time: " + task.getFormattedTime());
                    tvPriority.setText("Priority: " + task.getPriority());
                    tvAlarm.setText("Alarm: " + (task.isAlarm() ? "ON" : "OFF"));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load task", Toast.LENGTH_SHORT).show()
                );
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
}
