package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class AdminTransactionDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AdminTransDetails";

    private TextView textViewDetailsCycleName;
    private TextView textViewDetailsRenterName;
    private TextView textViewDetailsOwnerName;
    private TextView textViewDetailsStartTime;
    private TextView textViewDetailsEndTime;
    private TextView textViewDetailsTotalCost;
    private TextView textViewDetailsStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_transaction_details);

        textViewDetailsCycleName = findViewById(R.id.textViewDetailsCycleName);
        textViewDetailsRenterName = findViewById(R.id.textViewDetailsRenterName);
        textViewDetailsOwnerName = findViewById(R.id.textViewDetailsOwnerName);
        textViewDetailsStartTime = findViewById(R.id.textViewDetailsStartTime);
        textViewDetailsEndTime = findViewById(R.id.textViewDetailsEndTime);
        textViewDetailsTotalCost = findViewById(R.id.textViewDetailsTotalCost);
        textViewDetailsStatus = findViewById(R.id.textViewDetailsStatus);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String bookingId = extras.getString("booking_id");
            String cycleName = extras.getString("cycle_name");
            String renterName = extras.getString("renter_name");
            String ownerName = extras.getString("owner_name");
            long startTimeMillis = extras.getLong("start_time", -1);
            long endTimeMillis = extras.getLong("end_time", -1);
            double totalCost = extras.getDouble("total_cost", 0.0);
            String bookingStatus = extras.getString("booking_status");

            textViewDetailsCycleName.setText("Cycle: " + (cycleName != null ? cycleName : "N/A"));
            textViewDetailsRenterName.setText("Renter: " + (renterName != null ? renterName : "N/A"));
            textViewDetailsOwnerName.setText("Owner: " + (ownerName != null ? ownerName : "N/A"));
            textViewDetailsStartTime.setText("Start Time: " + (startTimeMillis != -1 ? formatDate(new Date(startTimeMillis)) : "N/A"));
            textViewDetailsEndTime.setText("End Time: " + (endTimeMillis != -1 ? formatDate(new Date(endTimeMillis)) : "N/A"));
            textViewDetailsTotalCost.setText(String.format(Locale.US, "Total Cost: ₹%.2f", totalCost));
            textViewDetailsStatus.setText("Status: " + (bookingStatus != null ? bookingStatus : "N/A"));

        } else {
            Toast.makeText(this, "Error: Transaction details not provided.", Toast.LENGTH_SHORT).show();
                    finish();
        }
    }

    private String formatDate(Date date) {
        if (date == null) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return sdf.format(date);
    }
} 