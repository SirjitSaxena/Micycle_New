package com.example.micycle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.micycle.R;
import com.example.micycle.models.Cycle;
import java.util.List;
import android.widget.ImageView;
import android.content.Context;
import com.squareup.picasso.Picasso;
import android.widget.ImageButton;

public class AdminCycleAdapter extends RecyclerView.Adapter<AdminCycleAdapter.AdminCycleViewHolder> {

    private List<Cycle> cycleList;
    private Context context;
    private OnAdminCycleActionListener listener;

    public interface OnAdminCycleActionListener {
        void onBlockClick(Cycle cycle, int position);
    }

    public AdminCycleAdapter(Context context, List<Cycle> cycleList, OnAdminCycleActionListener listener) {
        this.context = context;
        this.cycleList = cycleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminCycleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_admin_cycle, parent, false);
        return new AdminCycleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminCycleViewHolder holder, int position) {
        Cycle cycle = cycleList.get(position);
        holder.adminCycleModelTextView.setText(cycle.getModel());
        holder.adminCyclePriceTextView.setText("Price: ₹" + cycle.getPricePerHour() + "/hr");
        holder.adminCycleStatusTextView.setText("Status: " + cycle.getAvailabilityStatus());

        List<String> imageUrls = cycle.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String imageUrl = imageUrls.get(0);
            Picasso.get().load(imageUrl).placeholder(R.drawable.ic_launcher_foreground).into(holder.adminCycleImageView);
        } else {
            holder.adminCycleImageView.setImageResource(R.drawable.ic_launcher_foreground);
        }

        if ("blocked".equalsIgnoreCase(cycle.getAvailabilityStatus())) {
            holder.adminBlockCycleButton.setImageResource(R.drawable.ic_unblock);
            holder.adminBlockCycleButton.setContentDescription("Unblock Cycle");
        } else {
            holder.adminBlockCycleButton.setImageResource(R.drawable.ic_block);
            holder.adminBlockCycleButton.setContentDescription("Block Cycle");
        }

        holder.adminBlockCycleButton.setOnClickListener(v -> {
            if (listener != null && holder.adminBlockCycleButton.isEnabled()) {
                listener.onBlockClick(cycle, holder.getAdapterPosition());
            }
        });

        boolean isBooked = cycle.getAvailabilityStatus() != null && cycle.getAvailabilityStatus().equals("booked");
        boolean isBlocked = cycle.getAvailabilityStatus() != null && cycle.getAvailabilityStatus().equals("blocked");

        holder.adminBlockCycleButton.setEnabled(!isBooked);
        holder.adminBlockCycleButton.setAlpha(isBooked ? 0.5f : 1.0f);
    }

    @Override
    public int getItemCount() {
        return cycleList.size();
    }

    public static class AdminCycleViewHolder extends RecyclerView.ViewHolder {
        ImageView adminCycleImageView;
        TextView adminCycleModelTextView;
        TextView adminCyclePriceTextView;
        TextView adminCycleStatusTextView;
        ImageButton adminBlockCycleButton;

        public AdminCycleViewHolder(@NonNull View itemView) {
            super(itemView);
            adminCycleImageView = itemView.findViewById(R.id.adminCycleImageView);
            adminCycleModelTextView = itemView.findViewById(R.id.adminCycleModelTextView);
            adminCyclePriceTextView = itemView.findViewById(R.id.adminCyclePriceTextView);
            adminCycleStatusTextView = itemView.findViewById(R.id.adminCycleStatusTextView);
            adminBlockCycleButton = itemView.findViewById(R.id.adminBlockCycleButton);
        }
    }

    public void setCycleList(List<Cycle> newCycleList) {
        this.cycleList = newCycleList;
        notifyDataSetChanged();
    }
}