package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.micycle.adapters.CycleAdapter;
import com.example.micycle.models.Cycle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;
import android.util.Log;
import com.example.micycle.models.Booking;
import com.google.firebase.firestore.FieldValue;
import java.util.Map;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.google.firebase.firestore.Query.Direction;
import java.util.Collections;
import java.util.Comparator;
import com.example.micycle.models.Location;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserMainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private RecyclerView cyclesRecyclerView;
    private CycleAdapter cycleAdapter;
    private List<Cycle> cycleList;
    private Button transactionHistoryButton;
    private Button profileButton;
    private Button myCyclesButton;
    private Spinner sortSpinner;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String currentSortOption = "None";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        cyclesRecyclerView = findViewById(R.id.cyclesRecyclerView);
        cyclesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        cycleList = new ArrayList<>();
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        cycleAdapter = new CycleAdapter(this, cycleList, currentUserId);
        cyclesRecyclerView.setAdapter(cycleAdapter);

        sortSpinner = findViewById(R.id.sortSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);
        sortSpinner.setOnItemSelectedListener(this);

        checkAndCompleteExpiredBookings();

        cycleAdapter.setOnItemClickListener(new CycleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Cycle cycle) {
                Intent detailIntent = new Intent(UserMainActivity.this, CycleDetailsActivity.class);
                detailIntent.putExtra("cycle_id", cycle.getCycleId());
                startActivity(detailIntent);
            }
        });

        transactionHistoryButton = findViewById(R.id.transactionHistoryButton);
        profileButton = findViewById(R.id.profileButton);
        myCyclesButton = findViewById(R.id.myCyclesButton);

        transactionHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent transactionHistoryIntent = new Intent(UserMainActivity.this, TransactionHistoryActivity.class);
                startActivity(transactionHistoryIntent);
            }
        });

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileIntent = new Intent(UserMainActivity.this, ProfileActivity.class);
                startActivity(profileIntent);
            }
        });

        myCyclesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myCyclesIntent = new Intent(UserMainActivity.this, UserMyCyclesActivity.class);
                startActivity(myCyclesIntent);
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        currentSortOption = parent.getItemAtPosition(position).toString();
        fetchAvailableCycles();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    private void fetchAvailableCycles() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();

        db.collection("cycles")
                .whereNotEqualTo("ownerId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cycleList.clear();
                            Date now = new Date();
                            List<Cycle> availableCycles = new ArrayList<>();
                            List<com.google.android.gms.tasks.Task<DocumentSnapshot>> locationTasks = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Cycle cycle = document.toObject(Cycle.class);
                            cycle.setCycleId(document.getId());

                            boolean needsStatusUpdate = false;
                            if ("booked".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
                                Date bookedUntil = cycle.getBookedUntilTimestamp();
                                if (bookedUntil != null && bookedUntil.before(now)) {
                                    needsStatusUpdate = true;
                                }
                            }

                            if (needsStatusUpdate) {
                                db.collection("cycles").document(cycle.getCycleId())
                                        .update("availabilityStatus", "available",
                                                    "bookedUntilTimestamp", null)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("UserMainActivity", "Cycle " + cycle.getCycleId() + " status updated to available.");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w("UserMainActivity", "Error updating cycle status: " + cycle.getCycleId(), e);
                                        });

                                cycle.setAvailabilityStatus("available");
                                cycle.setBookedUntilTimestamp(null);
                            }

                            if ("available".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
                                    availableCycles.add(cycle);

                                    if (cycle.getLocationId() != null && !cycle.getLocationId().isEmpty()) {
                                        com.google.android.gms.tasks.Task<DocumentSnapshot> locationTask = db.collection("locations").document(cycle.getLocationId()).get();
                                        locationTasks.add(locationTask.addOnSuccessListener(locationDocumentSnapshot -> {
                                            if (locationDocumentSnapshot.exists()) {
                                                Location location = locationDocumentSnapshot.toObject(Location.class);
                                                if (location != null && location.getName() != null) {
                                                    cycle.setLocationName(location.getName());
                                                } else {
                                                    cycle.setLocationName("Unknown Location");
                                                }
                                            } else {
                                                cycle.setLocationName("Location Not Found");
                                            }
                                        }).addOnFailureListener(e -> {
                                            Log.w("UserMainActivity", "Error fetching location for cycle " + cycle.getCycleId(), e);
                                            cycle.setLocationName("Error Fetching Location");
                                        }));
                                    } else {
                                        cycle.setLocationName("N/A");
                                    }
                                }
                            }

                            Tasks.whenAllComplete(locationTasks)
                                .addOnCompleteListener(locationFetchTask -> {
                                    cycleList.addAll(availableCycles);

                                    sortCycleList();

                        cycleAdapter.notifyDataSetChanged();
                                    Log.d("UserMainActivity", "Available cycles (client-updated, filtered, and sorted) fetched successfully: " + cycleList.size());
                                });

                    } else {
                        Log.w("UserMainActivity", "Error getting documents: ", task.getException());
                        Toast.makeText(UserMainActivity.this, "Error loading cycles: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            Log.w("UserMainActivity", "User not authenticated. Cannot fetch available cycles.");
            cycleList.clear();
            cycleAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Please log in to view available cycles.", Toast.LENGTH_LONG).show();
        }
    }

    private void sortCycleList() {
        switch (currentSortOption) {
            case "Price (Low to High)":
                Collections.sort(cycleList, Comparator.comparingDouble(Cycle::getPricePerHour));
                break;
            case "Price (High to Low)":
                Collections.sort(cycleList, Comparator.comparingDouble(Cycle::getPricePerHour).reversed());
                break;
            case "Rating (High to Low)":
                Collections.sort(cycleList, Comparator.comparingDouble(Cycle::getAverageRating).reversed()
                        .thenComparingInt(Cycle::getNumberOfReviews));
                break;
            case "Rating (Low to High)":
                Collections.sort(cycleList, Comparator.comparingDouble(Cycle::getAverageRating)
                        .thenComparingInt(Cycle::getNumberOfReviews));
                break;
            case "Location Name (A-Z)":
                Collections.sort(cycleList, Comparator.comparing(Cycle::getLocationName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
                break;
            case "Location Name (Z-A)":
                Collections.sort(cycleList, Comparator.comparing(Cycle::getLocationName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed());
                break;
            case "None":
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndCompleteExpiredBookings();
    }

    private void checkAndCompleteExpiredBookings() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w("UserMainActivity", "User not authenticated. Cannot check for expired bookings.");
            return;
        }

        String currentUserId = currentUser.getUid();
        Date now = new Date();
        Log.d("UserMainActivity", "Checking for expired active bookings for user " + currentUserId + " at: " + now);

        db.collection("bookings")
                .whereEqualTo("bookingStatus", "active")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UserMainActivity", "Successfully fetched active bookings for user " + currentUserId + ". Count: " + task.getResult().size());
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Booking booking = document.toObject(Booking.class);
                                Date endTime = booking.getEndTime();
                                String bookingId = document.getId();

                                Log.d("UserMainActivity", "Processing Booking ID: " + bookingId +
                                        ", Status: " + booking.getBookingStatus() +
                                        ", End Time: " + endTime);

                                if (endTime != null && endTime.before(now)) {
                                    Log.d("UserMainActivity", "Booking " + bookingId + " is expired. Attempting to update status to completed.");

                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("bookingStatus", "completed");
                                    updates.put("updatedAt", FieldValue.serverTimestamp());

                                    db.collection("bookings").document(bookingId)
                                            .update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("UserMainActivity", "SUCCESS: Booking " + bookingId + " marked as completed.");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("UserMainActivity", "FAILURE: Error marking booking " + bookingId + " as completed.", e);
                                                Toast.makeText(UserMainActivity.this, "Failed to auto-update booking status.", Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    if (endTime != null) {
                                         Log.d("UserMainActivity", "Booking " + bookingId + " end time is in the future or now. Not updating status.");
                                    } else {
                                         Log.d("UserMainActivity", "Booking " + bookingId + " has no end time. Not updating status.");
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("UserMainActivity", "Error processing booking document: " + document.getId(), e);
                            }
                        }
                         Log.d("UserMainActivity", "Finished checking for and updating expired bookings.");
                    } else {
                        Log.w("UserMainActivity", "Error fetching active bookings for status check: ", task.getException());
                         Toast.makeText(UserMainActivity.this, "Error loading active bookings.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
