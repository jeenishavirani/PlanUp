package com.example.planup.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planup.R;
import com.example.planup.model.TaskModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    Context context;
    List<TaskModel> taskList;
    private boolean isViewOnly = false;

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel task);
    }

    public interface OnTaskStatusChangeListener {
        void onTaskStatusChanged(TaskModel task, boolean isCompleted);
    }

    OnTaskClickListener clickListener;
    OnTaskStatusChangeListener statusChangeListener;

    public TaskAdapter(Context context,
                       List<TaskModel> taskList,
                       OnTaskClickListener clickListener,
                       OnTaskStatusChangeListener statusChangeListener) {

        this.context = context;
        this.taskList = new ArrayList<>(taskList);
        this.clickListener = clickListener;
        this.statusChangeListener = statusChangeListener;
    }

    // Constructor for View Only mode
    public TaskAdapter(Context context,
                       List<TaskModel> taskList,
                       OnTaskClickListener clickListener,
                       boolean isViewOnly) {
        this.context = context;
        this.taskList = new ArrayList<>(taskList);
        this.clickListener = clickListener;
        this.isViewOnly = isViewOnly;
    }

    public void updateTasks(List<TaskModel> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TaskDiffCallback(this.taskList, newList));
        this.taskList.clear();
        this.taskList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = taskList.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvTaskTime.setText(task.getFormattedDate() + " • " + task.getFormattedTime());

        // Update Priority Bar and Text
        String priority = task.getPriority();
        if ("High".equalsIgnoreCase(priority)) {
            holder.tvPriority.setText("H");
            holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_high);
            holder.priorityBar.setBackgroundColor(ContextCompat.getColor(context, R.color.priority_high));
        } else if ("Medium".equalsIgnoreCase(priority)) {
            holder.tvPriority.setText("M");
            holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_medium);
            holder.priorityBar.setBackgroundColor(ContextCompat.getColor(context, R.color.priority_medium));
        } else {
            holder.tvPriority.setText("L");
            holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_low);
            holder.priorityBar.setBackgroundColor(ContextCompat.getColor(context, R.color.priority_low));
        }

        // 🔹 Handle AI Category
        String category = task.getCategory();
        if (category != null && !category.isEmpty()) {
            holder.tvCategory.setVisibility(View.VISIBLE);
            holder.tvCategory.setText(category);
            
            int colorResId;
            switch (category.toLowerCase()) {
                case "work": colorResId = R.color.cat_work; break;
                case "personal": colorResId = R.color.cat_personal; break;
                case "health": colorResId = R.color.cat_health; break;
                case "finance": colorResId = R.color.cat_finance; break;
                case "social": colorResId = R.color.cat_social; break;
                case "study": colorResId = R.color.cat_study; break;
                default: colorResId = R.color.cat_others; break;
            }
            
            // Apply background color to the category tag
            GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.bg_priority_low);
            if (drawable != null) {
                drawable = (GradientDrawable) drawable.mutate();
                drawable.setColor(ContextCompat.getColor(context, colorResId));
                holder.tvCategory.setBackground(drawable);
            }
        } else {
            holder.tvCategory.setVisibility(View.GONE);
        }

        // Handle Completion State
        boolean isCompleted = "Completed".equalsIgnoreCase(task.getStatus()) || "Completed Late".equalsIgnoreCase(task.getStatus());
        holder.cbTaskCompleted.setOnCheckedChangeListener(null);
        holder.cbTaskCompleted.setChecked(isCompleted);

        updateTaskStyle(holder, isCompleted, task.getStatus());

        if (isViewOnly) {
            holder.cbTaskCompleted.setEnabled(false);
            holder.cbTaskCompleted.setAlpha(0.6f);
        } else {
            holder.cbTaskCompleted.setEnabled(true);
            holder.cbTaskCompleted.setAlpha(1.0f);
            holder.cbTaskCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateTaskStyle(holder, isChecked, isChecked ? "Completed" : "Pending");
                if (statusChangeListener != null) {
                    statusChangeListener.onTaskStatusChanged(task, isChecked);
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onTaskClick(task);
        });
    }

    private void updateTaskStyle(TaskViewHolder holder, boolean isCompleted, String status) {
        if (isCompleted) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemView.setAlpha(0.6f);
            holder.tvStatus.setText("Completed");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.task_completed));
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.itemView.setAlpha(1.0f);
            if ("Missed".equalsIgnoreCase(status)) {
                holder.tvStatus.setText("Missed");
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.task_missed));
            } else {
                holder.tvStatus.setText("Pending");
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.task_pending));
            }
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTaskTime, tvPriority, tvStatus, tvCategory;
        CheckBox cbTaskCompleted;
        View priorityBar;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            cbTaskCompleted = itemView.findViewById(R.id.cbTaskCompleted);
            priorityBar = itemView.findViewById(R.id.priorityBar);
        }
    }

    private static class TaskDiffCallback extends DiffUtil.Callback {
        private final List<TaskModel> oldList;
        private final List<TaskModel> newList;
        public TaskDiffCallback(List<TaskModel> oldList, List<TaskModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }
        @Override public boolean areItemsTheSame(int oldP, int newP) { return Objects.equals(oldList.get(oldP).getId(), newList.get(newP).getId()); }
        @Override public boolean areContentsTheSame(int oldP, int newP) { return oldList.get(oldP).equals(newList.get(newP)); }
    }
}