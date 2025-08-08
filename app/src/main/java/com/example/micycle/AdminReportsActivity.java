package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;


import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Import Query

public class AdminReportsActivity extends AppCompatActivity {

    private static final String TAG = "AdminReportsActivity";

    private TextView textViewTotalUsers;
    private TextView textViewTotalCycles;
    private TextView textViewTotalBookings;
    private TextView textViewActiveBookings;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

        db = FirebaseFirestore.getInstance();

        textViewTotalUsers = findViewById(R.id.textViewTotalUsers);
        textViewTotalCycles = findViewById(R.id.textViewTotalCycles);
        textViewTotalBookings = findViewById(R.id.textViewTotalBookings);
        textViewActiveBookings = findViewById(R.id.textViewActiveBookings);

        fetchReportsData();
    }

    private void fetchReportsData() {
        // Fetch Total Users Count
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalUsers = queryDocumentSnapshots.size();
                    textViewTotalUsers.setText("Total Users: " + totalUsers);
                    Log.d(TAG, "Fetched total users: " + totalUsers);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching total users.", e);
                    textViewTotalUsers.setText("Total Users: Error");
                     Toast.makeText(this, "Error fetching user count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Fetch Total Cycles Count
        db.collection("cycles").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalCycles = queryDocumentSnapshots.size();
                    textViewTotalCycles.setText("Total Cycles: " + totalCycles);
                     Log.d(TAG, "Fetched total cycles: " + totalCycles);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching total cycles.", e);
                    textViewTotalCycles.setText("Total Cycles: Error");
                    Toast.makeText(this, "Error fetching cycle count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Fetch Total Bookings Count
        db.collection("bookings").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalBookings = queryDocumentSnapshots.size();
                    textViewTotalBookings.setText("Total Bookings: " + totalBookings);
                     Log.d(TAG, "Fetched total bookings: " + totalBookings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching total bookings.", e);
                    textViewTotalBookings.setText("Total Bookings: Error");
                    Toast.makeText(this, "Error fetching total bookings count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Fetch Active Bookings Count (Assuming "active" status indicates active booking)
        db.collection("bookings")
                .whereEqualTo("bookingStatus", "active") // Assuming "active" is the status for current bookings
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeBookings = queryDocumentSnapshots.size();
                    textViewActiveBookings.setText("Active Bookings: " + activeBookings);
                     Log.d(TAG, "Fetched active bookings: " + activeBookings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching active bookings.", e);
                    textViewActiveBookings.setText("Active Bookings: Error");
                    Toast.makeText(this, "Error fetching active bookings count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
