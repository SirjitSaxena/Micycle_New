package com.example.micycle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.micycle.R;
import com.example.micycle.models.Location;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locationList;
    // Add an OnItemClickListener interface
    private OnItemClickListener listener;

    // Define the OnItemClickListener interface
    public interface OnItemClickListener {
        void onItemClick(Location location);
    }

    public LocationAdapter(List<Location> locationList) {
        this.locationList = locationList;
    }

    // Method to set the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_location, parent, false);
        return new LocationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locationList.get(position);
        holder.locationNameTextView.setText(location.getName());
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    // Method to update the list of locations
    public void setLocations(List<Location> locations) {
        this.locationList = locations;
        notifyDataSetChanged(); // Notify the adapter that the data has changed
    }

    class LocationViewHolder extends RecyclerView.ViewHolder { // Make ViewHolder non-static to access listener
        TextView locationNameTextView;

        LocationViewHolder(View itemView) {
            super(itemView);
            locationNameTextView = itemView.findViewById(R.id.locationNameTextView);

            // Set click listener for the item view
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(locationList.get(position));
                }
            });
        }
    }
}
