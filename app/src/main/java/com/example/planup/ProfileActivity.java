package com.example.planup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    ImageView imgProfile;
    TextView tvNickname, tvFullName, tvEmail, tvGender;
    Button btnEditProfile, btnLogout;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔐 Check login
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);

        // 🔹 Bind views
        imgProfile = findViewById(R.id.imgProfile);
        tvNickname = findViewById(R.id.tvNickname);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvGender = findViewById(R.id.tvGender);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        // 🔹 Load user data
        loadUserProfile();

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    // 🔥 Fetch data from Firestore
    private void loadUserProfile() {

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String fullName = document.getString("fullName");
                        String nickname = document.getString("nickname");
                        String email = document.getString("email");
                        String gender = document.getString("gender");

                        tvFullName.setText(fullName);
                        tvNickname.setText(nickname);
                        tvEmail.setText(email);
                        tvGender.setText(gender);

                        // 👤 Gender based avatar
                        if (gender != null && gender.equalsIgnoreCase("female")) {
                            imgProfile.setImageResource(R.drawable.ic_avatar_girl);
                        } else {
                            imgProfile.setImageResource(R.drawable.ic_avatar_boy);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    // 🔴 Logout
    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
