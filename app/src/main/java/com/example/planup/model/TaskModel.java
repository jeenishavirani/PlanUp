package com.example.planup.model;

import com.google.firebase.firestore.Exclude;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskModel {

    @Exclude
    private String id;

    private String title;
    private String description;
    private String priority;
    private boolean alarm;
    private String status;     // Pending | Completed | Missed | Completed Late
    private Date dueDate;
    private long createdAt;
    private Long completedAt;
    private String category;   // 🔹 AI-Generated Category

    // 🔹 Required empty constructor
    public TaskModel() {}

    // ---------------- GETTERS ----------------

    @Exclude
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isAlarm() {
        return alarm;
    }

    public String getStatus() {
        return status;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public String getCategory() {
        return category;
    }

    // ---------------- SETTERS ----------------

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // ---------------- FORMATTED HELPERS ----------------

    @Exclude
    public String getFormattedDate() {
        if (dueDate == null) return "";
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(dueDate);
    }

    @Exclude
    public String getFormattedTime() {
        if (dueDate == null) return "";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(dueDate);
    }

    // ---------------- STATUS HELPERS ----------------

    @Exclude
    public boolean isDone() {
        return "Completed".equalsIgnoreCase(status) || "Completed Late".equalsIgnoreCase(status);
    }

    /**
     * A task is MISSED if:
     * - dueDate passed
     * - AND status is NOT Done (neither Completed nor Completed Late)
     */
    @Exclude
    public boolean isMissed() {
        if (dueDate == null) return false;
        if (isDone()) return false;
        return new Date().after(dueDate);
    }

    @Exclude
    public boolean isCompletedLate() {
        if (dueDate == null) return false;
        if (!"Completed".equalsIgnoreCase(status) && !"Completed Late".equalsIgnoreCase(status)) return false;
        // This is only true if it was marked as completed after the due date.
        // Usually, the app logic should set "Completed Late" when the user checks it after the deadline.
        return new Date().after(dueDate);
    }

}