package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button manageLocationsButton;
    private Button manageCyclesButton;
    private Button manageTransactionsButton;
    private Button manageUsersButton;
    private Button viewReportsButton;
    private Button adminLogoutButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();

        manageLocationsButton = findViewById(R.id.manageLocationsButton);
        manageCyclesButton = findViewById(R.id.manageCyclesButton);
        manageTransactionsButton = findViewById(R.id.manageTransactionsButton);
        manageUsersButton = findViewById(R.id.manageUsersButton);
        viewReportsButton = findViewById(R.id.viewReportsButton);
        adminLogoutButton = findViewById(R.id.adminLogoutButton);

        manageLocationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminManageLocationsActivity.class);
            startActivity(intent);
        });

        manageCyclesButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminManageCyclesActivity.class);
            startActivity(intent);
        });

        manageTransactionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminManageTransactionsActivity.class);
            startActivity(intent);
        });

        manageUsersButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminManageUsersActivity.class);
            startActivity(intent);
        });

        viewReportsButton.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminReportsActivity.class));
        });

        adminLogoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // This activity will serve as the admin dashboard.
    }
}
