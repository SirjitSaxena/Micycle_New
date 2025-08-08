package com.example.micycle.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color; // Import Color
import android.util.Log; // Import Log for error logging

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.micycle.R; // Make sure this R import is correct
import com.example.micycle.models.Cycle; // Import the Cycle model
import com.example.micycle.models.Location; // Import Location model
import com.google.firebase.firestore.FirebaseFirestore; // Import FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale; // Import Locale

public class CycleAdapter extends RecyclerView.Adapter<CycleAdapter.CycleViewHolder> {

    private List<Cycle> cycleList;
    private OnItemClickListener listener; // Declare the listener
    private Context context; // Declare Context member
    private String currentUserId; // Declare currentUserId member
    private FirebaseFirestore db; // Declare FirebaseFirestore instance

    // Interface for item click listener
    public interface OnItemClickListener {
        void onItemClick(Cycle cycle);
    }

    // Method to set the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public CycleAdapter(Context context, List<Cycle> cycleList, String currentUserId) {
        this.context = context;
        this.cycleList = cycleList;
        this.currentUserId = currentUserId;
        db = FirebaseFirestore.getInstance(); // Initialize FirebaseFirestore
    }

    @NonNull
    @Override
    public CycleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the CORRECT list item layout for available cycles using the stored context
        View view = LayoutInflater.from(context)
                .inflate(R.layout.list_item_available_cycle, parent, false); // Corrected layout file
        return new CycleViewHolder(view, listener, cycleList);
    }

    @Override
    public void onBindViewHolder(@NonNull CycleViewHolder holder, int position) {
        // Get the Cycle object for the current position
        Cycle cycle = cycleList.get(position);

        // Bind the data to the UI elements in the list item layout
        String modelText = cycle.getModel();
        // Add "(Yours)" tag if the cycle is owned by the current user
        if (currentUserId != null && cycle.getOwnerId() != null && cycle.getOwnerId().equals(currentUserId)) {
            modelText = modelText + " (Yours)";
        }
        holder.modelTextView.setText(modelText);
        holder.priceTextView.setText("₹" + String.format(Locale.US, "%.2f", cycle.getPricePerHour()) + "/hour"); // Format price
        
        List<String> imageUrls = cycle.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String firstImageUrl = imageUrls.get(0);
            Picasso.get().load(firstImageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground) // Use a placeholder drawable
                    .error(R.drawable.ic_launcher_foreground) // Use an error drawable
                    .into(holder.cycleImageView);
        } else {
            // Set a default image if no URLs are available
            holder.cycleImageView.setImageResource(R.drawable.ic_launcher_foreground); // Use your default image resource
        }
        // holder.cycleImageView.setImage... (using cycle.getImageUrls().get(0) or similar)

        // Fetch and display Location name from locationId
        String locationId = cycle.getLocationId();
        if (locationId != null && !locationId.isEmpty()) {
            db.collection("locations").document(locationId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Location location = documentSnapshot.toObject(Location.class);
                            if (location != null && location.getName() != null) {
                                holder.locationTextView.setText("Location: " + location.getName());
                            } else {
                                holder.locationTextView.setText("Location: Unknown");
                                Log.w("CycleAdapter", "Location data is null or name is missing for ID: " + locationId);
                            }
                        } else {
                            holder.locationTextView.setText("Location: Not found");
                            Log.w("CycleAdapter", "Location with ID " + locationId + " not found.");
                        }
                    })
                    .addOnFailureListener(e -> {
                         Log.w("CycleAdapter", "Error fetching location " + locationId, e);
                         holder.locationTextView.setText("Location: Error");
                    });
        } else {
            holder.locationTextView.setText("Location: N/A"); // Handle case with no location ID
        }

        // Highlight cycles owned by the current user

        // Display Rating and Review Count
        if (cycle.getNumberOfReviews() > 0) {
            holder.ratingTextView.setText(String.format(Locale.US, "%.1f ⭐", cycle.getAverageRating()));
            holder.reviewCountTextView.setText(String.format(Locale.US, "(%d)", cycle.getNumberOfReviews()));
            holder.ratingTextView.setVisibility(View.VISIBLE);
            holder.reviewCountTextView.setVisibility(View.VISIBLE);
        } else {
            holder.ratingTextView.setVisibility(View.GONE);
            holder.reviewCountTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        // Return the total number of items in the list
        return cycleList.size();
    }

    // ViewHolder class to hold references to the UI elements in the list item
    public static class CycleViewHolder extends RecyclerView.ViewHolder {
        ImageView cycleImageView;
        TextView modelTextView;
        TextView priceTextView;
        TextView locationTextView;
        TextView ratingTextView;
        TextView reviewCountTextView;

        public CycleViewHolder(@NonNull View itemView, final OnItemClickListener listener, final List<Cycle> cycleList) {
            super(itemView);
            // Get references to the UI elements using the CORRECT IDs from list_item_available_cycle.xml
            cycleImageView = itemView.findViewById(R.id.cycleImageView); // This ID should be in list_item_available_cycle.xml
            modelTextView = itemView.findViewById(R.id.cycleModelTextView); // This ID should be in list_item_available_cycle.xml
            priceTextView = itemView.findViewById(R.id.cyclePriceTextView); // This ID should be in list_item_available_cycle.xml
            locationTextView = itemView.findViewById(R.id.cycleLocationTextView); // This ID should be in list_item_available_cycle.xml
            ratingTextView = itemView.findViewById(R.id.cycleRatingTextView);
            reviewCountTextView = itemView.findViewById(R.id.cycleReviewCountTextView);

            // Set up item click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        listener.onItemClick(cycleList.get(getAdapterPosition())); // Call the listener's onItemClick method
                    }
                }
            });
        }
    }

    // Method to update the data in the adapter
    public void setCycleList(List<Cycle> newCycleList) {
        this.cycleList = newCycleList;
        notifyDataSetChanged(); // Notify the RecyclerView that the data has changed
    }

    // Method to set the current user ID
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged(); // Refresh the list to apply highlighting
    }
}
