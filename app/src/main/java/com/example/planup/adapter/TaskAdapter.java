package com.example.planup.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planup.R;
import com.example.planup.model.TaskModel;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    Context context;
    List<TaskModel> taskList;

    // 🔹 Click task → open TaskDetail
    public interface OnTaskClickListener {
        void onTaskClick(TaskModel task);
    }

    // 🔹 Checkbox → update status
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
        this.taskList = taskList;
        this.clickListener = clickListener;
        this.statusChangeListener = statusChangeListener;
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

        // 🔹 Title
        holder.tvTitle.setText(task.getTitle());

        // 🔹 Date & Time (safe)
        String dateTime = task.getFormattedDate();
        if (!task.getFormattedTime().isEmpty()) {
            dateTime += " • " + task.getFormattedTime();
        }
        holder.tvTaskTime.setText(dateTime);

        // 🔹 Status text
        holder.tvStatus.setText(task.getStatus());

        // 🔹 Priority badge
        String priority = task.getPriority();
        if ("High".equalsIgnoreCase(priority)) {
            holder.tvPriority.setText("H");
            holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_high);
        } else if ("Medium".equalsIgnoreCase(priority)) {
            holder.tvPriority.setText("M");
            holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_medium);
        } else {
            holder.tvPriority.setText("L");
            holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_low);
        }

        // 🔹 Prevent checkbox recycling issue
        holder.cbTaskCompleted.setOnCheckedChangeListener(null);
        holder.cbTaskCompleted.setChecked(
                "Completed".equalsIgnoreCase(task.getStatus())
        );

        // 🔹 Checkbox → update status
        holder.cbTaskCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (statusChangeListener != null) {
                statusChangeListener.onTaskStatusChanged(task, isChecked);
            }
        });

        // 🔹 Click → open TaskDetailActivity
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList == null ? 0 : taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvTaskTime, tvPriority, tvStatus;
        CheckBox cbTaskCompleted;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cbTaskCompleted = itemView.findViewById(R.id.cbTaskCompleted);
        }
    }
}
