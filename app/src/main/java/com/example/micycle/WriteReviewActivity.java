package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView; // Import TextView
import android.widget.Toast;
import android.util.Log;

import com.example.micycle.models.Review; // Import Review model
import com.example.micycle.models.Cycle; // Import Cycle model
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction; // Import Transaction
import com.google.firebase.firestore.DocumentReference; // Import DocumentReference
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.google.firebase.firestore.FieldValue; // Import FieldValue


import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map


public class WriteReviewActivity extends AppCompatActivity {

    private static final String TAG = "WriteReviewActivity";

    private RatingBar ratingBar;
    private EditText editTextReviewText;
    private Button buttonSubmitReview;
    private TextView textViewReviewCycleName; // TextView for cycle name

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private String bookingId;
    private String cycleId;
    private String reviewId; // To store the review ID when editing
    private boolean isEditing = false; // Flag to indicate if we are editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        ratingBar = findViewById(R.id.ratingBar);
        editTextReviewText = findViewById(R.id.editTextReviewText);
        buttonSubmitReview = findViewById(R.id.buttonSubmitReview);
        textViewReviewCycleName = findViewById(R.id.textViewReviewCycleName);


        // Get data from intent extras
        bookingId = getIntent().getStringExtra("booking_id");
        cycleId = getIntent().getStringExtra("cycle_id");
        reviewId = getIntent().getStringExtra("review_id"); // Get reviewId if passed (for editing)
        isEditing = getIntent().getBooleanExtra("is_editing", false); // Check if we are in editing mode
        String cycleName = getIntent().getStringExtra("cycle_name"); // Get cycle name if passed


        if (currentUser == null || bookingId == null || cycleId == null) {
            Toast.makeText(this, "Error: Authentication or transaction data missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Optional: Display cycle name if available
        if (cycleName != null) {
            textViewReviewCycleName.setText("Reviewing: " + cycleName);
             textViewReviewCycleName.setVisibility(View.VISIBLE);
        } else {
             textViewReviewCycleName.setVisibility(View.GONE);
        }

        // If editing, fetch existing review and populate UI
        if (isEditing && reviewId != null) {
            buttonSubmitReview.setText("Update Review"); // Change button text
            fetchExistingReview(reviewId);
        } else {
             buttonSubmitReview.setText("Submit Review"); // Set button text for new review
        }

        buttonSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void fetchExistingReview(String reviewId) {
        db.collection("reviews").document(reviewId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Review review = documentSnapshot.toObject(Review.class);
                        if (review != null) {
                            ratingBar.setRating((float) review.getRating());
                            editTextReviewText.setText(review.getReviewText());
                        } else {
                            Toast.makeText(this, "Error loading existing review.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Fetched review document but toObject returned null for review ID: " + reviewId);
                            finish(); // Close activity on error
                        }
                    } else {
                        Toast.makeText(this, "Existing review not found.", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Review document not found for ID: " + reviewId);
                        finish(); // Close if document not found
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching existing review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching review document: " + reviewId, e);
                    finish(); // Close on error
                });
    }

    private void submitReview() {
        double rating = ratingBar.getRating();
        String reviewText = editTextReviewText.getText().toString().trim();

        if (rating <= 0) {
            Toast.makeText(this, "Please provide a rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple submissions
        buttonSubmitReview.setEnabled(false);

        String reviewerId = currentUser.getUid();

        if (isEditing && reviewId != null) {
            // Update existing review
            updateReview(reviewId, rating, reviewText.isEmpty() ? null : reviewText);
        } else {
            // Create a new review
            Review review = new Review(cycleId, bookingId, reviewerId, rating, reviewText.isEmpty() ? null : reviewText);
            addReview(review);
        }
    }

    private void addReview(Review newReview) {
        // Use a Firestore Transaction to ensure atomicity when updating cycle's average rating and review count
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 1. Get the current state of the Cycle document
            DocumentReference cycleRef = db.collection("cycles").document(newReview.getCycleId());
            DocumentSnapshot cycleSnapshot = transaction.get(cycleRef);

            if (!cycleSnapshot.exists()) {
                throw new RuntimeException("Cycle document not found!"); // Should not happen if cycleId is valid
            }

            Cycle cycle = cycleSnapshot.toObject(Cycle.class);
            if (cycle == null) {
                 throw new RuntimeException("Cycle object is null.");
            }

            // 2. Calculate the new average rating and number of reviews for adding a new review
            int currentNumberOfReviews = cycle.getNumberOfReviews();
            double currentTotalRating = cycle.getAverageRating() * currentNumberOfReviews;

            int newNumberOfReviews = currentNumberOfReviews + 1;
            double newTotalRating = currentTotalRating + newReview.getRating();
            double newAverageRating = newTotalRating / newNumberOfReviews;


            // 3. Update the Cycle document within the transaction
            Map<String, Object> cycleUpdates = new HashMap<>();
            cycleUpdates.put("averageRating", newAverageRating);
            cycleUpdates.put("numberOfReviews", newNumberOfReviews);

            transaction.update(cycleRef, cycleUpdates);

            // 4. Add the new Review document
            DocumentReference reviewRef = db.collection("reviews").document(); // Firestore will generate a new ID
            transaction.set(reviewRef, newReview);

            // 5. Update the Booking document to mark it as reviewed and add reviewId
            DocumentReference bookingRef = db.collection("bookings").document(newReview.getBookingId());
             Map<String, Object> bookingUpdates = new HashMap<>();
             bookingUpdates.put("isReviewed", true);
             bookingUpdates.put("reviewId", reviewRef.getId()); // Store the new review ID in booking

            transaction.update(bookingRef, bookingUpdates);

            // Return null to indicate successful transaction
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Review submitted and cycle/booking updated successfully.");
            Toast.makeText(WriteReviewActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
            finish(); // Close activity
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error submitting review or updating cycle/booking.", e);
            Toast.makeText(WriteReviewActivity.this, "Error submitting review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            buttonSubmitReview.setEnabled(true); // Re-enable button on failure
        });
    }

    private void updateReview(String reviewId, double newRating, String newReviewText) {
         db.runTransaction((Transaction.Function<Void>) transaction -> {
             // 1. Get the current state of the Review and Cycle documents
             DocumentReference reviewRef = db.collection("reviews").document(reviewId);
             DocumentSnapshot reviewSnapshot = transaction.get(reviewRef);

             if (!reviewSnapshot.exists()) {
                 throw new RuntimeException("Review document not found!");
             }

             Review oldReview = reviewSnapshot.toObject(Review.class);
             if (oldReview == null) {
                  throw new RuntimeException("Old Review object is null.");
             }

             String currentCycleId = oldReview.getCycleId(); // Get cycleId from the old review
             DocumentReference cycleRef = db.collection("cycles").document(currentCycleId);
             DocumentSnapshot cycleSnapshot = transaction.get(cycleRef);

             if (!cycleSnapshot.exists()) {
                 throw new RuntimeException("Cycle document not found for review's cycleId!");
             }

             Cycle cycle = cycleSnapshot.toObject(Cycle.class);
             if (cycle == null) {
                  throw new RuntimeException("Cycle object is null when updating review.");
             }

             // 2. Calculate the new average rating and number of reviews for updating
             int currentNumberOfReviews = cycle.getNumberOfReviews();
             double currentTotalRating = cycle.getAverageRating() * currentNumberOfReviews;

             // Subtract the old rating's contribution
             currentTotalRating -= oldReview.getRating();

             // Add the new rating's contribution
             double newTotalRating = currentTotalRating + newRating;
             double newAverageRating = newTotalRating / currentNumberOfReviews; // Number of reviews doesn't change

             // 3. Update the Review document within the transaction
             Map<String, Object> reviewUpdates = new HashMap<>();
             reviewUpdates.put("rating", newRating);
             reviewUpdates.put("reviewText", newReviewText);
             reviewUpdates.put("createdAt", FieldValue.serverTimestamp()); // Optional: update timestamp on edit

             transaction.update(reviewRef, reviewUpdates);

             // 4. Update the Cycle document within the transaction
             Map<String, Object> cycleUpdates = new HashMap<>();
             cycleUpdates.put("averageRating", newAverageRating);
             // numberOfReviews remains the same

             transaction.update(cycleRef, cycleUpdates);


             // Return null to indicate successful transaction
             return null;
         }).addOnSuccessListener(aVoid -> {
             Log.d(TAG, "Review updated and cycle statistics recalculated successfully.");
             Toast.makeText(WriteReviewActivity.this, "Review updated successfully!", Toast.LENGTH_SHORT).show();
             finish(); // Close activity
         }).addOnFailureListener(e -> {
             Log.e(TAG, "Error updating review or recalculating cycle statistics.", e);
             Toast.makeText(WriteReviewActivity.this, "Error updating review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
             buttonSubmitReview.setEnabled(true); // Re-enable button on failure
         });
    }
}
