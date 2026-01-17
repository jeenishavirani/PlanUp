package com.example.planup;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    EditText etFullName, etNickname, etEmail;
    Spinner spGender;
    Button btnSave;
    ImageView btnBack;
    LinearLayout header;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etFullName = findViewById(R.id.etFullName);
        etNickname = findViewById(R.id.etNickname);
        etEmail = findViewById(R.id.etEmail); // read-only
        spGender = findViewById(R.id.spGender);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        header = findViewById(R.id.header);

        // Handle system UI insets
        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Top padding for header
            if (header != null) {
                header.setPadding(header.getPaddingLeft(), systemBars.top, header.getPaddingRight(), header.getPaddingBottom());
            }
            
            // Bottom padding for the main container
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        // Spinner adapter
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.gender_options,
                        android.R.layout.simple_spinner_item
                );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);

        // Load existing user data from Firestore
        loadUserData(adapter);

        // Save button
        btnSave.setOnClickListener(v -> saveUserData());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUserData(ArrayAdapter<CharSequence> adapter) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        etFullName.setText(document.getString("fullName"));
                        etNickname.setText(document.getString("nickname"));
                        etEmail.setText(document.getString("email"));

                        String gender = document.getString("gender");
                        if (gender != null && !gender.isEmpty()) {
                            int position = adapter.getPosition(gender);
                            if (position >= 0) {
                                spGender.setSelection(position);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> 
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show());
    }

    private void saveUserData() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        String fullName = etFullName.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();
        String gender = spGender.getSelectedItem().toString();

        if (fullName.isEmpty() || nickname.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("nickname", nickname);
        updates.put("gender", gender);

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish(); // go back to ProfileActivity
                })
                .addOnFailureListener(e -> 
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
