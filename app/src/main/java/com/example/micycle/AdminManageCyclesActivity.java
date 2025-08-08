package com.example.micycle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.micycle.adapters.AdminCycleAdapter;
import com.example.micycle.models.Cycle;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
import android.util.Log;
import android.view.View; // Import View class

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.micycle.models.Message;

public class AdminManageCyclesActivity extends AppCompatActivity implements AdminCycleAdapter.OnAdminCycleActionListener {

    private static final String TAG = "AdminManageCycles";

    private RecyclerView adminCyclesRecyclerView;
    private AdminCycleAdapter adminCycleAdapter;
    private List<Cycle> cycleList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_cycles);
        // Placeholder content for Admin Manage Cycles

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        adminCyclesRecyclerView = findViewById(R.id.adminCyclesRecyclerView);
        adminCyclesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        cycleList = new ArrayList<>();
        adminCycleAdapter = new AdminCycleAdapter(this, cycleList, this);
        adminCyclesRecyclerView.setAdapter(adminCycleAdapter);

        fetchCycles();
    }

    private void fetchCycles() {
        Log.d(TAG, "Fetching all cycles for admin.");
        db.collection("cycles")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cycleList.clear();
                        Date now = new Date();
                        List<Cycle> allFetchedCycles = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Cycle cycle = document.toObject(Cycle.class);
                            cycle.setCycleId(document.getId());
                            allFetchedCycles.add(cycle);
                        }

                        Log.d(TAG, "Successfully fetched " + allFetchedCycles.size() + " cycles.");

                        for (Cycle cycle : allFetchedCycles) {
                            boolean needsStatusUpdate = false;
                            Log.d(TAG, "Processing Cycle ID: " + cycle.getCycleId() + ", Status: " + cycle.getAvailabilityStatus() + ", BookedUntil: " + cycle.getBookedUntilTimestamp());

                            if ("booked".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
                                Date bookedUntil = cycle.getBookedUntilTimestamp();
                                if (bookedUntil != null && bookedUntil.before(now)) {
                                    needsStatusUpdate = true;
                                    Log.d(TAG, "Cycle " + cycle.getCycleId() + " is expired. Marking for update.");
                                }
                            }

                            if (needsStatusUpdate) {
                                db.collection("cycles").document(cycle.getCycleId())
                                        .update("availabilityStatus", "available",
                                                "bookedUntilTimestamp", null)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "SUCCESS: Cycle " + cycle.getCycleId() + " status updated to available in Firestore.");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "FAILURE: Error updating cycle status for " + cycle.getCycleId() + " in Firestore.", e);
                                            Toast.makeText(AdminManageCyclesActivity.this, "Failed to auto-update cycle status for " + cycle.getModel(), Toast.LENGTH_SHORT).show();
                                        });

                                cycle.setAvailabilityStatus("available");
                                cycle.setBookedUntilTimestamp(null);
                                Log.d(TAG, "Cycle " + cycle.getCycleId() + " local status updated to available.");
                            }

                            cycleList.add(cycle);
                        }

                        adminCycleAdapter.setCycleList(cycleList);
                        Log.d(TAG, "Admin cycles list updated with " + cycleList.size() + " cycles.");

                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(AdminManageCyclesActivity.this, "Error fetching cycles: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    
    public void onEditClick(Cycle cycle, int position) {
        Date now = new Date();
        if ("booked".equalsIgnoreCase(cycle.getAvailabilityStatus()) && cycle.getBookedUntilTimestamp() != null && cycle.getBookedUntilTimestamp().after(now)) {
             Toast.makeText(this, "Cannot edit a cycle that is currently booked.", Toast.LENGTH_SHORT).show();
             Log.d(TAG, "Attempted to edit booked cycle: " + cycle.getCycleId());
        } else if ("blocked".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
             Toast.makeText(this, "Cannot edit a cycle that is currently blocked.", Toast.LENGTH_SHORT).show();
             Log.d(TAG, "Attempted to edit blocked cycle: " + cycle.getCycleId());
        } else {
        Intent intent = new Intent(AdminManageCyclesActivity.this, AddEditCycleActivity.class);
        intent.putExtra("cycle_id", cycle.getCycleId());
        startActivity(intent);
            Log.d(TAG, "Navigating to AddEditCycleActivity to edit cycle: " + cycle.getCycleId());
        }
    }

    @Override
    public void onBlockClick(Cycle cycle, int position) {
        String currentStatus = cycle.getAvailabilityStatus();
        String cycleId = cycle.getCycleId();
        String cycleModel = cycle.getModel();
        String ownerId = cycle.getOwnerId();

        String newStatus;
        String actionType;
        String dialogTitle;

        if ("blocked".equalsIgnoreCase(currentStatus)) {
            newStatus = "available";
            actionType = "cycle_unblocked";
            dialogTitle = "Unblock Cycle: " + cycleModel;
        } else if ("available".equalsIgnoreCase(currentStatus)) {
            newStatus = "blocked";
            actionType = "cycle_blocked";
            dialogTitle = "Block Cycle: " + cycleModel;
        } else {
            Toast.makeText(this, "Cannot block/unblock a cycle that is currently booked.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Attempted to block/unblock booked cycle: " + cycleId);
            return;
        }

        showAdminMessageDialog(actionType, ownerId, cycleId, cycleModel, dialogTitle, (adminMessage) -> {
            if (cycleId != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("availabilityStatus", newStatus);
                if ("blocked".equalsIgnoreCase(newStatus)) {
                    updates.put("bookedUntilTimestamp", null);
                }

                db.collection("cycles").document(cycleId)
                        .update(updates)
                            .addOnSuccessListener(aVoid -> {
                            cycle.setAvailabilityStatus(newStatus);
                            if ("blocked".equalsIgnoreCase(newStatus)) {
                                cycle.setBookedUntilTimestamp(null);
                            }
                            adminCycleAdapter.notifyItemChanged(position);

                            String statusMessage = newStatus.toLowerCase();
                            Toast.makeText(AdminManageCyclesActivity.this, cycleModel + " has been " + statusMessage, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Cycle " + cycleModel + " (ID: " + cycleId + ") has been " + statusMessage);

                            })
                            .addOnFailureListener(e -> {
                            String action = newStatus.toLowerCase();
                            Log.w(TAG, "Error " + action + " cycle with ID: " + cycleId, e);
                            Toast.makeText(AdminManageCyclesActivity.this, "Error " + action + " cycle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
            } else {
                Log.w(TAG, "Attempted to " + newStatus.toLowerCase() + " cycle with null cycleId.");
                Toast.makeText(this, "Error: Cycle ID is missing.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCycles();
    }

    private void showAdminMessageDialog(String actionType, String userId, String relatedItemId, String relatedItemName, String dialogTitle, java.util.function.Consumer<String> onMessageSubmitted) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(dialogTitle);

        View view = getLayoutInflater().inflate(R.layout.dialog_admin_message, null);
        EditText messageEditText = view.findViewById(R.id.editTextAdminMessage);
        builder.setView(view);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String adminMessage = messageEditText.getText().toString().trim();
            if (adminMessage.isEmpty()) {
                Toast.makeText(AdminManageCyclesActivity.this, "Reason cannot be empty.", Toast.LENGTH_SHORT).show();
            } else {
                createAdminMessage(userId, actionType, relatedItemId, relatedItemName, adminMessage);
                if (onMessageSubmitted != null) {
                    onMessageSubmitted.accept(adminMessage);
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createAdminMessage(String userId, String actionType, String relatedItemId, String relatedItemName, String messageContent) {
        FirebaseUser adminUser = mAuth.getCurrentUser();
        if (adminUser == null) {
            Log.w(TAG, "Admin user not logged in, cannot create message.");
            return;
        }

        String adminId = adminUser.getUid();
        Date timestamp = new Date();

        Message newMessage = new Message(userId, adminId, actionType, relatedItemId, relatedItemName, messageContent, timestamp);

        db.collection("messages")
                .add(newMessage)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Admin message added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding admin message", e));
    }
}
