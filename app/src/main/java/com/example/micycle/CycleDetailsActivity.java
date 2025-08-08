package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.viewpager2.widget.ViewPager2;

import com.example.micycle.models.Cycle;
import com.example.micycle.models.Location; // Import Location model
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;
import com.example.micycle.adapters.CycleImageAdapter;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Date; // Import Date
import java.text.SimpleDateFormat; // Import SimpleDateFormat


public class CycleDetailsActivity extends AppCompatActivity {
    
    private TextView modelTextView;
    private TextView descriptionTextView;
    private TextView priceTextView;
    private TextView locationTextView;
    private TextView availabilityTextView;
    private TextView colorTextView; // Declare color TextView
    private TextView bookedUntilTextView; // Declare booked until TextView
    private Button bookNowButton;
    private Button viewReviewsButton; // Declare the new button
    private ViewPager2 cycleImagesViewPager;
    private TextView ratingTextView; // TextView for average rating
    private TextView reviewCountTextView; // TextView for number of reviews

    private FirebaseFirestore db;
    private String cycleId;
    private Cycle cycle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_details);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get references to the UI element
        modelTextView = findViewById(R.id.cycleModelTextView);
        descriptionTextView = findViewById(R.id.cycleDescriptionTextView);
        priceTextView = findViewById(R.id.cyclePriceTextView);
        locationTextView = findViewById(R.id.cycleLocationTextView);
        availabilityTextView = findViewById(R.id.cycleAvailabilityTextView);
        colorTextView = findViewById(R.id.cycleColorTextView); // Initialize color TextView
        bookedUntilTextView = findViewById(R.id.bookedUntilTextView); // Initialize booked until TextView
        bookNowButton = findViewById(R.id.bookNowButton);
        viewReviewsButton = findViewById(R.id.viewReviewsButton); // Initialize the new button
        cycleImagesViewPager = findViewById(R.id.cycleImagesViewPager);
        ratingTextView = findViewById(R.id.cycleDetailsRatingTextView); // Initialize rating TextView
        reviewCountTextView = findViewById(R.id.cycleDetailsReviewCountTextView); // Initialize review count TextView

        // Get the cycle ID from the Intent
        Intent intent = getIntent();
        if (intent.hasExtra("cycle_id")) {
            cycleId = intent.getStringExtra("cycle_id");
            if (cycleId != null) {
                fetchCycleDetails(cycleId); // Fetch cycle details using the ID
            } else {
                Toast.makeText(this, "Cycle ID is missing.", Toast.LENGTH_SHORT).show();
                finish(); // Close activity if no ID is passed
            }
        } else {
            Toast.makeText(this, "Cycle ID is missing.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no ID is passed
        }

        // Set click listener for the Book Now button
        bookNowButton.setOnClickListener(v -> {
            // Navigate to BookingActivity
            Intent bookingIntent = new Intent(CycleDetailsActivity.this, BookingActivity.class);
            // Pass relevant cycle data to BookingActivity
            bookingIntent.putExtra("cycle_id", cycleId);
            startActivity(bookingIntent);
        });

        // Set click listener for the View Reviews button
        viewReviewsButton.setOnClickListener(v -> {
            if (cycleId != null) {
                Intent reviewsIntent = new Intent(CycleDetailsActivity.this, ReviewsListActivity.class);
                reviewsIntent.putExtra("cycle_id", cycleId);
                startActivity(reviewsIntent);
            } else {
                Toast.makeText(CycleDetailsActivity.this, "Cycle ID is not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to fetch cycle details from Firestore
    private void fetchCycleDetails(String cycleId) {
        db.collection("cycles").document(cycleId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cycle = documentSnapshot.toObject(Cycle.class);
                        if (cycle != null) {

                            modelTextView.setText(cycle.getModel());
                            colorTextView.setText(cycle.getColor()); // Set the color
                            descriptionTextView.setText(cycle.getDescription());
                            // Retrieve price directly from documentSnapshot and check for null
                            Double pricePerHour = documentSnapshot.getDouble("pricePerHour");
                            if (pricePerHour != null) {
                                priceTextView.setText("₹" + String.format(Locale.US, "%.2f", pricePerHour) + "/hour");
                            } else {
                                priceTextView.setText("Price: N/A"); // Handle null price
                            }

                            // Display Rating and Review Count
                            if (cycle.getNumberOfReviews() > 0) {
                                ratingTextView.setText(String.format(Locale.US, "Average Rating: %.1f ⭐", cycle.getAverageRating()));
                                reviewCountTextView.setText(String.format(Locale.US, "(%d reviews)", cycle.getNumberOfReviews()));
                                ratingTextView.setVisibility(View.VISIBLE);
                                reviewCountTextView.setVisibility(View.VISIBLE);
                                viewReviewsButton.setVisibility(View.VISIBLE); // Show the View Reviews button
                            } else {
                                ratingTextView.setVisibility(View.GONE);
                                reviewCountTextView.setVisibility(View.GONE);
                                viewReviewsButton.setVisibility(View.GONE); // Hide the View Reviews button if no reviews
                            }

                            // Fetch and display location name
                            String locationId = cycle.getLocationId();
                            if (locationId != null && !locationId.isEmpty()) {
                                fetchLocationName(locationId);
                            } else {
                                locationTextView.setText("N/A"); // Set text directly without "Location:" prefix
                            }

                            availabilityTextView.setText(cycle.getAvailabilityStatus());

                            // Show booked until if the cycle is booked
                            if ("booked".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
                                Date bookedUntil = cycle.getBookedUntilTimestamp();
                                if (bookedUntil != null) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                    bookedUntilTextView.setText("Booked Until: " + sdf.format(bookedUntil));
                                    bookedUntilTextView.setVisibility(View.VISIBLE); // Make it visible
                                } else {
                                    bookedUntilTextView.setVisibility(View.GONE); // Hide if booked but no timestamp
                                }
                                bookNowButton.setEnabled(false); // Disable booking if booked
                                bookNowButton.setText("Not Available");

                            } else {
                                bookedUntilTextView.setVisibility(View.GONE); // Hide if not booked
                                bookNowButton.setEnabled(true); // Enable booking if available
                                bookNowButton.setText("Book Now");
                            }

                            // Check if image URLs list is available and not empty
                            if (cycle.getImageUrls() != null && !cycle.getImageUrls().isEmpty()) {
                                // Create and set the adapter for ViewPager2
                                CycleImageAdapter adapter = new CycleImageAdapter(cycle.getImageUrls());
                                cycleImagesViewPager.setAdapter(adapter);
                                cycleImagesViewPager.setVisibility(View.VISIBLE); // Make ViewPager visible
                            } else {
                                // Handle case where there are no images (e.g., display a placeholder)
                                // For now, the ViewPager2 will just be empty
                                cycleImagesViewPager.setVisibility(View.GONE); // Hide ViewPager if no images
                            }

                            Log.d("CycleDetails", "Cycle details fetched successfully: " + cycleId);
                        } else {
                            Toast.makeText(CycleDetailsActivity.this, "Error: Cycle data is null.", Toast.LENGTH_SHORT).show();
                            Log.e("CycleDetails", "Fetched document but toObject returned null.");
                            finish(); // Close activity on error
                        }
                    } else {
                        // Document doesn\'t exist
                        Toast.makeText(CycleDetailsActivity.this, "Error: Cycle not found.", Toast.LENGTH_SHORT).show();
                        Log.w("CycleDetails", "Cycle with ID " + cycleId + " not found.");
                        finish(); // Close if document not found
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching document
                    Toast.makeText(CycleDetailsActivity.this, "Error loading cycle details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.w("CycleDetails", "Error fetching document " + cycleId, e);
                    finish(); // Close on error
                });
    }

    // Method to fetch location name from Firestore
    private void fetchLocationName(String locationId) {
        db.collection("locations").document(locationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Location location = documentSnapshot.toObject(Location.class);
                        if (location != null && location.getName() != null) {
                            locationTextView.setText(location.getName()); // Set location name directly
                        } else {
                            locationTextView.setText("Unknown"); // Set text directly
                            Log.w("CycleDetails", "Location data is null or name is missing for ID: " + locationId);
                        }
                    } else {
                        locationTextView.setText("Not found"); // Set text directly
                        Log.w("CycleDetails", "Location with ID " + locationId + " not found.\n");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CycleDetailsActivity.this, "Error loading location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.w("CycleDetails", "Error fetching location " + locationId, e);
                    locationTextView.setText("Error");
                });
    }
}