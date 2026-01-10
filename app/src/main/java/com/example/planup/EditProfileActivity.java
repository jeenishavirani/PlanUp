package com.example.planup;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EditProfileActivity extends AppCompatActivity {

    EditText etFullName, etNickname, etEmail;
    Spinner spGender;
    Button btnSave;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        etFullName = findViewById(R.id.etFullName);
        etNickname = findViewById(R.id.etNickname);
        etEmail = findViewById(R.id.etEmail); // read-only
        spGender = findViewById(R.id.spGender);
        btnSave = findViewById(R.id.btnSave);

        // SharedPreferences
        prefs = getSharedPreferences("PlanUpPrefs", MODE_PRIVATE);

        // Spinner adapter
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.gender_options,
                        android.R.layout.simple_spinner_item
                );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);

        // Load existing user data
        loadUserData(adapter);

        // Save button
        btnSave.setOnClickListener(v -> saveUserData());
    }

    private void loadUserData(ArrayAdapter<CharSequence> adapter) {
        etFullName.setText(prefs.getString("user_fullname", ""));
        etNickname.setText(prefs.getString("user_nickname", ""));
        etEmail.setText(prefs.getString("user_email", ""));

        String gender = prefs.getString("user_gender", "");
        if (!gender.isEmpty()) {
            int position = adapter.getPosition(gender);
            if (position >= 0) {
                spGender.setSelection(position);
            }
        }
    }

    private void saveUserData() {
        String fullName = etFullName.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();
        String gender = spGender.getSelectedItem().toString();

        if (fullName.isEmpty() || nickname.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_fullname", fullName);
        editor.putString("user_nickname", nickname);
        editor.putString("user_gender", gender);
        editor.apply();

        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
        finish(); // go back to ProfileActivity
    }
}
