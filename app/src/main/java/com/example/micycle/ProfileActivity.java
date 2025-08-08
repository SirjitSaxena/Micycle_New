package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout; // Import LinearLayout
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity"; // Add a TAG for logging

    private TextView emailTextView;
    private EditText nameEditText, phoneEditText;
    private Button saveProfileButton;
    private Button profileLogoutButton; // Declare logout button
    private Button changePasswordButton; // Button for Change Password
    private Button messagesButton; // Button for Messages

    // Phone Verification UI elements
    private LinearLayout phoneVerificationLayout;
    private TextView verificationInstructionsTextView;
    private Button sendVerificationCodeButton;
    private EditText verificationCodeEditText;
    private Button verifyCodeButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Get references to the main profile UI elements
        nameEditText = findViewById(R.id.nameEditText);
        emailTextView = findViewById(R.id.emailTextView);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        profileLogoutButton = findViewById(R.id.profileLogoutButton); // Find logout button
        changePasswordButton = findViewById(R.id.changePasswordButton); // Initialize Change Password button
        messagesButton = findViewById(R.id.messagesButton); // Initialize Messages button

        // Initialize Phone Verification UI elements
        phoneVerificationLayout = findViewById(R.id.phoneVerificationLayout);
        verificationInstructionsTextView = findViewById(R.id.verificationInstructionsTextView);
        sendVerificationCodeButton = findViewById(R.id.sendVerificationCodeButton);
        verificationCodeEditText = findViewById(R.id.verificationCodeEditText);
        verifyCodeButton = findViewById(R.id.verifyCodeButton);

        // Fetch and display user profile data
        fetchUserProfile();

        // Set OnClickListener for the Save Changes button
        saveProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserProfile();
            }
        });

        // Set OnClickListener for the Logout button
        profileLogoutButton.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out the current user
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish current activity
        });

        // Set OnClickListener for the Change Password button
        changePasswordButton.setOnClickListener(v -> {
            // Implement the logic to navigate to ChangePasswordActivity
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // Set OnClickListener for the Messages button
        messagesButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, UserMessagesActivity.class);
            startActivity(intent);
        });
    }

    private void fetchUserProfile() {
        if (currentUser == null) return;

            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Document exists, retrieve data
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");

                        // Display the data in the EditTexts and TextView
                        nameEditText.setText(name);
                        emailTextView.setText(email);
                        phoneEditText.setText(phoneNumber);

                    Log.d(TAG, "User profile fetched successfully for user: " + userId);

                    } else {
                        // Document doesn't exist for this user
                        Toast.makeText(this, "User profile not found. Please complete your profile.\\nPhone number is optional.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "User document not found for UID: " + userId);
                        // You might want to pre-fill email if available from Firebase Auth
                        emailTextView.setText(currentUser.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching document
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching user document for UID: " + userId, e);
                    // Optionally disable editing fields on error
                    nameEditText.setEnabled(false);
                    phoneEditText.setEnabled(false);
                    saveProfileButton.setEnabled(false);
                });
    }

    private void saveUserProfile() {
        if (currentUser == null) return;

            String name = nameEditText.getText().toString().trim();
            String phoneNumber = phoneEditText.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

        // Important Detail: Phone number must be provided on profile save (as per documentation)
            if (phoneNumber.isEmpty()) {
             Toast.makeText(this, "Phone number is required.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a map of data to update
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("phoneNumber", phoneNumber);

            // Update the user document in Firestore
        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully! Phone number is optional.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "User profile updated successfully for user: " + currentUser.getUid());

                    // After saving, re-fetch profile to check verification status and show UI if needed
                    fetchUserProfile(); // This will now trigger the logic to show verification if needed

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating user document for UID: " + currentUser.getUid(), e);
                });
    }
}