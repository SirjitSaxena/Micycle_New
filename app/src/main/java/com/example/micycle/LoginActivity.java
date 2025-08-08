package com.example.micycle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import com.example.micycle.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailUsernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailUsernameEditText = findViewById(R.id.emailUsernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpTextView = findViewById(R.id.signUpTextView);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailUsername = emailUsernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (emailUsername.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(emailUsername, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    if (user != null) {
                                        db.collection("users")
                                                .document(user.getUid())
                                                .get()
                                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> dbTask) {
                                                        if (dbTask.isSuccessful()) {
                                                            DocumentSnapshot document = dbTask.getResult();
                                                            if (document.exists()) {
                                                                User loggedInUser = document.toObject(User.class);
                                                                if (loggedInUser != null) {
                                                                    // Check if the user is blocked
                                                                    if (loggedInUser.getBlocked()) {
                                                                        Log.d(TAG, "User is blocked: " + loggedInUser.getEmail());
                                                                        Toast.makeText(LoginActivity.this, "Your account has been blocked.", Toast.LENGTH_LONG).show();
                                                                        mAuth.signOut();
                                                                    } else {
                                                                        String userType = loggedInUser.getUserType();
                                                                        if (userType != null) {
                                                                            if ("regular".equalsIgnoreCase(userType)) {
                                                                                Toast.makeText(LoginActivity.this, "User Login Successful!", Toast.LENGTH_SHORT).show();
                                                                                Intent userIntent = new Intent(LoginActivity.this, UserMainActivity.class);
                                                                                userIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                startActivity(userIntent);
                                                                                finish();
                                                                            } else if ("admin".equalsIgnoreCase(userType)) {
                                                                                Toast.makeText(LoginActivity.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                                                                                Intent adminIntent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                                                                adminIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                startActivity(adminIntent);
                                                                                finish();
                                                                            } else {
                                                                                Toast.makeText(LoginActivity.this, "Unknown user type", Toast.LENGTH_SHORT).show();
                                                                                mAuth.signOut();
                                                                            }
                                                                        } else {
                                                                            Log.w(TAG, "User type not found in database");
                                                                            Toast.makeText(LoginActivity.this, "User type not found in database", Toast.LENGTH_SHORT).show();
                                                                            mAuth.signOut();
                                                                        }
                                                                    }
                                                                } else {
                                                                    Log.w(TAG, "Failed to convert user document to User object.");
                                                                    Toast.makeText(LoginActivity.this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                                                                    mAuth.signOut();
                                                                }
                                                            } else {
                                                                Log.w(TAG, "User document does not exist for UID: " + user.getUid());
                                                                Toast.makeText(LoginActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                                                                mAuth.signOut();
                                                            }
                                                        } else {
                                                            Log.w(TAG, "Error getting user document.", dbTask.getException());
                                                            Toast.makeText(LoginActivity.this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
                                                            mAuth.signOut();
                                                        }
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Authentication success, but user is null.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        User loggedInUser = task.getResult().toObject(User.class);
                        if (loggedInUser != null) {
                            // Check if the user is blocked
                            if (loggedInUser.getBlocked()) {
                                Log.d(TAG, "Already signed-in user is blocked: " + loggedInUser.getEmail());
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this, "Your account has been blocked.", Toast.LENGTH_LONG).show();
                            } else {
                                String userType = loggedInUser.getUserType();
                                Log.d(TAG, "User already signed in. Type: " + userType);
                                if ("admin".equalsIgnoreCase(userType)) {
                                    Intent adminIntent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                    adminIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(adminIntent);
                                } else {
                                    Intent userIntent = new Intent(LoginActivity.this, UserMainActivity.class);
                                    userIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(userIntent);
                                }
                                finish();
                            }
                        }
                    } else {
                        Log.w(TAG, "Error fetching user data for already signed-in user or document missing.", task.getException());
                    }
                }
            });
        }
    }
}
