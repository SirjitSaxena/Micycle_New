package com.example.micycle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.example.micycle.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.Properties;
import java.util.Random;

import sendinblue.ApiClient;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;


public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private String generatedOtp;

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText otpEditText;
    private Button signUpButton;
    private Button sendOtpButton;
    private Button verifyOtpButton;
    private Button resendOtpButton;
    private TextView timerTextView;
    private CountDownTimer countDownTimer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        otpEditText = findViewById(R.id.otpEditText);
        signUpButton = findViewById(R.id.signUpButton);
        sendOtpButton = findViewById(R.id.sendOtpButton);
        verifyOtpButton = findViewById(R.id.verifyOtpButton);
        resendOtpButton = findViewById(R.id.resendOtpButton);
        timerTextView = findViewById(R.id.timerTextView);

        resendOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOtp();
            }
        });

        sendOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOtp();
            }
        });

        verifyOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyOtp();
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(SignUpActivity.this, "Password should be at least 6 characters.", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "createUserWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    if (user != null) {
                                        String userId = user.getUid();
                                        String userEmail = user.getEmail();
                                        String userName = name;
                                        String userType = "regular";

                                        User newUser = new User(userId, userEmail, userName, null, userType, new Date());

                                        db.collection("users")
                                                .document(userId)
                                                .set(newUser)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> dbTask) {
                                                        if (dbTask.isSuccessful()) {
                                                            Log.d(TAG, "User data saved to Firestore for user: " + userId);
                                                            Toast.makeText(SignUpActivity.this, "Account created and data saved!", Toast.LENGTH_SHORT).show();

                                                            Intent loginIntent = new Intent(SignUpActivity.this, LoginActivity.class);
                                                            loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(loginIntent);
                                                            finish();

                                                        } else {
                                                            Log.w(TAG, "Error saving user data to Firestore.", dbTask.getException());
                                                            Toast.makeText(SignUpActivity.this, "Account created, but data save failed.", Toast.LENGTH_SHORT).show();

                                                            Intent loginIntent = new Intent(SignUpActivity.this, LoginActivity.class);
                                                            loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            startActivity(loginIntent);
                                                            finish();
                                                        }
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(SignUpActivity.this, "Authentication success, but user is null.", Toast.LENGTH_SHORT).show();
                                    }

                                } else {
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(SignUpActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Additional setup for Sign Up activity will go here
    }

    private void sendOtp() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(SignUpActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            return;
        }

        generatedOtp = String.valueOf(new Random().nextInt(999999 - 100000 + 1) + 100000);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ApiClient defaultClient = new ApiClient();
                // Configure API key authorization: api-key
                ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
                apiKeyAuth.setApiKey("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");

                TransactionalEmailsApi apiInstance = new TransactionalEmailsApi(defaultClient);
                SendSmtpEmailSender sender = new SendSmtpEmailSender();
                sender.setEmail("saxenasirjit14112001@gmail.com");
                sender.setName("MiCycle");
                SendSmtpEmailTo to = new SendSmtpEmailTo();
                to.setEmail(email);
                Properties headers = new Properties();
                headers.setProperty("Some-Custom-Name", "unique-id-1234");
                SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
                sendSmtpEmail.setSender(sender);
                sendSmtpEmail.setTo(java.util.Collections.singletonList(to));
                sendSmtpEmail.setHtmlContent("<html><body><p>Dear user,</p><p>Your One-Time Password (OTP) for MiCycle signup is: <b>" + generatedOtp + "</b></p><p>Please use this OTP to complete your registration. This OTP is valid for a short period.</p><p>If you did not request this, please ignore this email.</p><p>Thank you,<br/>The MiCycle Team</p></body></html>");
                sendSmtpEmail.setSubject("MiCycle: Your Signup OTP");
                sendSmtpEmail.setHeaders(headers);

                try {
                    apiInstance.sendTransacEmail(sendSmtpEmail);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SignUpActivity.this, "OTP sent to your email.", Toast.LENGTH_SHORT).show();
                            otpEditText.setVisibility(View.VISIBLE);
                            verifyOtpButton.setVisibility(View.VISIBLE);
                            sendOtpButton.setVisibility(View.GONE);
                            timerTextView.setVisibility(View.VISIBLE);
                            resendOtpButton.setVisibility(View.GONE);
                            startTimer();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error sending email", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SignUpActivity.this, "Failed to send OTP.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void verifyOtp() {
        String enteredOtp = otpEditText.getText().toString().trim();
        if (enteredOtp.equals(generatedOtp)) {
            Toast.makeText(SignUpActivity.this, "OTP Verified!", Toast.LENGTH_SHORT).show();
            verifyOtpButton.setVisibility(View.GONE);
            otpEditText.setVisibility(View.GONE);
            signUpButton.setVisibility(View.VISIBLE);
            timerTextView.setVisibility(View.GONE);
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
        } else {
            Toast.makeText(SignUpActivity.this, "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("Resend OTP in " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                timerTextView.setVisibility(View.GONE);
                resendOtpButton.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // User is already signed in, potentially redirect to main activity
            // depending on your app's flow after successful sign up/login.
            // For now, we let them stay on the SignUp screen.
            // reload(); // You might want to refresh UI or navigate
        }
    }
}
