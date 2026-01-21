package com.example.planup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planup.adapter.TaskAdapter;
import com.example.planup.model.TaskModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment
        implements TaskAdapter.OnTaskStatusChangeListener {

    private static final String TAG = "TasksFragment";

    // UI
    RecyclerView rvTasks;
    LinearLayout layoutEmpty, layoutHeader;
    FloatingActionButton fabAddTask;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ListenerRegistration taskListener;

    // Adapter
    TaskAdapter taskAdapter;

    public TasksFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // 🔹 Views
        rvTasks = view.findViewById(R.id.rvTasks);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        layoutHeader = view.findViewById(R.id.layoutHeader);

        // Handle top insets for header
        ViewCompat.setOnApplyWindowInsetsListener(layoutHeader, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // 🔹 Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 RecyclerView
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));

        taskAdapter = new TaskAdapter(
                requireContext(),
                new ArrayList<>(),
                task -> {
                    Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
                    intent.putExtra("taskId", task.getId());
                    startActivity(intent);
                },
                this
        );

        rvTasks.setAdapter(taskAdapter);

        // 🔹 FAB click
        fabAddTask.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddTaskActivity.class)));

        // Add levitation (pulse) animation to FAB
        Animation levitation = AnimationUtils.loadAnimation(requireContext(), R.anim.fab_pulse);
        fabAddTask.startAnimation(levitation);

        return view;
    }

    // ================= TASK LIST =================
    private void attachTaskListener() {

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        if (taskListener != null) {
            taskListener.remove();
        }

        taskListener = db.collection("users")
                .document(uid)
                .collection("tasks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {

                    if (e != null) {
                        Log.e(TAG, "Task listener error", e);
                        return;
                    }

                    if (querySnapshot == null) return;

                    List<TaskModel> newTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        TaskModel task = doc.toObject(TaskModel.class);
                        task.setId(doc.getId());

                        String status = task.getStatus();
                        boolean isDone = task.isDone();

                        // 🔥 AUTO-MISS LOGIC
                        if (task.isMissed() && !"Missed".equalsIgnoreCase(status)) {
                            status = "Missed";
                            task.setStatus(status);

                            db.collection("users")
                                    .document(uid)
                                    .collection("tasks")
                                    .document(task.getId())
                                    .update("status", "Missed");
                        }

                        // ✅ HIDE COMPLETED AND MISSED TASKS
                        // This fragment is for managing ACTIVE tasks only
                        if (!isDone && !task.isMissed()) {
                            newTasks.add(task);
                        }
                    }

                    Collections.sort(newTasks, (t1, t2) -> {
                        Date d1 = t1.getDueDate();
                        Date d2 = t2.getDueDate();
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d1.compareTo(d2);
                    });

                    // Empty state
                    if (newTasks.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvTasks.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvTasks.setVisibility(View.VISIBLE);
                    }

                    taskAdapter.updateTasks(newTasks);
                });
    }


    // ================= STATUS CHANGE =================
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
                .update(updates)
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to update task",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ================= LIFECYCLE =================
    @Override
    public void onStart() {
        super.onStart();
        attachTaskListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }
    }
}
