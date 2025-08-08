package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.example.micycle.models.Location;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

public class AddEditLocationActivity extends AppCompatActivity {

    private EditText editTextLocationName;
    private Button buttonSaveLocation;
    private TextView addEditLocationTitle;

    private FirebaseFirestore db;

    private String locationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_location);

        db = FirebaseFirestore.getInstance();

        addEditLocationTitle = findViewById(R.id.addEditLocationTitle);
        editTextLocationName = findViewById(R.id.editTextLocationName);
        buttonSaveLocation = findViewById(R.id.buttonSaveLocation);

        if (getIntent().hasExtra("location_id")) {
            locationId = getIntent().getStringExtra("location_id");
            String locationName = getIntent().getStringExtra("location_name");

            addEditLocationTitle.setText("Edit Location");
            editTextLocationName.setText(locationName);
            buttonSaveLocation.setText("Update Location");
        } else {
            locationId = null;
            addEditLocationTitle.setText("Add New Location");
            buttonSaveLocation.setText("Save Location");
        }

        buttonSaveLocation.setOnClickListener(v -> {
            saveLocation();
        });
    }

    private void saveLocation() {
        String name = editTextLocationName.getText().toString().trim();

        if (name.isEmpty()) {
            editTextLocationName.setError("Location name is required");
            editTextLocationName.requestFocus();
            return;
        }

        if (locationId == null) {
            Location newLocation = new Location(null, name);

            db.collection("locations")
                    .add(newLocation)
                    .addOnSuccessListener(documentReference -> {
                        String generatedLocationId = documentReference.getId();
                        Log.d("AddEditLocation", "New location added with ID: " + generatedLocationId);

                        documentReference.update("locationId", generatedLocationId)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Location saved successfully!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error updating location with ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("AddEditLocation", "Error updating location with ID", e);
                                    setResult(RESULT_CANCELED);
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("AddEditLocation", "Error adding location", e);
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        } else {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);

            db.collection("locations").document(locationId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Location updated successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("AddEditLocation", "Error updating location", e);
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }
    }
}
