package com.example.planup;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditTaskActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private Button btnDate, btnTime, btnSave;
    private SwitchMaterial switchAlarm;

    private RadioGroup rgPriority;


    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String taskId;
    private Calendar alarmCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        Toast.makeText(this, "EditTaskActivity opened", Toast.LENGTH_SHORT).show();

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        alarmCalendar = Calendar.getInstance();

        // Get taskId
        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind views (SAFE)
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        btnDate = findViewById(R.id.btnDate);
        btnTime = findViewById(R.id.btnTime);
        btnSave = findViewById(R.id.btnSave);
        rgPriority = findViewById(R.id.rgPriority);
        switchAlarm = findViewById(R.id.switchAlarm);

        if (btnDate == null || btnTime == null || btnSave == null) {
            Toast.makeText(this, "Layout error", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnDate.setOnClickListener(v -> openDatePicker());
        btnTime.setOnClickListener(v -> openTimePicker());
    }

    // ================= DATE PICKER =================
    private void openDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    alarmCalendar.set(Calendar.YEAR, year);
                    alarmCalendar.set(Calendar.MONTH, month);
                    alarmCalendar.set(Calendar.DAY_OF_MONTH, day);
                    btnDate.setText(day + "/" + (month + 1) + "/" + year);
                },
                alarmCalendar.get(Calendar.YEAR),
                alarmCalendar.get(Calendar.MONTH),
                alarmCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ================= TIME PICKER =================
    private void openTimePicker() {
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    alarmCalendar.set(Calendar.HOUR_OF_DAY, hour);
                    alarmCalendar.set(Calendar.MINUTE, minute);
                    alarmCalendar.set(Calendar.SECOND, 0);

                    SimpleDateFormat sdf =
                            new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    btnTime.setText(sdf.format(alarmCalendar.getTime()));
                },
                alarmCalendar.get(Calendar.HOUR_OF_DAY),
                alarmCalendar.get(Calendar.MINUTE),
                false
        ).show();
    }
}
