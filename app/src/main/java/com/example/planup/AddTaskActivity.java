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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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
    ImageView btnBack;
    LinearLayout layoutHeader;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    private Calendar alarmCalendar = Calendar.getInstance();

    private String tempTaskId;
    private String tempTaskTitle;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    scheduleAllRemindersAndFinish(tempTaskId, tempTaskTitle);
                } else {
                    Toast.makeText(this, "Task saved, but alarm won't show without notification permission.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_add_task);

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDesc = findViewById(R.id.etTaskDesc);
        togglePriority = findViewById(R.id.togglePriority);
        switchAlarm = findViewById(R.id.switchAlarm);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnDate = findViewById(R.id.btnDate);
        btnTime = findViewById(R.id.btnTime);
        btnBack = findViewById(R.id.btnBack);
        layoutHeader = findViewById(R.id.layoutHeader);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (layoutHeader != null) {
                layoutHeader.setPadding(layoutHeader.getPaddingLeft(), systemBars.top, layoutHeader.getPaddingRight(), layoutHeader.getPaddingBottom());
            }
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnDate.setOnClickListener(v -> openDatePicker());
        btnTime.setOnClickListener(v -> openTimePicker());
        btnSaveTask.setOnClickListener(v -> saveTask());
        btnBack.setOnClickListener(v -> finish());
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

        if (date.equals("Date") || time.equals("Time")) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
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
                        checkAndScheduleReminders(documentReference.getId(), title);
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

    private void checkAndScheduleReminders(String taskId, String taskTitle) {
        this.tempTaskId = taskId;
        this.tempTaskTitle = taskTitle;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return; 
            }
        }

        scheduleAllRemindersAndFinish(taskId, taskTitle);
    }

    private void scheduleAllRemindersAndFinish(String taskId, String taskTitle) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Task saved. Please grant 'Alarms & reminders' permission.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                finish();
                return;
            }
        }

        // 1. Primary Alarm (at task time)
        schedulePrimaryAlarm(taskId, taskTitle);

        // 2. Proactive Reminders
        int taskIdHash = taskId.hashCode();
        ReminderScheduler.scheduleTwoHourBeforeReminder(this, alarmCalendar.getTimeInMillis(), taskIdHash, taskTitle);
        ReminderScheduler.scheduleFiveMinuteBeforeReminder(this, alarmCalendar.getTimeInMillis(), taskIdHash, taskTitle);
        
        // 3. Missed Task Notification (5 min after)
        ReminderScheduler.scheduleMissedTaskReminder(this, alarmCalendar.getTimeInMillis(), taskIdHash, taskTitle, taskId);

        Toast.makeText(this, "Task saved and reminders set!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void schedulePrimaryAlarm(String taskId, String taskTitle) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("taskTitle", taskTitle);
        intent.putExtra("taskId", taskId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(alarmCalendar.getTimeInMillis(), pendingIntent);
            alarmManager.setAlarmClock(clockInfo, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(), pendingIntent);
        }
    }
}
