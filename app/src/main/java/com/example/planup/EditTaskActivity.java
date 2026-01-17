package com.example.planup;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditTaskActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private Button btnDate, btnTime, btnSave;
    private MaterialButtonToggleGroup togglePriority;
    private MaterialSwitch switchAlarm;
    private ImageView btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String taskId;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        btnDate = findViewById(R.id.btnDate);
        btnTime = findViewById(R.id.btnTime);
        btnSave = findViewById(R.id.btnSave);
        togglePriority = findViewById(R.id.togglePriority);
        switchAlarm = findViewById(R.id.switchAlarm);
        btnBack = findViewById(R.id.btnBack);

        loadTaskData();

        btnDate.setOnClickListener(v -> openDatePicker());
        btnTime.setOnClickListener(v -> openTimePicker());
        btnSave.setOnClickListener(v -> updateTask());
        btnBack.setOnClickListener(v -> finish());
    }

    // ================= LOAD TASK =================
    private void loadTaskData() {

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(taskId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    etTitle.setText(doc.getString("title"));
                    etDescription.setText(doc.getString("description"));

                    // ✅ LOAD dueDate
                    Date dueDate = doc.getDate("dueDate");
                    if (dueDate != null) {
                        calendar.setTime(dueDate);

                        SimpleDateFormat df =
                                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        SimpleDateFormat tf =
                                new SimpleDateFormat("hh:mm a", Locale.getDefault());

                        btnDate.setText(df.format(dueDate));
                        btnTime.setText(tf.format(dueDate));
                    }

                    String priority = doc.getString("priority");
                    if ("High".equalsIgnoreCase(priority)) {
                        togglePriority.check(R.id.btnHigh);
                    } else if ("Medium".equalsIgnoreCase(priority)) {
                        togglePriority.check(R.id.btnMedium);
                    } else if ("Low".equalsIgnoreCase(priority)) {
                        togglePriority.check(R.id.btnLow);
                    }

                    Boolean alarm = doc.getBoolean("alarm");
                    switchAlarm.setChecked(alarm != null && alarm);
                });
    }

    // ================= UPDATE TASK =================
    private void updateTask() {

        if (mAuth.getCurrentUser() == null) return;

        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Required");
            return;
        }

        int checkedId = togglePriority.getCheckedButtonId();
        if (checkedId == -1) {
            Toast.makeText(this, "Select priority", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialButton rb = findViewById(checkedId);
        String priority = rb.getText().toString();

        boolean hasAlarm = switchAlarm.isChecked();

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", desc);
        updates.put("priority", priority);
        updates.put("alarm", hasAlarm);
        updates.put("dueDate", calendar.getTime());

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (hasAlarm) {
                        scheduleAlarm(taskId, title);
                    } else {
                        cancelAlarm(taskId);
                    }
                    Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                );
    }

    private void scheduleAlarm(String taskId, String taskTitle) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("taskTitle", taskTitle);
        intent.putExtra("taskId", taskId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent);
            alarmManager.setAlarmClock(clockInfo, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void cancelAlarm(String taskId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    // ================= DATE PICKER =================
    private void openDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);

                    btnDate.setText(day + "/" + (month + 1) + "/" + year);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ================= TIME PICKER =================
    private void openTimePicker() {
        new TimePickerDialog(this,
                (view, hour, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);

                    SimpleDateFormat sdf =
                            new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    btnTime.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        ).show();
    }
}
