package com.example.micycle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.micycle.R;
import com.example.micycle.models.Booking;
import com.example.micycle.models.Cycle; // Import Cycle model
import com.example.micycle.models.User; // Import User model
import com.google.firebase.firestore.FirebaseFirestore; // Import FirebaseFirestore
import java.text.SimpleDateFormat; // Import SimpleDateFormat
import java.util.List;
import java.util.Locale; // Import Locale
import java.util.Date;
import java.util.concurrent.TimeUnit; // Import TimeUnit
import android.util.Log; // Import Log

public class AdminTransactionAdapter extends RecyclerView.Adapter<AdminTransactionAdapter.AdminTransactionViewHolder> {

    private static final String TAG = "AdminTransAdapter"; // Add a TAG for logging

    private List<Booking> bookingList;
    private OnItemClickListener listener; // Declare the listener
    private FirebaseFirestore db; // FirebaseFirestore instance

    // Define the listener interface with individual data points
    public interface OnItemClickListener {
        void onItemClick(String bookingId, String cycleName, String renterName, String ownerName, Date startTime, Date endTime, double totalCost, String bookingStatus);
    }

    public AdminTransactionAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
    }

    // Method to set the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminTransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_admin_transaction, parent, false);
        return new AdminTransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminTransactionViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // Set initial text while fetching data
        holder.cycleNameTextView.setText("Cycle: Loading...");
        holder.renterNameTextView.setText("Renter: Loading...");
        holder.ownerNameTextView.setText("Owner: Loading...");

        String formattedStartTime = formatDate(booking.getStartTime());
        String formattedEndTime = formatDate(booking.getEndTime());
        String formattedTotalCost = String.format(Locale.US, "Cost: ₹%.2f", booking.getTotalCost());
        String bookingStatus = booking.getBookingStatus() != null ? booking.getBookingStatus() : "N/A";

        holder.timesTextView.setText("Times: " + formattedStartTime + " - " + formattedEndTime);
        holder.costTextView.setText(formattedTotalCost);
        holder.statusTextView.setText("Status: " + bookingStatus);

        // Fetch and display cycle name
        fetchCycleName(booking.getCycleId(), holder.cycleNameTextView, new CycleNameFetchListener() {
            @Override
            public void onCycleNameFetched(String cycleName) {
                booking.setCycleName(cycleName); // Update booking object with fetched name
                setClickListener(holder.itemView, booking, cycleName, holder.renterNameTextView.getText().toString(), holder.ownerNameTextView.getText().toString(), booking.getStartTime(), booking.getEndTime(), booking.getTotalCost(), bookingStatus);
            }
        });

        // Fetch and display renter name
        fetchUserName(booking.getUserId(), holder.renterNameTextView, "Renter", new UserNameFetchListener() {
            @Override
            public void onUserNameFetched(String userName) {
                setClickListener(holder.itemView, booking, holder.cycleNameTextView.getText().toString(), userName, holder.ownerNameTextView.getText().toString(), booking.getStartTime(), booking.getEndTime(), booking.getTotalCost(), bookingStatus);
            }
        });

        // Fetch and display owner name
        fetchUserName(booking.getOwnerId(), holder.ownerNameTextView, "Owner", new UserNameFetchListener() {
            @Override
            public void onUserNameFetched(String userName) {
                setClickListener(holder.itemView, booking, holder.cycleNameTextView.getText().toString(), holder.renterNameTextView.getText().toString(), userName, booking.getStartTime(), booking.getEndTime(), booking.getTotalCost(), bookingStatus);
            }
        });

        // Set initial click listener that might have placeholder data before fetches complete
        setClickListener(holder.itemView, booking, holder.cycleNameTextView.getText().toString(), holder.renterNameTextView.getText().toString(), holder.ownerNameTextView.getText().toString(), booking.getStartTime(), booking.getEndTime(), booking.getTotalCost(), bookingStatus);
    }

    // Helper method to set click listener with all data
    private void setClickListener(View itemView, Booking booking, String cycleName, String renterName, String ownerName, Date startTime, Date endTime, double totalCost, String bookingStatus) {
        itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Pass all the currently available data
                listener.onItemClick(
                    booking.getBookingId(),
                    cycleName,
                    renterName,
                    ownerName,
                    startTime,
                    endTime,
                    totalCost,
                    bookingStatus
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    // Method to fetch and set the cycle name
    private void fetchCycleName(String cycleId, TextView textView, CycleNameFetchListener listener) {
        if (cycleId != null) {
            db.collection("cycles").document(cycleId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Cycle cycle = documentSnapshot.toObject(Cycle.class);
                            if (cycle != null && cycle.getModel() != null) {
                                String cycleModel = cycle.getModel();
                                textView.setText("Cycle: " + cycleModel);
                                Log.d(TAG, "Fetched cycle model for ID " + cycleId + ": " + cycleModel);
                                if (listener != null) listener.onCycleNameFetched(cycleModel);
                            } else {
                                textView.setText("Cycle: Unknown Model");
                                Log.w(TAG, "Cycle document exists for ID " + cycleId + " but model is null or cycle object is null.");
                                if (listener != null) listener.onCycleNameFetched("Unknown Model");
                            }
                        } else {
                            textView.setText("Cycle: Not Found");
                            Log.w(TAG, "Cycle document not found for ID: " + cycleId);
                            if (listener != null) listener.onCycleNameFetched("Not Found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching cycle for ID: " + cycleId, e);
                        textView.setText("Cycle: Error loading");
                        if (listener != null) listener.onCycleNameFetched("Error loading");
                    });
        } else {
            textView.setText("Cycle: N/A");
            Log.w(TAG, "Null cycle ID provided.");
            if (listener != null) listener.onCycleNameFetched("N/A");
        }
    }

    private void fetchUserName(String userId, TextView textView, String role, UserNameFetchListener listener) {
        if (userId != null) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null && user.getName() != null) {
                                String userName = user.getName();
                                textView.setText(role + ": " + userName);
                                Log.d(TAG, "Fetched " + role + " name for ID " + userId + ": " + userName);
                                if (listener != null) listener.onUserNameFetched(userName);
                            } else {
                                textView.setText(role + ": Unknown");
                                Log.w(TAG, role + " document exists for ID " + userId + " but name is null or user object is null.");
                                if (listener != null) listener.onUserNameFetched("Unknown");
                            }
                        } else {
                            textView.setText(role + ": Not Found");
                            Log.w(TAG, role + " document not found for ID: " + userId);
                            if (listener != null) listener.onUserNameFetched("Not Found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching " + role + " for ID: " + userId, e);
                        textView.setText(role + ": Error loading");
                        if (listener != null) listener.onUserNameFetched("Error loading");
                    });
        } else {
            textView.setText(role + ": N/A");
            Log.w(TAG, "Null " + role + " ID provided.");
            if (listener != null) listener.onUserNameFetched("N/A");
        }
    }

    // Helper interfaces for callbacks
    private interface CycleNameFetchListener {
        void onCycleNameFetched(String cycleName);
    }

    private interface UserNameFetchListener {
        void onUserNameFetched(String userName);
    }

    public static class AdminTransactionViewHolder extends RecyclerView.ViewHolder {
        TextView cycleNameTextView;
        TextView renterNameTextView; // TextView for Renter Name
        TextView ownerNameTextView;  // TextView for Owner Name
        TextView timesTextView;
        TextView costTextView;
        TextView statusTextView;

        public AdminTransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cycleNameTextView = itemView.findViewById(R.id.textViewTransactionCycleName);
            renterNameTextView = itemView.findViewById(R.id.textViewTransactionRenterName); // Reference to Renter Name TextView
            ownerNameTextView = itemView.findViewById(R.id.textViewTransactionOwnerName);    // Reference to Owner Name TextView
            timesTextView = itemView.findViewById(R.id.textViewTransactionTimes);
            costTextView = itemView.findViewById(R.id.textViewTransactionCost);
            statusTextView = itemView.findViewById(R.id.textViewTransactionStatus);
        }
    }

    // Method to update the data in the adapter
    public void setTransactionList(List<Booking> newBookingList) {
        this.bookingList = newBookingList;
        notifyDataSetChanged();
    }

    private String formatDate(Date date) {
        if (date == null) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return sdf.format(date);
    }
}