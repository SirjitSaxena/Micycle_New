package com.example.micycle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.micycle.R;
import com.example.micycle.models.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String formattedTimestamp = sdf.format(message.getTimestamp());

        // Display action type and related item name
        String actionText = formatActionType(message.getActionType());
        if (message.getRelatedItemName() != null && !message.getRelatedItemName().isEmpty()) {
            actionText += ": " + message.getRelatedItemName();
        }
        holder.textViewActionType.setText("Action: " + actionText);

        // Display message content and timestamp
        holder.textViewMessageContent.setText("Message: " + message.getMessageContent());
        holder.textViewTimestamp.setText("Time: " + formattedTimestamp);

        // Optionally include related item ID if needed, for now skipping
        // holder.textViewRelatedItem.setText("Related Item ID: " + message.getRelatedItemId());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewActionType;
        TextView textViewMessageContent;
        TextView textViewTimestamp;
        // TextView textViewRelatedItem; // If you decide to include related item ID

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewActionType = itemView.findViewById(R.id.textViewActionType);
            textViewMessageContent = itemView.findViewById(R.id.textViewMessageContent);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            // textViewRelatedItem = itemView.findViewById(R.id.textViewRelatedItem);
        }
    }

    // Helper method to format the action type for display
    private String formatActionType(String actionType) {
        if (actionType == null) {
            return "Unknown Action";
        }
        switch (actionType) {
            case "cycle_edited":
                return "Cycle Edited";
            case "cycle_removed":
                return "Cycle Removed";
            case "cycle_blocked":
                return "Cycle Blocked";
            case "cycle_unblocked":
                return "Cycle Unblocked";
            case "user_blocked":
                return "User Blocked";
            case "user_unblocked":
                return "User Unblocked";
            default:
                return actionType; // Return as is if unknown
        }
    }
}
