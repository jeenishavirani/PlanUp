package com.example.planup;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.planup.model.TaskModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {

    EditText etTaskTitle, etTaskDesc;
    MaterialButtonToggleGroup togglePriority;
    MaterialSwitch switchAlarm;
    MaterialButton btnSaveTask, btnDate, btnTime;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    private Calendar alarmCalendar = Calendar.getInstance();

    private String tempTaskId;
    private String tempTaskTitle;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    scheduleAlarmAndFinish(tempTaskId, tempTaskTitle);
                } else {
                    Toast.makeText(this, "Task saved, but alarm won't show without notification permission.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDesc = findViewById(R.id.etTaskDesc);
        togglePriority = findViewById(R.id.togglePriority);
        switchAlarm = findViewById(R.id.switchAlarm);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnDate = findViewById(R.id.btnDate);
        btnTime = findViewById(R.id.btnTime);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnDate.setOnClickListener(v -> openDatePicker());
        btnTime.setOnClickListener(v -> openTimePicker());
        btnSaveTask.setOnClickListener(v -> saveTask());
    }

    private void openDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            alarmCalendar.set(Calendar.YEAR, year);
            alarmCalendar.set(Calendar.MONTH, month);
            alarmCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            btnDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        }, alarmCalendar.get(Calendar.YEAR), alarmCalendar.get(Calendar.MONTH), alarmCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void openTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            alarmCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            alarmCalendar.set(Calendar.MINUTE, minute);
            alarmCalendar.set(Calendar.SECOND, 0);

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            String formattedTime = sdf.format(alarmCalendar.getTime());

            btnTime.setText(formattedTime);
        }, alarmCalendar.get(Calendar.HOUR_OF_DAY), alarmCalendar.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private void saveTask() {
        String title = etTaskTitle.getText().toString().trim();
        String desc = etTaskDesc.getText().toString().trim();
        String date = btnDate.getText().toString();
        String time = btnTime.getText().toString();

        if (title.isEmpty()) {
            etTaskTitle.setError("Task title required");
            return;
        }

        if (desc.isEmpty()) {
            etTaskDesc.setError("Task description required");
            return;
        }

        if (date.equals("Date")) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.equals("Time")) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (togglePriority.getCheckedButtonId() == -1) {
            Toast.makeText(this, "Please select a priority", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasAlarm = switchAlarm.isChecked();
        String uid = mAuth.getCurrentUser().getUid();

        Date dueDate = alarmCalendar.getTime();

        TaskModel task = new TaskModel();
        task.setTitle(title);
        task.setDescription(desc);
        task.setPriority(getPriority());
        task.setAlarm(hasAlarm);
        task.setStatus("Pending");
        task.setDueDate(dueDate);
        task.setCreatedAt(System.currentTimeMillis());


        db.collection("users").document(uid).collection("tasks").add(task)
                .addOnSuccessListener(documentReference -> {
                    if (hasAlarm) {
                        checkAndScheduleAlarm(documentReference.getId(), title);
                    } else {
                        Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save task", Toast.LENGTH_SHORT).show());
    }

    private String getPriority() {
        int selectedId = togglePriority.getCheckedButtonId();
        MaterialButton selectedButton = findViewById(selectedId);
        return selectedButton.getText().toString();
    }

    private void checkAndScheduleAlarm(String taskId, String taskTitle) {
        this.tempTaskId = taskId;
        this.tempTaskTitle = taskTitle;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return; 
            }
        }

        scheduleAlarmAndFinish(taskId, taskTitle);
    }

    private void scheduleAlarmAndFinish(String taskId, String taskTitle) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Task saved. Please grant 'Alarms & reminders' permission for alarms to work.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                finish();
                return;
            }
        }

        scheduleAlarm(taskId, taskTitle);
        Toast.makeText(this, "Task saved and alarm set!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void scheduleAlarm(String taskId, String taskTitle) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("taskTitle", taskTitle);
        intent.putExtra("taskId", taskId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(), pendingIntent);
    }
}
