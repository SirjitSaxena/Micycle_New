package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.micycle.adapters.AdminTransactionAdapter;
import com.example.micycle.models.Booking;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.widget.Toast;
import android.util.Log;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Calendar;

import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;

public class AdminManageTransactionsActivity extends AppCompatActivity implements AdminTransactionAdapter.OnItemClickListener {

    private static final String TAG = "AdminTransMngAct";

    private RecyclerView adminTransactionsRecyclerView;
    private AdminTransactionAdapter adminTransactionAdapter;
    private List<Booking> bookingList;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_transactions);

        db = FirebaseFirestore.getInstance();

        adminTransactionsRecyclerView = findViewById(R.id.adminTransactionsRecyclerView);
        adminTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        bookingList = new ArrayList<>();
        adminTransactionAdapter = new AdminTransactionAdapter(bookingList);
        adminTransactionAdapter.setOnItemClickListener(this);
        adminTransactionsRecyclerView.setAdapter(adminTransactionAdapter);

        fetchTransactions();
    }

    private void fetchTransactions() {
        db.collection("bookings")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookingList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Booking booking = document.toObject(Booking.class);
                            booking.setBookingId(document.getId());
                        bookingList.add(booking);
                    }
                    adminTransactionAdapter.setTransactionList(bookingList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching transactions.", e);
                    Toast.makeText(this, "Error loading transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(String bookingId, String cycleName, String renterName, String ownerName, Date startTime, Date endTime, double totalCost, String bookingStatus) {
        Intent intent = new Intent(this, AdminTransactionDetailsActivity.class);
        intent.putExtra("booking_id", bookingId);
        intent.putExtra("cycle_name", cycleName);
        intent.putExtra("renter_name", renterName);
        intent.putExtra("owner_name", ownerName);
        intent.putExtra("start_time", startTime != null ? startTime.getTime() : -1);
        intent.putExtra("end_time", endTime != null ? endTime.getTime() : -1);
        intent.putExtra("total_cost", totalCost);
        intent.putExtra("booking_status", bookingStatus);
        startActivity(intent);
    }
}
