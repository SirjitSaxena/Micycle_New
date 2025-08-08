package com.example.micycle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.micycle.adapters.TransactionAdapter;
import com.example.micycle.models.Booking;
import com.example.micycle.models.Cycle;
import com.example.micycle.models.Review;
import com.example.micycle.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionHistoryActivity extends AppCompatActivity implements
        TransactionAdapter.OnReviewButtonClickListener,
        TransactionAdapter.OnEditReviewButtonClickListener,
        TransactionAdapter.OnDeleteReviewButtonClickListener {

    private static final String TAG = "TransactionHistory";

    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter bookingAdapter;
    private List<Booking> bookingList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private ToggleButton sortToggle;
    private boolean isNewestFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        bookingList = new ArrayList<>();
        bookingAdapter = new TransactionAdapter(bookingList);
        bookingAdapter.setOnReviewButtonClickListener(this);
        bookingAdapter.setOnEditReviewButtonClickListener(this);
        bookingAdapter.setOnDeleteReviewButtonClickListener(this);
        transactionsRecyclerView.setAdapter(bookingAdapter);

        sortToggle = findViewById(R.id.sortToggle);
        sortToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                isNewestFirst = true;
                applySortingAndNotifyAdapter();
                sortToggle.setTextColor(getResources().getColor(R.color.toggle_button_text_color_checked));
            } else {
                isNewestFirst = false;
                applySortingAndNotifyAdapter();
                sortToggle.setTextColor(getResources().getColor(R.color.toggle_button_text_color_unchecked));
            }
        });

        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchUserBookings();
    }

    private void fetchUserBookings() {
        Log.d(TAG, "Fetching user bookings for user ID: " + currentUserId);
        List<Booking> fetchedBookings = new ArrayList<>();
        AtomicInteger pendingQueries = new AtomicInteger(2);

        db.collection("bookings")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully fetched rented bookings. Count: " + queryDocumentSnapshots.size());
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        booking.setBookingId(document.getId());
                        booking.setTransactionType("rented");
                        fetchedBookings.add(booking);
                    }
                    if (pendingQueries.decrementAndGet() == 0) {
                        processFetchedBookings(fetchedBookings);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching rented bookings.", e);
                    if (pendingQueries.decrementAndGet() == 0) {
                        processFetchedBookings(fetchedBookings);
                    }
                });

        db.collection("bookings")
                .whereEqualTo("ownerId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully fetched lent bookings. Count: " + queryDocumentSnapshots.size());
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Booking booking = document.toObject(Booking.class);
                        booking.setBookingId(document.getId());
                        booking.setTransactionType("lent");
                        fetchedBookings.add(booking);
                    }
                    if (pendingQueries.decrementAndGet() == 0) {
                        processFetchedBookings(fetchedBookings);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching lent bookings.\n", e);
                    if (pendingQueries.decrementAndGet() == 0) {
                        processFetchedBookings(fetchedBookings);
                    }
                });
    }

    private void processFetchedBookings(List<Booking> fetchedBookings) {
        Log.d(TAG, "Processing fetched bookings. Total count: " + fetchedBookings.size());
        bookingList.clear();
        bookingList.addAll(fetchedBookings);

        if (bookingList.isEmpty()) {
            Log.d(TAG, "Booking list is empty. Initializing adapter with empty list.");
            bookingAdapter.notifyDataSetChanged();
            return;
        }

        AtomicInteger pendingOtherUserFetches = new AtomicInteger(bookingList.size());

        for (Booking booking : bookingList) {
            String cycleId = booking.getCycleId();
            if (cycleId != null) {
                db.collection("cycles").document(cycleId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Cycle cycle = documentSnapshot.toObject(Cycle.class);
                                if (cycle != null && cycle.getModel() != null) {
                                    booking.setCycleName(cycle.getModel());
                                    Log.d(TAG, "Fetched cycle model for booking " + booking.getBookingId() + ": " + cycle.getModel());
                                } else {
                                    booking.setCycleName("Unknown Cycle");
                                    Log.w(TAG, "Cycle document exists for booking " + booking.getBookingId() + " but model is null or cycle object is null.\n");
                                }
                            } else {
                                booking.setCycleName("Cycle Not Found");
                                Log.w(TAG, "Cycle document not found for booking ID: " + cycleId);
                            }

                            if (pendingOtherUserFetches.decrementAndGet() == 0) {
                                fetchReviewStatusForBookings();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching cycle for ID: " + cycleId, e);
                            booking.setCycleName("Error Fetching Cycle");

                            if (pendingOtherUserFetches.decrementAndGet() == 0) {
                                fetchReviewStatusForBookings();
                            }
                        });
            } else {
                booking.setCycleName("Invalid Cycle ID");
                Log.w(TAG, "Booking " + booking.getBookingId() + " has invalid cycle ID.\n");
                if (pendingOtherUserFetches.decrementAndGet() == 0) {
                    fetchReviewStatusForBookings();
                }
            }
        }
    }

    private void fetchReviewStatusForBookings() {
        Log.d(TAG, "Fetching review status for bookings.");
        AtomicInteger pendingReviewStatusFetches = new AtomicInteger(bookingList.size());

        for (Booking booking : bookingList) {
            if ("rented".equals(booking.getTransactionType()) && "completed".equals(booking.getBookingStatus())) {
                db.collection("reviews")
                        .whereEqualTo("bookingId", booking.getBookingId())
                        .limit(1)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                booking.setReviewed(true);
                                booking.setReviewId(queryDocumentSnapshots.getDocuments().get(0).getId());
                                Log.d(TAG, "Booking " + booking.getBookingId() + " is reviewed. Review ID: " + booking.getReviewId());
                            } else {
                                booking.setReviewed(false);
                                booking.setReviewId(null);
                                Log.d(TAG, "Booking " + booking.getBookingId() + " is not reviewed.");
                            }

                            if (pendingReviewStatusFetches.decrementAndGet() == 0) {
                                fetchOtherUserNames();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching review status for booking: " + booking.getBookingId(), e);
                            booking.setReviewed(false);
                            booking.setReviewId(null);
                            if (pendingReviewStatusFetches.decrementAndGet() == 0) {
                                fetchOtherUserNames();
                            }
                        });
            } else {
                booking.setReviewed(false);
                booking.setReviewId(null);
                if (pendingReviewStatusFetches.decrementAndGet() == 0) {
                    fetchOtherUserNames();
                }
            }
        }
    }

    private void fetchOtherUserNames() {
        Log.d(TAG, "Fetching other user names for bookings.");
        AtomicInteger pendingOtherUserFetches = new AtomicInteger(bookingList.size());

        for (Booking booking : bookingList) {
            String otherUserId = booking.getTransactionType().equals("rented") ? booking.getOwnerId() : booking.getUserId();
            String role = booking.getTransactionType().equals("rented") ? "Owner" : "Renter";

            if (otherUserId != null) {
                db.collection("users").document(otherUserId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                User otherUser = documentSnapshot.toObject(User.class);
                                if (otherUser != null && otherUser.getName() != null) {
                                    booking.setOtherUserName(otherUser.getName());
                                    Log.d(TAG, "Fetched " + role + " name for booking " + booking.getBookingId() + ": " + otherUser.getName());
                                } else {
                                    booking.setOtherUserName("Unknown " + role);
                                    Log.w(TAG, role + " document exists for booking " + booking.getBookingId() + " but name is null or user object is null.\n");
                                }
                            } else {
                                booking.setOtherUserName(role + " Not Found");
                                Log.w(TAG, role + " document not found for user ID: " + otherUserId);
                            }

                            if (pendingOtherUserFetches.decrementAndGet() == 0) {
                                applySortingAndNotifyAdapter();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching " + role + " for ID: " + otherUserId, e);
                            booking.setOtherUserName("Error Fetching " + role);
                            if (pendingOtherUserFetches.decrementAndGet() == 0) {
                                applySortingAndNotifyAdapter();
                            }
                        });
            } else {
                booking.setOtherUserName("N/A");
                Log.w(TAG, role + " ID is null for booking " + booking.getBookingId() + "\n");
                if (pendingOtherUserFetches.decrementAndGet() == 0) {
                    applySortingAndNotifyAdapter();
                }
            }
        }
    }

    private void applySortingAndNotifyAdapter() {
        if (isNewestFirst) {
            Collections.sort(bookingList, (b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()));
        } else {
            Collections.sort(bookingList, (b1, b2) -> b1.getCreatedAt().compareTo(b2.getCreatedAt()));
        }
        bookingAdapter.notifyDataSetChanged();
        Log.d(TAG, "Booking list sorted and adapter notified.");
    }

    private void updateAdapter() {
        // This method seems redundant now that applySortingAndNotifyAdapter handles the update
        // bookingAdapter.notifyDataSetChanged();
    }

    @Override
    public void onReviewButtonClick(Booking booking) {
        Log.d(TAG, "Leave Review button clicked for booking: " + booking.getBookingId());
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra("bookingId", booking.getBookingId());
        intent.putExtra("cycleId", booking.getCycleId());
        startActivity(intent);
    }

    @Override
    public void onEditReviewButtonClick(Booking booking) {
        Log.d(TAG, "Edit Review button clicked for booking: " + booking.getBookingId());
        if (booking.getReviewId() != null) {
            Intent intent = new Intent(this, WriteReviewActivity.class);
            intent.putExtra("bookingId", booking.getBookingId());
            intent.putExtra("cycleId", booking.getCycleId());
            intent.putExtra("reviewId", booking.getReviewId());
            startActivity(intent);
        } else {
            Log.w(TAG, "Edit Review clicked but booking has no reviewId: " + booking.getBookingId());
            Toast.makeText(this, "Cannot edit review, review ID not found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteReviewButtonClick(Booking booking) {
        Log.d(TAG, "Delete Review button clicked for booking: " + booking.getBookingId());
        if (booking.getReviewId() != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Review")
                    .setMessage("Are you sure you want to delete this review?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            deleteReview(booking);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            Log.w(TAG, "Delete Review clicked but booking has no reviewId: " + booking.getBookingId());
            Toast.makeText(this, "Cannot delete review, review ID not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteReview(Booking booking) {
        Log.d(TAG, "Deleting review: " + booking.getReviewId() + " for booking: " + booking.getBookingId());
        if (booking.getReviewId() == null) return;

        WriteBatch batch = db.batch();

        batch.delete(db.collection("reviews").document(booking.getReviewId()));

        batch.update(db.collection("bookings").document(booking.getBookingId()),
                "isReviewed", false,
                "reviewId", null);

        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Review and booking updated successfully in batch.");
            Toast.makeText(TransactionHistoryActivity.this, "Review deleted.", Toast.LENGTH_SHORT).show();

            booking.setReviewed(false);
            booking.setReviewId(null);

            updateCycleReviewStatistics(booking.getCycleId());

            int position = bookingList.indexOf(booking);
            if (position != -1) {
                bookingAdapter.notifyItemChanged(position);
            } else {
                fetchUserBookings();
            }
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error deleting review or updating booking in batch.", e);
            Toast.makeText(TransactionHistoryActivity.this, "Failed to delete review.", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCycleReviewStatistics(String cycleId) {
        Log.d(TAG, "Updating cycle review statistics for cycle ID: " + cycleId);
        db.collection("reviews")
                .whereEqualTo("cycleId", cycleId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalRating = 0;
                    int numberOfReviews = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Review review = document.toObject(Review.class);
                        totalRating += review.getRating();
                        numberOfReviews++;
                    }
                    double averageRating = (numberOfReviews == 0) ? 0.0 : totalRating / numberOfReviews;

                    Log.d(TAG, String.format(Locale.US, "Calculated Average Rating for cycle %s: %.1f (%d reviews)", cycleId, averageRating, numberOfReviews));

                    db.collection("cycles").document(cycleId)
                            .update("averageRating", averageRating, "numberOfReviews", numberOfReviews)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Cycle " + cycleId + " review statistics updated successfully.");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating cycle review statistics for cycle " + cycleId, e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching reviews for cycle " + cycleId + " to update statistics.", e);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserBookings();
    }
}
