package com.example.planup.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class TaskCleanupHelper {

    private static final String TAG = "TaskCleanupHelper";
    private static final long ONE_WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;

    public static void cleanOldTasks() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        long cutoff = System.currentTimeMillis() - ONE_WEEK_MILLIS;

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .whereLessThan("createdAt", cutoff)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return;

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit().addOnSuccessListener(unused -> 
                        Log.d(TAG, "Old tasks (older than 1 week) deleted successfully.")
                    ).addOnFailureListener(e -> 
                        Log.e(TAG, "Failed to delete old tasks", e)
                    );
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error querying old tasks", e));
    }
}
