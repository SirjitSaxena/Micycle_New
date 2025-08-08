package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity; // Import Activity
import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; // Use androidx AlertDialog

import com.example.micycle.adapters.ImageViewPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {

    private ViewPager2 imageViewPager;
    private Button deleteImageButton;
    private ImageViewPagerAdapter adapter;
    private List<String> imageUrls; // This list is now local to this activity
    private String cycleId; // Keep cycleId for context, but not for Firestore ops here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        imageViewPager = findViewById(R.id.imageViewPager);
        deleteImageButton = findViewById(R.id.deleteImageButton);

        // Get image URLs and cycle ID from the Intent
        imageUrls = getIntent().getStringArrayListExtra("imageUrls");
        cycleId = getIntent().getStringExtra("cycleId"); // Still get cycleId for context

        if (imageUrls == null) { // Check for null list
            imageUrls = new ArrayList<>(); // Initialize as empty if null
        }

        if (imageUrls.isEmpty()) {
            Toast.makeText(this, "No images to display.", Toast.LENGTH_SHORT).show();
            // Return the empty list back to the calling activity
            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("updatedImageUrls", new ArrayList<>(imageUrls));
            setResult(Activity.RESULT_OK, resultIntent);
            finish(); // Close the activity if no images
            return;
        }

        adapter = new ImageViewPagerAdapter(this, imageUrls);
        imageViewPager.setAdapter(adapter);

        deleteImageButton.setOnClickListener(v -> {
            int currentPosition = imageViewPager.getCurrentItem();
            if (currentPosition >= 0 && currentPosition < imageUrls.size()) {
                String imageUrlToDelete = imageUrls.get(currentPosition);
                confirmAndDeleteImage(imageUrlToDelete, currentPosition);
            }
        });
    }

    private void confirmAndDeleteImage(String imageUrl, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image locally?") // Clarify local deletion
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteImageLocally(position); // Call local deletion method
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Method to delete an image URL only from the local list
    private void deleteImageLocally(int position) {
        if (position >= 0 && position < imageUrls.size()) {
            imageUrls.remove(position);
            adapter.setImageUrls(imageUrls); // Update adapter with new list
            adapter.notifyDataSetChanged();

            Toast.makeText(ImageViewerActivity.this, "Image removed locally.", Toast.LENGTH_SHORT).show();
            Log.d("ImageViewerActivity", "Image removed locally at position: " + position);

            // If no images left after deletion, finish and return the empty list
            if (imageUrls.isEmpty()) {
                Toast.makeText(this, "All images deleted.", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putStringArrayListExtra("updatedImageUrls", new ArrayList<>(imageUrls));
                setResult(Activity.RESULT_OK, resultIntent);
                finish(); // Close the activity
            }
            // If images remain, ViewPager2 might automatically move to the next/previous item
            // No need to manually set the current item here
        } else {
            Toast.makeText(ImageViewerActivity.this, "Error deleting image locally.", Toast.LENGTH_SHORT).show();
             Log.e("ImageViewerActivity", "Attempted to delete image at invalid position: " + position);
        }
    }

    // Override onBackPressed to return the updated list when the user presses back
    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putStringArrayListExtra("updatedImageUrls", new ArrayList<>(imageUrls));
        setResult(Activity.RESULT_OK, resultIntent);
        super.onBackPressed();
    }
}
