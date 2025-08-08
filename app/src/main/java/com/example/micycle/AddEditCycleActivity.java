package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.view.View; // Import View
import android.widget.Button; // Import Button
import android.widget.EditText; // Import EditText
import android.widget.TextView; // Import TextView
import android.widget.Toast; // Import Toast
import android.util.Log; // Import Log
import android.widget.Spinner; // Import Spinner
import android.widget.ArrayAdapter; // Import ArrayAdapter

import com.example.micycle.models.Cycle; // Import Cycle model
import com.example.micycle.models.Location; // Import Location model
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference; // Import DocumentReference
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot; // Import QueryDocumentSnapshot

import java.util.ArrayList; // Import ArrayList
import java.util.List; // Import List
import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.util.Base64; // For Base64 encoding

// Imports for Volley (add these manually if you use Volley)
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

// Add these imports for better error handling
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.toolbox.HttpHeaderParser;
import java.io.UnsupportedEncodingException;

import android.app.AlertDialog;
import android.app.Activity; // Import Activity

public class AddEditCycleActivity extends AppCompatActivity {

    private EditText modelEditText;
    private EditText colorEditText;
    private EditText descriptionEditText;
    private EditText pricePerHourEditText;
    private Spinner locationSpinner; // Added Spinner for location selection
    private Button selectImagesButton; // Placeholder for image selection
    private Button saveCycleButton;
    private TextView addEditCycleTitle;
    private Button viewImagesButton; // Declare the view images button

    private String cycleId = null; // To store cycle ID if editing
    private Cycle fetchedCycle; // Declare a member variable to store the fetched cycle data
    private List<String> localImageUrls = new ArrayList<>(); // Local list to manage image URLs

    private FirebaseFirestore db; // Declare Firestore instance
    private FirebaseAuth mAuth; // Declare FirebaseAuth instance

    // Declare the ActivityResultLauncher for picking multiple images
    private ActivityResultLauncher<Intent> pickImagesLauncher;
    // List to hold URIs of selected images (for new uploads)
    private List<Uri> selectedImageUris = new ArrayList<>();
    // List to hold ImgBB URLs *of newly uploaded images*
    private List<String> newlyUploadedImageUrls = new ArrayList<>();

    // ImgBB API Key - Replace with your actual API key
    private static final String IMGBB_API_KEY = "cbd30ff22d9cde8941f5bb8a2c43ec03";
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload";

    // Volley Request Queue
    private RequestQueue requestQueue; // <-- Declare RequestQueue

    // Keep track of pending uploads
    private int pendingUploadCount = 0; // <-- Add counter for pending uploads

    // Lists for location data
    private List<Location> locationList = new ArrayList<>(); // List to hold Location objects
    private List<String> locationNames = new ArrayList<>(); // List to hold location names for the Spinner
    private ArrayAdapter<String> locationAdapter; // Adapter for the location Spinner

    private static final int IMAGE_VIEWER_REQUEST_CODE = 101; // Request code for ImageViewerActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_cycle);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Volley Request Queue
        requestQueue = Volley.newRequestQueue(this); // <-- Initialize RequestQueue

        // Initialize the ActivityResultLauncher for picking images
        pickImagesLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        selectedImageUris.clear(); // Clear previous selections
                        newlyUploadedImageUrls.clear(); // Clear previous uploads

                        if (data.getClipData() != null) {
                            // Multiple images selected
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                selectedImageUris.add(imageUri);
                            }
                        } else if (data.getData() != null) {
                            // Single image selected
                            Uri imageUri = data.getData();
                            selectedImageUris.add(imageUri);
                        }

                        // TODO: Display selected image previews to the user

                        if (!selectedImageUris.isEmpty()) {
                            // Disable save button and show progress while uploading
                            saveCycleButton.setEnabled(false);
                            pendingUploadCount = selectedImageUris.size(); // Set the total count of uploads
                            Toast.makeText(this, selectedImageUris.size() + " image(s) selected. Uploading...", Toast.LENGTH_SHORT).show();

                            // Upload each selected image to ImgBB
                            for (Uri imageUri : selectedImageUris) {
                                uploadImageToImgBB(imageUri);
                            }
                        } else {
                             Toast.makeText(this, "No images selected.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                         Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Get references to the UI elements
        addEditCycleTitle = findViewById(R.id.addEditCycleTitle);
        modelEditText = findViewById(R.id.modelEditText);
        colorEditText = findViewById(R.id.colorEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        pricePerHourEditText = findViewById(R.id.pricePerHourEditText);
        locationSpinner = findViewById(R.id.locationSpinner); // Initialize location Spinner
        selectImagesButton = findViewById(R.id.selectImagesButton);
        saveCycleButton = findViewById(R.id.saveCycleButton);
        viewImagesButton = findViewById(R.id.viewImagesButton); // Initialize the view images button

        // Initialize the location adapter
        locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, locationNames);
        locationSpinner.setAdapter(locationAdapter);

        // Fetch locations from Firestore
        fetchLocations();

        // Check if we are in edit mode (passed via Intent)
        Intent intent = getIntent();
        if (intent.hasExtra("cycle_id")) {
            cycleId = intent.getStringExtra("cycle_id");
            addEditCycleTitle.setText("Edit Cycle");
            saveCycleButton.setText("Update Cycle");

            // If in edit mode, fetch existing cycle data
            fetchCycleData(cycleId);

            // Disable save button until data is fetched (and locations are loaded)
            saveCycleButton.setEnabled(false);

        } else {
            // No cycle ID means we are adding a new cycle
            addEditCycleTitle.setText("Add New Cycle");
            saveCycleButton.setText("Save Cycle");
            // Form fields will be empty for a new cycle
            // localImageUrls is already empty for a new cycle
        }

        // Set click listener for Select Images button
        selectImagesButton.setOnClickListener(v -> {
            Intent pickImagesIntent = new Intent(Intent.ACTION_GET_CONTENT);
            pickImagesIntent.setType("image/*");
            pickImagesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImagesLauncher.launch(pickImagesIntent);
        });

        // Set click listener for Save Cycle button
        saveCycleButton.setOnClickListener(v -> {
            // Before saving, check if there are pending uploads
            if (pendingUploadCount > 0) {
                Toast.makeText(AddEditCycleActivity.this, "Please wait for all images to upload.", Toast.LENGTH_SHORT).show();
                return; // Prevent saving until uploads are done
            }

            // **Added validation: Check if there is at least one image URL**
            if (localImageUrls.isEmpty()) {
                Toast.makeText(AddEditCycleActivity.this, "Please add at least one image for the cycle.", Toast.LENGTH_SHORT).show();
                return; // Prevent saving if no images are present
            }

            // Get data from EditText fields
            String model = modelEditText.getText().toString().trim();
            String color = colorEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            String priceStr = pricePerHourEditText.getText().toString().trim();

            // Get the selected location ID from the spinner
            String selectedLocationId = null;
            int selectedLocationPosition = locationSpinner.getSelectedItemPosition();
            if (selectedLocationPosition != Spinner.INVALID_POSITION && selectedLocationPosition < locationList.size()) {
                selectedLocationId = locationList.get(selectedLocationPosition).getLocationId();
            } else {
                 Toast.makeText(AddEditCycleActivity.this, "Please select a location.", Toast.LENGTH_SHORT).show();
                 return; // Prevent saving if no location is selected
            }

            // Basic validation
            if (model.isEmpty() || color.isEmpty() || description.isEmpty() || priceStr.isEmpty() || selectedLocationId == null) {
                Toast.makeText(AddEditCycleActivity.this, "Please fill in all fields and select a location.", Toast.LENGTH_SHORT).show();
                return;
            }

            double pricePerHour;
            try {
                pricePerHour = Double.parseDouble(priceStr);
                if (pricePerHour <= 0) {
                    Toast.makeText(AddEditCycleActivity.this, "Price per hour must be a positive number.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(AddEditCycleActivity.this, "Invalid price format.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the current logged-in user's ID
            String ownerId = null;
            if (mAuth.getCurrentUser() != null) {
                ownerId = mAuth.getCurrentUser().getUid();
            } else {
                // This should not happen if the user is properly authenticated to reach this screen
                Toast.makeText(AddEditCycleActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a map of data to save/update
            Map<String, Object> cycleData = new HashMap<>();
            cycleData.put("model", model);
            cycleData.put("color", color);
            cycleData.put("description", description);
            cycleData.put("pricePerHour", pricePerHour);
            cycleData.put("locationId", selectedLocationId);
            cycleData.put("imageUrls", localImageUrls); // Use the local list for saving

            if (cycleId == null) { // Adding a new cycle
                if (ownerId == null) { // Should not happen in add mode if authenticated
                    Toast.makeText(AddEditCycleActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                    return;
                }
                cycleData.put("ownerId", ownerId); // Set ownerId for a new cycle
                cycleData.put("availabilityStatus", "available"); // New cycles are available by default

                // Add new cycle to Firestore
                db.collection("cycles")
                    .add(cycleData)
                        .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddEditCycleActivity.this, "Cycle added successfully!", Toast.LENGTH_SHORT).show();
                        Log.d("AddEditCycleActivity", "DocumentSnapshot added with ID: " + documentReference.getId());
                        finish(); // Close activity after saving
                        })
                        .addOnFailureListener(e -> {
                        Toast.makeText(AddEditCycleActivity.this, "Error adding cycle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("AddEditCycleActivity", "Error adding document", e);
                        });

            } else { // Editing an existing cycle
                // Update the existing cycle document in Firestore
                db.collection("cycles").document(cycleId)
                    .update(cycleData) // Update with the map containing localImageUrls
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AddEditCycleActivity.this, "Cycle updated successfully!", Toast.LENGTH_SHORT).show();
                        Log.d("AddEditCycleActivity", "DocumentSnapshot updated with ID: " + cycleId);
                        finish(); // Close activity after updating
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AddEditCycleActivity.this, "Error updating cycle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("AddEditCycleActivity", "Error updating document", e);
                        });
            }
        });

        // Set click listener for View Images button
        viewImagesButton.setOnClickListener(v -> {
            if (!localImageUrls.isEmpty()) {
                Intent viewerIntent = new Intent(AddEditCycleActivity.this, ImageViewerActivity.class);
                viewerIntent.putStringArrayListExtra("imageUrls", new ArrayList<>(localImageUrls));
                viewerIntent.putExtra("cycleId", cycleId); // Pass cycle ID (useful for context in viewer, though not for deletion in viewer)
                startActivityForResult(viewerIntent, IMAGE_VIEWER_REQUEST_CODE); // Use startActivityForResult
            } else {
                Toast.makeText(AddEditCycleActivity.this, "No images to view for this cycle.", Toast.LENGTH_SHORT).show();
            }
        });

        // This activity will handle adding or editing a cycle.
    }

    // Handle the result from ImageViewerActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_VIEWER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.hasExtra("updatedImageUrls")) {
                localImageUrls = data.getStringArrayListExtra("updatedImageUrls");
                // You can now use this updated localImageUrls list when saving
                Log.d("AddEditCycleActivity", "Received updated image URLs from viewer. Size: " + localImageUrls.size());
                 Toast.makeText(this, "Image list updated locally.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to fetch existing cycle data for editing
    private void fetchCycleData(String cycleId) {
        db.collection("cycles").document(cycleId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                    fetchedCycle = documentSnapshot.toObject(Cycle.class);
                    if (fetchedCycle != null) {
                        fetchedCycle.setCycleId(documentSnapshot.getId()); // Ensure cycleId is set in the object
                        populateForm(fetchedCycle);
                        // Populate localImageUrls with fetched images
                        if (fetchedCycle.getImageUrls() != null) {
                            localImageUrls = new ArrayList<>(fetchedCycle.getImageUrls());
                            Log.d("AddEditCycleActivity", "Fetched existing images. Count: " + localImageUrls.size());
                        } else {
                            localImageUrls = new ArrayList<>();
                            Log.d("AddEditCycleActivity", "No existing images found.");
                        }

                        // Enable save button after data is fetched and form is populated
                        saveCycleButton.setEnabled(true);
                    } else {
                        Toast.makeText(this, "Error fetching cycle data.", Toast.LENGTH_SHORT).show();
                        Log.e("AddEditCycleActivity", "Fetched document but toObject returned null.");
                        finish(); // Close activity on error
                    }
                } else {
                    Toast.makeText(this, "Cycle not found.", Toast.LENGTH_SHORT).show();
                    Log.w("AddEditCycleActivity", "Cycle document with ID " + cycleId + " not found.");
                    finish(); // Close activity if cycle not found
                    }
                })
                .addOnFailureListener(e -> {
                Toast.makeText(this, "Error fetching cycle data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("AddEditCycleActivity", "Error fetching cycle document", e);
                finish(); // Close activity on error
            });
    }

    // Method to populate the form with fetched cycle data
    private void populateForm(Cycle cycle) {
        modelEditText.setText(cycle.getModel());
        colorEditText.setText(cycle.getColor());
        descriptionEditText.setText(cycle.getDescription());
        pricePerHourEditText.setText(String.valueOf(cycle.getPricePerHour()));

        // Select the correct location in the spinner
        if (cycle.getLocationId() != null && !locationList.isEmpty()) {
            for (int i = 0; i < locationList.size(); i++) {
                if (locationList.get(i).getLocationId().equals(cycle.getLocationId())) {
                    locationSpinner.setSelection(i);
                    break;
                }
            }
        }
        // Note: Image URLs are now managed in localImageUrls
    }

    // Method to upload an image to ImgBB
    private void uploadImageToImgBB(Uri imageUri) {
        // Convert Uri to Base64 String
        final String base64Image; // Declare as final

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] bytes = getBytes(inputStream);
            base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP); // Assign value
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show();
            decrementPendingUploadCount(); // Decrement even on failure
            return; // Exit the method if image processing fails
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, IMGBB_UPLOAD_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            String imageUrl = jsonResponse.getJSONObject("data").getString("url");
                            newlyUploadedImageUrls.add(imageUrl); // Add to temporary list
                            Log.d("ImgBB", "Upload successful: " + imageUrl);
                            // Add the newly uploaded URL to the local list
                            localImageUrls.add(imageUrl);
                            Toast.makeText(this, "Image uploaded.", Toast.LENGTH_SHORT).show();
                        } else {
                            String error = jsonResponse.getString("error");
                            Toast.makeText(this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                            Log.e("ImgBB", "Upload failed: " + error);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing upload response.", Toast.LENGTH_SHORT).show();
                        Log.e("ImgBB", "JSON parsing error", e);
                    }
                    decrementPendingUploadCount(); // Decrement after response
                },
                error -> {
                    Toast.makeText(this, "Image upload error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ImgBB", "Volley Error", error);
                    // Added handling for network response to get more details
                    NetworkResponse networkResponse = error.networkResponse;
                    if (networkResponse != null) {
                        Log.e("ImgBB", "Error Status Code: " + networkResponse.statusCode);
                        try {
                            String responseBody = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                            Log.e("ImgBB", "Error Response Body: " + responseBody);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    decrementPendingUploadCount(); // Decrement after error
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("key", IMGBB_API_KEY);
                params.put("image", base64Image);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    // Helper method to get bytes from a Uri
    private byte[] getBytes(InputStream inputStream) throws IOException {
        byte[] bytesResult = null;
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        try {
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            bytesResult = byteBuffer.toByteArray();
        } finally {
            // Close the InputStream and ByteArrayOutputStream
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                byteBuffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytesResult;
    }

    // Method to decrement the pending upload count and enable save button when done
    private void decrementPendingUploadCount() {
        pendingUploadCount--;
        if (pendingUploadCount <= 0) {
            pendingUploadCount = 0; // Ensure it doesn't go below zero
            saveCycleButton.setEnabled(true); // Enable save button
            if (!newlyUploadedImageUrls.isEmpty()) {
                 // Optional: Show a toast indicating new images are ready to be saved
                 // Toast.makeText(this, newlyUploadedImageUrls.size() + " new image(s) uploaded and added locally. Click Save to confirm.", Toast.LENGTH_LONG).show();
                 newlyUploadedImageUrls.clear(); // Clear the temporary list
            }
        }
    }

    // Method to fetch locations from Firestore and populate the spinner
    private void fetchLocations() {
        db.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        locationList.clear();
                        locationNames.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Location location = document.toObject(Location.class);
                            location.setLocationId(document.getId()); // Assuming Location model has setLocationId
                            locationList.add(location);
                            // Assuming Location model has a getName() method or similar for display
                            locationNames.add(location.getName());
                        }
                        locationAdapter.notifyDataSetChanged();

                        // If in edit mode and locations are loaded, try to pre-select location
                        if (cycleId != null && fetchedCycle != null) {
                            populateForm(fetchedCycle);
                        }

                    } else {
                        Toast.makeText(AddEditCycleActivity.this, "Error loading locations: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.w("AddEditCycleActivity", "Error getting documents.", task.getException());
                    }
                });
    }
}