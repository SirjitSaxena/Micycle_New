package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.micycle.adapters.ReviewAdapter;
import com.example.micycle.models.Review;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReviewsListActivity extends AppCompatActivity {

    private RecyclerView reviewsRecyclerView;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private FirebaseFirestore db;
    private String cycleId;

    private static final String TAG = "ReviewsListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews_list);

        db = FirebaseFirestore.getInstance();

        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewList);
        reviewsRecyclerView.setAdapter(reviewAdapter);

        // Get the cycle ID from the Intent
        Intent intent = getIntent();
        if (intent.hasExtra("cycle_id")) {
            cycleId = intent.getStringExtra("cycle_id");
            if (cycleId != null && !cycleId.isEmpty()) {
                fetchReviewsForCycle(cycleId);
            } else {
                Toast.makeText(this, "Cycle ID is missing.", Toast.LENGTH_SHORT).show();
                finish(); // Close activity if no ID is passed
            }
        } else {
            Toast.makeText(this, "Cycle ID is missing.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no ID is passed
        }
    }

    private void fetchReviewsForCycle(String cycleId) {
        db.collection("reviews")
                .whereEqualTo("cycleId", cycleId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reviewList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Review review = document.toObject(Review.class);
                            reviewList.add(review);
                        }
                        reviewAdapter.setReviewList(reviewList); // Update the adapter with the fetched list
                        Log.d(TAG, "Fetched " + reviewList.size() + " reviews for cycle " + cycleId);
                    } else {
                        Log.w(TAG, "Error getting reviews.", task.getException());
                        Toast.makeText(this, "Error loading reviews: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
