package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.micycle.adapters.UserMyCycleAdapter;
import com.example.micycle.models.Cycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;
import android.util.Log;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class UserMyCyclesActivity extends AppCompatActivity {

    private RecyclerView myCyclesRecyclerView;
    private UserMyCycleAdapter userMyCycleAdapter;
    private List<Cycle> myCycleList;
    private Button addCycleButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_my_cycles);

        myCyclesRecyclerView = findViewById(R.id.myCyclesRecyclerView);
        myCyclesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        myCycleList = new ArrayList<>();
        userMyCycleAdapter = new UserMyCycleAdapter(this, myCycleList);
        myCyclesRecyclerView.setAdapter(userMyCycleAdapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userMyCycleAdapter.setOnItemClickListener(new UserMyCycleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Cycle cycle) {
                Toast.makeText(UserMyCyclesActivity.this, "View Details for: " + cycle.getModel(), Toast.LENGTH_SHORT).show();
                Intent detailIntent = new Intent(UserMyCyclesActivity.this, CycleDetailsActivity.class);
                detailIntent.putExtra("cycle_id", cycle.getCycleId());
                startActivity(detailIntent);
            }
        });

        userMyCycleAdapter.setOnEditButtonClickListener(new UserMyCycleAdapter.OnEditButtonClickListener() {
            @Override
            public void onEditButtonClick(Cycle cycle) {
                Toast.makeText(UserMyCyclesActivity.this, "Edit clicked for: " + cycle.getModel(), Toast.LENGTH_SHORT).show();
                Intent editIntent = new Intent(UserMyCyclesActivity.this, AddEditCycleActivity.class);
                editIntent.putExtra("cycle_id", cycle.getCycleId());
                startActivity(editIntent);
            }
        });

        userMyCycleAdapter.setOnRemoveButtonClickListener(new UserMyCycleAdapter.OnRemoveButtonClickListener() {
            @Override
            public void onRemoveButtonClick(Cycle cycle) {
                // Show a confirmation dialog before removing
                new AlertDialog.Builder(UserMyCyclesActivity.this)
                        .setTitle("Remove Cycle")
                        .setMessage("Are you sure you want to remove this cycle?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            // User confirmed deletion, call the delete method
                            deleteCycle(cycle.getCycleId());
                        })
                        .setNegativeButton("Cancel", null) // Do nothing on cancel
                        .show();
            }
        });

        addCycleButton = findViewById(R.id.addCycleButton);
        addCycleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(UserMyCyclesActivity.this, "Add New Cycle Clicked!", Toast.LENGTH_SHORT).show();
                Intent addIntent = new Intent(UserMyCyclesActivity.this, AddEditCycleActivity.class);
                startActivity(addIntent);
            }
        });

        fetchMyCycles();

        // This activity will display cycles owned by the logged-in user.
    }

    private void fetchMyCycles() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();

            db.collection("cycles")
                    .whereEqualTo("ownerId", currentUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Cycle> allFetchedCycles = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try { // Add try-catch for safety during object mapping
                                Cycle cycle = document.toObject(Cycle.class);
                                cycle.setCycleId(document.getId());
                                    allFetchedCycles.add(cycle);

                                    Log.d("UserMyCycles", "Fetched Cycle ID: " + cycle.getCycleId() +
                                            ", Status: " + cycle.getAvailabilityStatus() +
                                            ", Booked Until: " + cycle.getBookedUntilTimestamp());
                                } catch (Exception e) {
                                    Log.e("UserMyCycles", "Error mapping cycle document: " + document.getId(), e);
                                    // Continue to the next document if mapping fails for one
                                }
                            }

                            myCycleList.clear();
                            Log.d("UserMyCycles", "myCycleList cleared. Current size: " + myCycleList.size());
                            Date now = new Date(); // Get current timestamp

                            // Iterate through fetched cycles to check and update booked status if expired
                            for (Cycle cycle : allFetchedCycles) {
                                try { // Add try-catch for safety during processing and adding
                                    boolean needsStatusUpdate = false;
                                    if ("booked".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
                                        Date bookedUntil = cycle.getBookedUntilTimestamp();
                                        // Check if bookedUntilTimestamp is in the past
                                        if (bookedUntil != null && bookedUntil.before(now)) {
                                            needsStatusUpdate = true;
                                        }
                                    }

                                    if (needsStatusUpdate) {
                                        Log.d("UserMyCycles", "Attempting to update cycle status for ID: " + cycle.getCycleId() + " from booked to available.");

                                        // Update status to "available" and clear bookedUntilTimestamp in Firestore
                                        db.collection("cycles").document(cycle.getCycleId())
                                                .update("availabilityStatus", "available",
                                                        "bookedUntilTimestamp", null) // Set timestamp to null
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("UserMyCycles", "SUCCESS: Cycle status updated to available for ID: " + cycle.getCycleId());
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("UserMyCycles", "FAILURE: Error updating cycle status for ID: " + cycle.getCycleId(), e);
                                                    Toast.makeText(UserMyCyclesActivity.this, "Failed to auto-update cycle status.", Toast.LENGTH_SHORT).show();
                                                });

                                        // Update the local cycle object for immediate display
                                        cycle.setAvailabilityStatus("available");
                                        cycle.setBookedUntilTimestamp(null);
                                    }
                                    // Add the cycle to the list regardless of status, as this is "My Cycles"
                                myCycleList.add(cycle);
                                    Log.d("UserMyCycles", "Added cycle ID " + cycle.getCycleId() + " to myCycleList. Current size: " + myCycleList.size());

                                } catch (Exception e) {
                                     Log.e("UserMyCycles", "Error processing or adding cycle to list: " + cycle.getCycleId(), e);
                                     // Continue to the next cycle if processing fails for one
                                }
                            }

                            // Moved notifyDataSetChanged() to after the loop that processes and updates cycles
                            userMyCycleAdapter.notifyDataSetChanged();
                            Log.d("UserMyCycles", "My cycles fetch and update process completed. Final list size: " + myCycleList.size());

                        } else {
                            Log.w("UserMyCycles", "Error getting documents: ", task.getException());
                            Toast.makeText(UserMyCyclesActivity.this, "Error loading your cycles: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            Log.w("UserMyCycles", "User not authenticated. Cannot fetch my cycles.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMyCycles();
    }

    private void deleteCycle(String cycleId) {
        db.collection("cycles").document(cycleId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserMyCyclesActivity.this, "Cycle removed successfully!", Toast.LENGTH_SHORT).show();
                    fetchMyCycles();
                    Log.d("UserMyCycles", "Cycle successfully deleted: " + cycleId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserMyCyclesActivity.this, "Error removing cycle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.w("UserMyCycles", "Error deleting cycle", e);
                });
    }
}
