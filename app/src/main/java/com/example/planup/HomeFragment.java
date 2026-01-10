package com.example.planup;

import java.util.Date;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planup.adapter.TaskAdapter;
import com.example.planup.model.TaskModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment
        implements TaskAdapter.OnTaskStatusChangeListener {

    private static final String TAG = "HomeFragment";

    // UI
    ImageView imgProfile, fabAddTask;
    TextView tvWelcome, tvPendingTasks, tvCompletedTasks, tvTotalTasks;
    RecyclerView rvTasks;
    LinearLayout layoutEmpty;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ListenerRegistration taskListener;

    // Adapter
    List<TaskModel> taskList;
    TaskAdapter taskAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 🔹 Views
        imgProfile = view.findViewById(R.id.imgProfile);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        rvTasks = view.findViewById(R.id.rvTasks);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        tvPendingTasks = view.findViewById(R.id.tvPendingTasks);
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks);
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks);

        // 🔹 Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 RecyclerView
        taskList = new ArrayList<>();
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));

        taskAdapter = new TaskAdapter(
                requireContext(),
                taskList,
                task -> {
                    Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
                    intent.putExtra("taskId", task.getId());
                    startActivity(intent);
                },
                this
        );

        rvTasks.setAdapter(taskAdapter);

        // 🔹 Load user info
        loadUserData();

        // 🔹 Clicks
        imgProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));

        fabAddTask.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddTaskActivity.class)));

        return view;
    }

    // ================= USER DATA =================
    private void loadUserData() {

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    String nickname = doc.getString("nickname");
                    String gender = doc.getString("gender");

                    if (nickname != null) {
                        tvWelcome.setText("Welcome 👋\n" + nickname);
                    }

                    if ("female".equalsIgnoreCase(gender)) {
                        imgProfile.setImageResource(R.drawable.ic_avatar_girl);
                    } else {
                        imgProfile.setImageResource(R.drawable.ic_avatar_boy);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to load user data",
                                Toast.LENGTH_SHORT).show()
                );
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

                            taskList.clear();

                            int pending = 0;
                            int completed = 0;
                            int missed = 0;

                            Date today = new Date();

                            for (QueryDocumentSnapshot doc : querySnapshot) {

                                TaskModel task = doc.toObject(TaskModel.class);
                                task.setId(doc.getId());

                                // 🔥 AUTO-MISS LOGIC
                                if (task.isMissed() && !"Missed".equalsIgnoreCase(task.getStatus())) {
                                    task.setStatus("Missed");

                                    db.collection("users")
                                            .document(uid)
                                            .collection("tasks")
                                            .document(task.getId())
                                            .update("status", "Missed");
                                }

                                // 🔥 HIDE COMPLETED TASKS FROM PREVIOUS DAYS
                                if ("Completed".equalsIgnoreCase(task.getStatus())
                                        || "Completed Late".equalsIgnoreCase(task.getStatus())) {

                                    if (task.getDueDate() != null && task.getDueDate().before(today)) {
                                        continue; // ❌ DO NOT ADD TO LIST
                                    }
                                }

                                // 🔹 ADD TO LIST
                                taskList.add(task);

                                // 🔹 COUNT STATS
                                if ("Completed".equalsIgnoreCase(task.getStatus())
                                        || "Completed Late".equalsIgnoreCase(task.getStatus())) {
                                    completed++;
                                } else if ("Missed".equalsIgnoreCase(task.getStatus())) {
                                    missed++;
                                } else {
                                    pending++;
                                }
                            }

                    // Empty state
                    if (taskList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvTasks.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvTasks.setVisibility(View.VISIBLE);
                    }

                    // Stats
                    tvPendingTasks.setText("Pending: " + pending);
                    tvCompletedTasks.setText("Completed: " + completed);
                    tvTotalTasks.setText("Total: " + taskList.size());

                    taskAdapter.notifyDataSetChanged();
                });
    }


    // ================= STATUS CHANGE =================
    @Override
    public void onTaskStatusChanged(TaskModel task, boolean isCompleted) {

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(task.getId())
                .update("status", isCompleted ? "Completed" : "Pending")
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
