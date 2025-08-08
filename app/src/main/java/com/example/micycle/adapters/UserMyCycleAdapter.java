package com.example.micycle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Import Toast
import android.content.Context; // Import Context
import android.view.MotionEvent; // Import MotionEvent

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.micycle.R;
import com.example.micycle.models.Cycle;
import com.squareup.picasso.Picasso; // Added import for Picasso (uncomment or add if needed)

import java.util.List;
import java.util.Locale; // Import Locale
import com.google.android.material.button.MaterialButton; // Import MaterialButton if you are using it

public class UserMyCycleAdapter extends RecyclerView.Adapter<UserMyCycleAdapter.UserMyCycleViewHolder> {

    private List<Cycle> myCycleList;
    private OnItemClickListener listener; // Listener for item clicks (View Details)
    private OnEditButtonClickListener editListener; // Listener for Edit button clicks
    private OnRemoveButtonClickListener removeListener; // Listener for Remove button clicks
    private Context context; // Declare Context member

    // Interface for item click listener (View Details)
    public interface OnItemClickListener {
        void onItemClick(Cycle cycle);
    }

    // Interface for Edit button click listener
    public interface OnEditButtonClickListener {
        void onEditButtonClick(Cycle cycle);
    }

    // Interface for Remove button click listener
    public interface OnRemoveButtonClickListener {
        void onRemoveButtonClick(Cycle cycle);
    }


    // Methods to set the listeners
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnEditButtonClickListener(OnEditButtonClickListener editListener) {
        this.editListener = editListener;
    }

    public void setOnRemoveButtonClickListener(OnRemoveButtonClickListener removeListener) {
        this.removeListener = removeListener;
    }


    public UserMyCycleAdapter(Context context, List<Cycle> myCycleList) {
        this.context = context;
        this.myCycleList = myCycleList;
    }

    @NonNull
    @Override
    public UserMyCycleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the list item layout for My Cycles
        View view = LayoutInflater.from(context)
                .inflate(R.layout.list_item_cycle, parent, false); // Corrected layout file
        return new UserMyCycleViewHolder(view, listener, editListener, removeListener, myCycleList, context); // Pass context to ViewHolder
    }

    @Override
    public void onBindViewHolder(@NonNull UserMyCycleViewHolder holder, int position) {
        // Get the Cycle object for the current position
        Cycle cycle = myCycleList.get(position);

        // Bind the data to the UI elements in the list item layout
        holder.modelTextView.setText(cycle.getModel());
        holder.priceTextView.setText("₹" + String.format(Locale.US, "%.2f", cycle.getPricePerHour()) + "/hour"); // Format price
        holder.statusTextView.setText("Status: " + cycle.getAvailabilityStatus());

        // Load image using Picasso
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

        // TODO: Disable Edit/Remove buttons if cycle is booked, as per documentation
    }

    @Override
    public int getItemCount() {
        // Return the total number of items in the list
        return myCycleList.size();
    }

    // ViewHolder class to hold references to the UI elements in the list item
    public static class UserMyCycleViewHolder extends RecyclerView.ViewHolder {
        ImageView cycleImageView;
        TextView modelTextView;
        TextView priceTextView;
        TextView statusTextView;
        Button editButton;
        Button removeButton;
        private Context viewHolderContext; // Store context

        public UserMyCycleViewHolder(@NonNull View itemView, final OnItemClickListener itemClickListener, final OnEditButtonClickListener editBtnListener, final OnRemoveButtonClickListener removeBtnListener, final List<Cycle> cycleList, Context context) { // Accept Context
            super(itemView);
            cycleImageView = itemView.findViewById(R.id.myCycleImageView);
            modelTextView = itemView.findViewById(R.id.myCycleModelTextView);
            priceTextView = itemView.findViewById(R.id.myCyclePriceTextView);
            statusTextView = itemView.findViewById(R.id.myCycleStatusTextView);
            editButton = itemView.findViewById(R.id.editCycleButton);
            removeButton = itemView.findViewById(R.id.removeCycleButton);
            viewHolderContext = context; // Store the context

            // Set up item click listener (for View Details)
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        itemClickListener.onItemClick(cycleList.get(getAdapterPosition()));
                    }
                }
            });

            // Set up Edit button OnTouchListener to show Toast if disabled
            // Removed OnTouchListener
            // editButton.setOnTouchListener(new View.OnTouchListener() {
            //     @Override
            //     public boolean onTouch(View v, MotionEvent event) {
            //         // Check if the button is NOT enabled
            //         if (!v.isEnabled()) {
            //              // If button is disabled, consume both ACTION_DOWN and ACTION_UP
            //              if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //                  return true; // Consume the down event
            //              } else if (event.getAction() == MotionEvent.ACTION_UP) {
            //                  // Show toast on touch up
            //                  Toast.makeText(viewHolderContext, "Cannot edit: Cycle is currently booked.", Toast.LENGTH_SHORT).show();
            //                  return true; // Consume the up event
            //              }
            //         }
            //         // If button is enabled, return false to allow the OnClickListener to handle
            //         return false;
            //     }
            // });

            // Set up the actual Edit button OnClickListener (will only be called if the button is enabled and onTouch returns false)
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (editBtnListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                         Cycle cycle = cycleList.get(getAdapterPosition());
                         if ("booked".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
                            Toast.makeText(viewHolderContext, "Cannot edit: Cycle is currently booked.", Toast.LENGTH_SHORT).show();
                         } else {
                            editBtnListener.onEditButtonClick(cycle);
                         }
                    }
                }
            });


            // Set up Remove button OnTouchListener to show Toast if disabled
            // Removed OnTouchListener
            // removeButton.setOnTouchListener(new View.OnTouchListener() {
            //     @Override
            //     public boolean onTouch(View v, MotionEvent event) {
            //          // Check if the button is NOT enabled
            //          if (!v.isEnabled()) {
            //              // If button is disabled, consume both ACTION_DOWN and ACTION_UP
            //              if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //                  return true; // Consume the down event
            //              } else if (event.getAction() == MotionEvent.ACTION_UP) {
            //                 // Show toast on touch up
            //                 Toast.makeText(viewHolderContext, "Cannot remove: Cycle is currently booked.", Toast.LENGTH_SHORT).show();
            //                 return true; // Consume the up event
            //              }
            //          }
            //          // If button is enabled, return false to allow the OnClickListener to handle
            //         return false;
            //     }
            // });

            // Set up the actual Remove button OnClickListener (will only be called if the button is enabled and onTouch returns false)
            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                     if (removeBtnListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                          Cycle cycle = cycleList.get(getAdapterPosition());
                          if ("booked".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
                             Toast.makeText(viewHolderContext, "Cannot remove: Cycle is currently booked.", Toast.LENGTH_SHORT).show();
                          } else {
                             removeBtnListener.onRemoveButtonClick(cycle);
                          }
                     }
                }
            });
        }
    }

    // Method to update the data in the adapter
    public void setMyCycleList(List<Cycle> newCycleList) {
        this.myCycleList = newCycleList;
        notifyDataSetChanged(); // Notify the RecyclerView that the data has changed
    }
}
