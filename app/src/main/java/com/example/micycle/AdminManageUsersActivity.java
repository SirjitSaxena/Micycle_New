package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.micycle.adapters.AdminUserAdapter;
import com.example.micycle.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.widget.Toast;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.example.micycle.adapters.AdminUserAdapter.OnBlockButtonClickListener;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import java.util.Date;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.micycle.models.Message;

public class AdminManageUsersActivity extends AppCompatActivity implements OnBlockButtonClickListener {

    private RecyclerView adminUsersRecyclerView;
    private AdminUserAdapter adminUserAdapter;
    private List<User> userList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final String TAG = "AdminManageUsers";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_users);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        adminUsersRecyclerView = findViewById(R.id.adminUsersRecyclerView);
        adminUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adminUserAdapter = new AdminUserAdapter(userList);
        adminUsersRecyclerView.setAdapter(adminUserAdapter);

        adminUserAdapter.setOnBlockButtonClickListener(this);

        fetchUsers();
    }

    private void fetchUsers() {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            user.setUserId(document.getId());
                            userList.add(user);
                        }
                        adminUserAdapter.setUserList(userList);
                        Log.d(TAG, "Users fetched: " + userList.size());
                    } else {
                        Log.w(TAG, "Error getting users.", task.getException());
                        Toast.makeText(AdminManageUsersActivity.this, "Error fetching users: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUsers();
    }

    @Override
    public void onBlockButtonClick(int position) {
        User userToModify = userList.get(position);
        boolean isCurrentlyBlocked = userToModify.getBlocked();
        String userId = userToModify.getUserId();
        String userName = userToModify.getName();

        boolean newBlockedStatus = !isCurrentlyBlocked;
        String actionType = newBlockedStatus ? "user_blocked" : "user_unblocked";
        String actionMessage = newBlockedStatus ? "Block" : "Unblock";

        new AlertDialog.Builder(this)
                .setTitle(actionMessage + " User: " + userName)
                .setMessage("Please provide a reason for this action:")
                .setView(R.layout.dialog_admin_message)
                .setPositiveButton("Submit", (dialog, which) -> {
                    EditText messageEditText = ((AlertDialog) dialog).findViewById(R.id.editTextAdminMessage);
                    String adminMessage = messageEditText.getText().toString().trim();

                    if (adminMessage.isEmpty()) {
                        Toast.makeText(AdminManageUsersActivity.this, "Reason cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (userId != null) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("blocked", newBlockedStatus);

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    userToModify.setBlocked(newBlockedStatus);
                                    adminUserAdapter.notifyItemChanged(position);

                                    String statusMessage = newBlockedStatus ? "blocked" : "unblocked";
                                    Toast.makeText(AdminManageUsersActivity.this, userName + " has been " + statusMessage, Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, userName + " (ID: " + userId + ") has been " + statusMessage);

                                    createAdminMessage(userId, actionType, userId, userName, adminMessage);

                                })
                                .addOnFailureListener(e -> {
                                    String action = newBlockedStatus ? "blocking" : "unblocking";
                                    Log.w(TAG, "Error " + action + " user with ID: " + userId, e);
                                    Toast.makeText(AdminManageUsersActivity.this, "Error " + action + " user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.w(TAG, "Attempted to " + actionMessage.toLowerCase() + " user with null userId.");
                        Toast.makeText(this, "Error: User ID is missing.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void createAdminMessage(String userId, String actionType, String relatedItemId, String relatedItemName, String messageContent) {
        FirebaseUser adminUser = mAuth.getCurrentUser();
        if (adminUser == null) {
            Log.w(TAG, "Admin user not logged in, cannot create message.");
            return;
        }

        String adminId = adminUser.getUid();
        Date timestamp = new Date();

        Message newMessage = new Message(userId, adminId, actionType, relatedItemId, relatedItemName, messageContent, timestamp);

        db.collection("messages")
                .add(newMessage)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Admin message added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding admin message", e));
    }
}
