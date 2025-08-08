package com.example.micycle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.micycle.R;
import com.example.micycle.models.Booking;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import android.util.Log;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.BookingViewHolder> {

    private static final String TAG = "TransactionAdapter"; // Correct TAG for logging

    private List<Booking> bookingList;
    private OnReviewButtonClickListener reviewButtonListener; // Listener for Leave Review
    private OnEditReviewButtonClickListener editReviewButtonListener; // Listener for Edit Review
    private OnDeleteReviewButtonClickListener deleteReviewButtonListener; // Listener for Delete Review


    public TransactionAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    // Define a new listener interface for the Leave Review button
    public interface OnReviewButtonClickListener {
        void onReviewButtonClick(Booking booking);
    }

    // Define a new listener interface for the Edit Review button
    public interface OnEditReviewButtonClickListener {
        void onEditReviewButtonClick(Booking booking);
    }

    // Define a new listener interface for the Delete Review button
    public interface OnDeleteReviewButtonClickListener {
        void onDeleteReviewButtonClick(Booking booking);
    }

    public void setOnReviewButtonClickListener(OnReviewButtonClickListener listener) {
        this.reviewButtonListener = listener;
    }

    public void setOnEditReviewButtonClickListener(OnEditReviewButtonClickListener listener) {
        this.editReviewButtonListener = listener;
    }

    public void setOnDeleteReviewButtonClickListener(OnDeleteReviewButtonClickListener listener) {
        this.deleteReviewButtonListener = listener;
    }


    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_transaction, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        holder.cycleNameTextView.setText(booking.getCycleName() != null ? booking.getCycleName() : "Unknown Cycle");


        if (booking.getTransactionType() != null) {
            if (booking.getTransactionType().equals("rented")) {
                holder.otherUserNameTextView.setText("Rented from: " + (booking.getOtherUserName() != null ? booking.getOtherUserName() : "Unknown Owner"));
                 holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_rented_background));
            } else if (booking.getTransactionType().equals("lent")) {
                holder.otherUserNameTextView.setText("Rented by: " + (booking.getOtherUserName() != null ? booking.getOtherUserName() : "Unknown Renter"));
                 holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_lent_background));
            } else {
                 holder.otherUserNameTextView.setText("User: N/A");
                  holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
            }
        } else {
            holder.otherUserNameTextView.setText("User: N/A");
             holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
        }


        holder.startTimeTextView.setText("Start: " + formatDate(booking.getStartTime()));
        holder.endTimeTextView.setText("End: " + formatDate(booking.getEndTime()));
        holder.totalCostTextView.setText(String.format(Locale.US, "Cost: ₹%.2f", booking.getTotalCost()));
        holder.statusTextView.setText(booking.getBookingStatus() != null ? booking.getBookingStatus() : "N/A");

        // Control visibility of the review buttons
        boolean isRentedCompleted = "rented".equals(booking.getTransactionType()) && "completed".equals(booking.getBookingStatus());

        if (isRentedCompleted && !booking.isReviewed()) {
            // Show Leave Review button
            holder.leaveReviewButton.setVisibility(View.VISIBLE);
            holder.editReviewButton.setVisibility(View.GONE);
            holder.deleteReviewButton.setVisibility(View.GONE);

            holder.leaveReviewButton.setOnClickListener(v -> {
                if (reviewButtonListener != null) {
                    reviewButtonListener.onReviewButtonClick(booking);
                }
            });
            holder.editReviewButton.setOnClickListener(null);
            holder.deleteReviewButton.setOnClickListener(null);

        } else if (isRentedCompleted && booking.isReviewed()) {
            // Show Edit and Delete Review buttons
            holder.leaveReviewButton.setVisibility(View.GONE);
            holder.editReviewButton.setVisibility(View.VISIBLE);
            holder.deleteReviewButton.setVisibility(View.VISIBLE);

            holder.leaveReviewButton.setOnClickListener(null);
            holder.editReviewButton.setOnClickListener(v -> {
                if (editReviewButtonListener != null) {
                    editReviewButtonListener.onEditReviewButtonClick(booking);
                }
            });
            holder.deleteReviewButton.setOnClickListener(v -> {
                if (deleteReviewButtonListener != null) {
                    deleteReviewButtonListener.onDeleteReviewButtonClick(booking);
                }
            });

        } else {
            // Hide all review buttons for other transaction types or statuses
            holder.leaveReviewButton.setVisibility(View.GONE);
            holder.editReviewButton.setVisibility(View.GONE);
            holder.deleteReviewButton.setVisibility(View.GONE);

            holder.leaveReviewButton.setOnClickListener(null);
            holder.editReviewButton.setOnClickListener(null);
            holder.deleteReviewButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView cycleNameTextView;
        TextView otherUserNameTextView;
        TextView startTimeTextView;
        TextView endTimeTextView;
        TextView totalCostTextView;
        TextView statusTextView;
        Button leaveReviewButton; // Button for Leave Review
        Button editReviewButton; // Button for Edit Review
        Button deleteReviewButton; // Button for Delete Review


        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            cycleNameTextView = itemView.findViewById(R.id.transactionCycleNameTextView);
            otherUserNameTextView = itemView.findViewById(R.id.transactionOtherUserNameTextView);
            startTimeTextView = itemView.findViewById(R.id.transactionStartTimeTextView);
            endTimeTextView = itemView.findViewById(R.id.transactionEndTimeTextView);
            totalCostTextView = itemView.findViewById(R.id.transactionTotalCostTextView);
            statusTextView = itemView.findViewById(R.id.transactionStatusTextView);
            leaveReviewButton = itemView.findViewById(R.id.leaveReviewButton); // Reference to the Leave Review Button
            editReviewButton = itemView.findViewById(R.id.editReviewButton); // Reference to the Edit Review Button
            deleteReviewButton = itemView.findViewById(R.id.deleteReviewButton); // Reference to the Delete Review Button
        }
    }

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