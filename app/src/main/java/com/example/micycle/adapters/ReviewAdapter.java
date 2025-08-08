package com.example.micycle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.micycle.R;
import com.example.micycle.models.Review;
import com.example.micycle.models.User; // Import User model
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore
import android.util.Log; // Import Log

import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;
    private FirebaseFirestore db; // Add Firestore instance

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
        this.db = FirebaseFirestore.getInstance(); // Initialize Firestore
    }

    public void setReviewList(List<Review> reviewList) {
        this.reviewList = reviewList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Display rating
        holder.reviewRatingTextView.setText(String.format(Locale.US, "Rating: %.1f ⭐", review.getRating()));

        // Display review comment
        holder.reviewCommentTextView.setText(review.getReviewText()); // Use getReviewText()

        // Fetch and display reviewer name
        fetchReviewerName(review.getReviewerId(), holder.reviewerNameTextView);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    private void fetchReviewerName(String reviewerId, TextView textView) {
        db.collection("users").document(reviewerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getName() != null) {
                            textView.setText(user.getName());
                        } else {
                            textView.setText("Unknown User");
                            Log.w("ReviewAdapter", "User data is null or name is missing for ID: " + reviewerId);
                        }
                    } else {
                        textView.setText("User Not Found");
                        Log.w("ReviewAdapter", "User with ID " + reviewerId + " not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    textView.setText("Error");
                    Log.w("ReviewAdapter", "Error fetching user " + reviewerId, e);
                });
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerNameTextView;
        TextView reviewRatingTextView;
        TextView reviewCommentTextView;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerNameTextView = itemView.findViewById(R.id.reviewerNameTextView);
            reviewRatingTextView = itemView.findViewById(R.id.reviewRatingTextView);
            reviewCommentTextView = itemView.findViewById(R.id.reviewCommentTextView);
        }
    }
}