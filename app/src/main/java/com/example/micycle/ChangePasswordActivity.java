package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import android.os.CountDownTimer;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Properties;
import java.util.Random;

import sendinblue.ApiClient;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";

    private EditText editTextNewPassword;
    private EditText editTextConfirmNewPassword;
    private Button buttonChangePassword;
    private EditText otpEditText;
    private Button sendOtpButton;
    private Button verifyOtpButton;
    private Button resendOtpButton;
    private TextView timerTextView;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private CountDownTimer countDownTimer;
    private String generatedOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmNewPassword = findViewById(R.id.editTextConfirmNewPassword);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        otpEditText = findViewById(R.id.otpEditText);
        sendOtpButton = findViewById(R.id.sendOtpButton);
        verifyOtpButton = findViewById(R.id.verifyOtpButton);
        resendOtpButton = findViewById(R.id.resendOtpButton);
        timerTextView = findViewById(R.id.timerTextView);

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if user is not logged in
            return;
        }

        sendOtpButton.setOnClickListener(v -> sendOtp());
        resendOtpButton.setOnClickListener(v -> sendOtp());
        verifyOtpButton.setOnClickListener(v -> verifyOtp());
        buttonChangePassword.setOnClickListener(v -> changePassword());
    }

    private void sendOtp() {
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmNewPassword = editTextConfirmNewPassword.getText().toString().trim();

        if (newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in new password and confirm it.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters long.", Toast.LENGTH_SHORT).show();
            return;
        }

        generatedOtp = String.valueOf(new Random().nextInt(999999 - 100000 + 1) + 100000);

        new Thread(() -> {
            ApiClient defaultClient = new ApiClient();
            ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
            apiKeyAuth.setApiKey("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");

            TransactionalEmailsApi apiInstance = new TransactionalEmailsApi(defaultClient);
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail("saxenasirjit14112001@gmail.com");
            sender.setName("MiCycle");
            SendSmtpEmailTo to = new SendSmtpEmailTo();
            to.setEmail(currentUser.getEmail());
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSender(sender);
            sendSmtpEmail.setTo(java.util.Collections.singletonList(to));
            sendSmtpEmail.setHtmlContent("<html><body><h1>Your OTP for changing your password is " + generatedOtp + "</h1></body></html>");
            sendSmtpEmail.setSubject("Your MiCycle Change Password OTP");

            try {
                apiInstance.sendTransacEmail(sendSmtpEmail);
                runOnUiThread(() -> {
                    Toast.makeText(ChangePasswordActivity.this, "OTP sent to your email.", Toast.LENGTH_SHORT).show();
                    otpEditText.setVisibility(View.VISIBLE);
                    verifyOtpButton.setVisibility(View.VISIBLE);
                    sendOtpButton.setVisibility(View.GONE);
                    timerTextView.setVisibility(View.VISIBLE);
                    resendOtpButton.setVisibility(View.GONE);
                    startTimer();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error sending email", e);
                runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "Failed to send OTP.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void verifyOtp() {
        String enteredOtp = otpEditText.getText().toString().trim();
        if (enteredOtp.equals(generatedOtp)) {
            Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show();
            verifyOtpButton.setVisibility(View.GONE);
            otpEditText.setVisibility(View.GONE);
            buttonChangePassword.setVisibility(View.VISIBLE);
            timerTextView.setVisibility(View.GONE);
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
        } else {
            Toast.makeText(this, "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("Resend OTP in " + millisUntilFinished / 1000 + "s");
            }

            public void onFinish() {
                timerTextView.setVisibility(View.GONE);
                resendOtpButton.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void changePassword() {
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmNewPassword = editTextConfirmNewPassword.getText().toString().trim();

        if (newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters long.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User password updated.");
                        Toast.makeText(ChangePasswordActivity.this, "Password changed successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.e(TAG, "Error updating password.", task.getException());
                        Toast.makeText(ChangePasswordActivity.this, "Failed to change password: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
