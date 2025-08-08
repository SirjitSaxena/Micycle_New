package com.example.micycle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.micycle.R; // Assuming R is in this package or needs to be imported
import com.example.micycle.models.User; // Assuming a User model class exists
import android.widget.Button; // Import Button

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnBlockButtonClickListener blockButtonClickListener; // Listener for block button clicks

    public AdminUserAdapter(List<User> userList) {
        this.userList = userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
        notifyDataSetChanged(); // Notify the adapter that the data set has changed
    }

    // Method to set the block button click listener
    public void setOnBlockButtonClickListener(OnBlockButtonClickListener listener) {
        this.blockButtonClickListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_user, parent, false);
        return new UserViewHolder(view, blockButtonClickListener); // Pass the listener to ViewHolder
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textViewUserName.setText(user.getName()); // Assuming getName() method exists in User model
        holder.textViewUserEmail.setText(user.getEmail()); // Assuming getEmail() method exists
        holder.textViewUserType.setText(user.getUserType()); // Assuming getUserType() method exists
        // Set button text based on user's blocked status
        if (user.getBlocked()) {
            holder.buttonToggleBlock.setText("Unblock");
            holder.buttonToggleBlock.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark)); // Example color for unblock
        } else {
            holder.buttonToggleBlock.setText("Block");
            holder.buttonToggleBlock.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark)); // Example color for block
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUserName;
        TextView textViewUserEmail;
        TextView textViewUserType;
        android.widget.Button buttonToggleBlock; // Reference to the new button

        public UserViewHolder(@NonNull View itemView, final OnBlockButtonClickListener listener) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewUserEmail = itemView.findViewById(R.id.textViewUserEmail);
            textViewUserType = itemView.findViewById(R.id.textViewUserType);
            buttonToggleBlock = itemView.findViewById(R.id.buttonToggleBlock); // Initialize the button

            // Set click listener for the block/unblock button
            buttonToggleBlock.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onBlockButtonClick(position); // Pass the position to the listener
                    }
                }
            });
        }
    }

    // Interface for the block button click listener
    public interface OnBlockButtonClickListener {
        void onBlockButtonClick(int position);
    }
}

