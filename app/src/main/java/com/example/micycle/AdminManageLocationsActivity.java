package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.content.Intent;
import android.content.DialogInterface;

import com.example.micycle.adapters.LocationAdapter;
import com.example.micycle.models.Location;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class AdminManageLocationsActivity extends AppCompatActivity implements LocationAdapter.OnItemClickListener {

    private RecyclerView locationsRecyclerView;
    private Button addLocationButton;
    private LocationAdapter locationAdapter;
    private List<Location> locationList;

    private FirebaseFirestore db;

    // Define a request code for the edit activity
    private static final int EDIT_LOCATION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_locations);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get references to the UI elements
        locationsRecyclerView = findViewById(R.id.locationsRecyclerView);
        addLocationButton = findViewById(R.id.addLocationButton);

        // Set up the RecyclerView
        locationList = new ArrayList<>();
        locationAdapter = new LocationAdapter(locationList);
        locationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationsRecyclerView.setAdapter(locationAdapter);
        locationAdapter.setOnItemClickListener(this);

        // Set click listener for Add Location button
        addLocationButton.setOnClickListener(v -> {
            // Create an Intent to start AddEditLocationActivity for adding a new location
            Intent intent = new Intent(AdminManageLocationsActivity.this, AddEditLocationActivity.class);
            startActivity(intent); // No need for startActivityForResult for adding
        });
    }

    // Handle the result from AddEditLocationActivity (specifically for editing)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the result is from the edit location request and was successful
        if (requestCode == EDIT_LOCATION_REQUEST && resultCode == RESULT_OK) {
            // If a location was successfully edited, refresh the list
            fetchLocations();
            Log.d("AdminLocations", "Refreshed locations after editing.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fetch locations from Firestore whenever the activity resumes
        fetchLocations();
    }

    // Method to fetch locations from Firestore
    private void fetchLocations() {
        db.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        locationList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Location location = document.toObject(Location.class);
                            location.setLocationId(document.getId());
                            locationList.add(location);
                        }
                        locationAdapter.notifyDataSetChanged();
                        Log.d("AdminLocations", "Locations fetched: " + locationList.size());
                    } else {
                        Log.w("AdminLocations", "Error getting locations.", task.getException());
                        Toast.makeText(AdminManageLocationsActivity.this, "Error loading locations.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onItemClick(Location location) {
        checkIfLocationIsUsed(location);
    }

    private void checkIfLocationIsUsed(Location location) {
        db.collection("cycles")
                .whereEqualTo("locationId", location.getLocationId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            showEditDeleteDialog(location);
                        } else {
                            Toast.makeText(this, "Cannot edit or delete this location. It is currently used by cycles.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w("AdminLocations", "Error checking for associated cycles.", task.getException());
                        Toast.makeText(this, "Error checking location usage.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditDeleteDialog(Location location) {
        new AlertDialog.Builder(this)
                .setTitle("Manage Location")
                .setMessage("Choose an action for " + location.getName())
                .setPositiveButton("Edit", (dialog, which) -> {
                    // Create an Intent to start AddEditLocationActivity for editing
                    Intent intent = new Intent(AdminManageLocationsActivity.this, AddEditLocationActivity.class);
                    // Pass the location data to the intent
                    intent.putExtra("location_id", location.getLocationId());
                    intent.putExtra("location_name", location.getName());
                    startActivityForResult(intent, EDIT_LOCATION_REQUEST); // Use startActivityForResult for editing
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    // Implement delete location logic
                    deleteLocation(location);
                })
                .setNeutralButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Method to delete a location from Firestore
    private void deleteLocation(Location location) {
        db.collection("locations").document(location.getLocationId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Location deleted successfully!", Toast.LENGTH_SHORT).show();
                    // Refresh the list after deletion
                    fetchLocations();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.w("AdminLocations", "Error deleting location", e);
                });
    }
}
