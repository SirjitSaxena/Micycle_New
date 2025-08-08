package com.example.micycle;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog; // Import DatePickerDialog
import android.app.TimePickerDialog; // Import TimePickerDialog
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date; // Import Date
import java.util.concurrent.TimeUnit; // Import TimeUnit

import com.google.firebase.firestore.FirebaseFirestore; // Import FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.firestore.DocumentReference; // Import DocumentReference
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.google.firebase.firestore.WriteBatch; // Import WriteBatch
import com.example.micycle.models.Booking; // Import Booking model
import com.example.micycle.models.Cycle; // Import Cycle model (already imported, but good to double-check)
import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map
import com.google.firebase.firestore.FieldValue; // Import FieldValue

public class BookingActivity extends AppCompatActivity {

    private String cycleId;
    private double pricePerHour; // Keep for initial calculation, but use fetched cycle's price for final cost
    private Cycle fetchedCycle; // To store the fetched cycle data

    private EditText startDateEditText;
    private EditText startTimeEditText;
    private EditText endDateEditText;
    private EditText endTimeEditText;
    private TextView totalCostTextView;
    private Button confirmBookingButton;

    // Variables to store selected dates and times
    private Calendar startDateTime;
    private Calendar endDateTime;

    private FirebaseFirestore db; // Declare FirebaseFirestore instance
    private FirebaseAuth mAuth; // Declare FirebaseAuth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        // This activity will handle the booking process.

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Retrieve cycle ID from the intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("cycle_id")) {
            cycleId = intent.getStringExtra("cycle_id");
            // pricePerHour = intent.getDoubleExtra("price_per_hour", 0.0); // We will fetch this with cycle details

            if (cycleId != null) {
                // Fetch cycle details immediately to get the latest availability and owner ID
                fetchCycleDetails(cycleId);
            } else {
                Toast.makeText(this, "Cycle ID is missing.", Toast.LENGTH_SHORT).show();
                finish(); // Close activity if no ID is passed
                return; // Exit onCreate
            }
        } else {
            // Handle case where no intent or data is provided
            Toast.makeText(this, "Booking information missing.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
            return; // Exit onCreate
        }

        // Get references to the UI elements
        startDateEditText = findViewById(R.id.startDateEditText);
        startTimeEditText = findViewById(R.id.startTimeEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        endTimeEditText = findViewById(R.id.endTimeEditText);
        totalCostTextView = findViewById(R.id.totalCostTextView);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);

        // Disable the confirm booking button until cycle data is fetched and validated
        confirmBookingButton.setEnabled(false);
        totalCostTextView.setText("₹0.00"); // Set initial cost to 0 with Rupee symbol

        // Initialize Calendar instances to current time initially
        startDateTime = Calendar.getInstance();
        // Clear seconds and milliseconds for precise comparison
        startDateTime.set(Calendar.SECOND, 0);
        startDateTime.set(Calendar.MILLISECOND, 0);

        endDateTime = Calendar.getInstance();
         // Clear seconds and milliseconds for precise comparison
        endDateTime.set(Calendar.SECOND, 0);
        endDateTime.set(Calendar.MILLISECOND, 0);

         // Set initial date and time in EditTexts (optional, but good for user)
        updateDateEditText(startDateEditText, startDateTime);
        updateTimeEditText(startTimeEditText, startDateTime);

         // Set initial end time to 1 hour after start time
        endDateTime.setTime(startDateTime.getTime()); // Start with the same date and time
        endDateTime.add(Calendar.HOUR_OF_DAY, 1); // Add 1 hour

        updateDateEditText(endDateEditText, endDateTime);
        updateTimeEditText(endTimeEditText, endDateTime);

        // Set click listeners for Date and Time EditTexts
        startDateEditText.setOnClickListener(v -> showDatePickerDialog(true));
        startTimeEditText.setOnClickListener(v -> showTimePickerDialog(true));
        endDateEditText.setOnClickListener(v -> showDatePickerDialog(false));
        endTimeEditText.setOnClickListener(v -> showTimePickerDialog(false));

        // Set click listener for Confirm Booking button
        confirmBookingButton.setOnClickListener(v -> {
            // 1. Validate selected dates and times (end time after start time, etc.)
            if (isValidDateTimeRange()) { // Use the consolidated validation method
            // 2. Calculate final total cost based on exact duration (already done in calculateTotalCost).
                // The cost is already displayed in totalCostTextView and based on valid dates.
            // 3. Initiate booking transaction (e.g., send data to Firebase).
                performBooking(); // Call the method to perform the Firestore transaction
            } else {
                Toast.makeText(this, "Please select valid start and end times. Ensure start time is not more than 5 minutes in the past and end time is at least 1 hour after start time.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Method to show Date Picker Dialog
    private void showDatePickerDialog(boolean isStartDate) {
        Calendar currentCalendar = isStartDate ? startDateTime : endDateTime;
        Calendar initialCalendar = (Calendar) currentCalendar.clone(); // Store initial state

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    currentCalendar.set(year, month, dayOfMonth);
                    currentCalendar.set(Calendar.SECOND, 0); // Clear seconds for precise comparison
                    currentCalendar.set(Calendar.MILLISECOND, 0); // Clear milliseconds

                     // Update the corresponding Calendar instance after date selection
                     if (isStartDate) {
                        startDateTime.setTime(currentCalendar.getTime());
                         updateDateEditText(startDateEditText, startDateTime); // Update EditText
                     } else {
                        endDateTime.setTime(currentCalendar.getTime());
                        updateDateEditText(endDateEditText, endDateTime); // Update EditText
                     }


                    // Check validity after setting the date
                    if (!isValidDateTimeRange()) {
                        // If invalid, revert to the initial valid date/time and show a toast
                         Toast.makeText(this, "Invalid date selection. Please ensure start time is not more than 5 minutes in the past and end time is at least 1 hour after start time.", Toast.LENGTH_LONG).show();
                    if (isStartDate) {
                             startDateTime.setTime(initialCalendar.getTime()); // Revert start time
                              updateDateEditText(startDateEditText, startDateTime); // Update EditText as well
                             updateTimeEditText(startTimeEditText, startDateTime); // Update time EditText as well
                         } else {
                             endDateTime.setTime(initialCalendar.getTime()); // Revert end time
                             updateDateEditText(endDateEditText, endDateTime); // Update EditText as well
                             updateTimeEditText(endTimeEditText, endDateTime); // Update time EditText as well
                         }

                    } else {
                         // If valid, EditTexts are already updated above
                    }

                    calculateTotalCost(); // Recalculate cost based on the current start/end times
                },
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    // Method to show Time Picker Dialog
    private void showTimePickerDialog(boolean isStartTime) {
        Calendar currentCalendar = isStartTime ? startDateTime : endDateTime;
        Calendar initialCalendar = (Calendar) currentCalendar.clone(); // Store initial state

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    currentCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    currentCalendar.set(Calendar.MINUTE, minute);
                    currentCalendar.set(Calendar.SECOND, 0); // Clear seconds for precise comparison
                    currentCalendar.set(Calendar.MILLISECOND, 0); // Clear milliseconds

                     // Update the corresponding Calendar instance after time selection
                     if (isStartTime) {
                        startDateTime.setTime(currentCalendar.getTime());
                         updateTimeEditText(startTimeEditText, startDateTime); // Update EditText
                     } else {
                        endDateTime.setTime(currentCalendar.getTime());
                        updateTimeEditText(endTimeEditText, endDateTime); // Update EditText
                     }


                    // Check validity after setting the time
                    if (!isValidDateTimeRange()) {
                        // If invalid, revert to the initial valid date/time and show a toast
                         Toast.makeText(this, "Invalid time selection. Please ensure start time is not more than 5 minutes in the past and end time is at least 1 hour after start time.", Toast.LENGTH_LONG).show();
                         if (isStartTime) {
                             startDateTime.setTime(initialCalendar.getTime()); // Revert start time
                              updateDateEditText(startDateEditText, startDateTime); // Update date EditText as well
                             updateTimeEditText(startTimeEditText, startDateTime); // Update time EditText as well
                         } else {
                             endDateTime.setTime(initialCalendar.getTime()); // Revert end time
                             updateDateEditText(endDateEditText, endDateTime); // Update date EditText as well
                             updateTimeEditText(endTimeEditText, endDateTime); // Update time EditText as well
                         }

                    } else {
                         // If valid, EditTexts are already updated above
                    }

                    calculateTotalCost(); // Recalculate cost based on the current start/end times
                },
                currentCalendar.get(Calendar.HOUR_OF_DAY),
                currentCalendar.get(Calendar.MINUTE),
                false); // false for 12-hour format, true for 24-hour format
        timePickerDialog.show();
    }

    // Method to update Date EditText field
    private void updateDateEditText(EditText editText, Calendar calendar) {
        String format = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);

        String formattedDate = sdf.format(calendar.getTime());

        editText.setText(formattedDate);
    }

    // Method to update Time EditText field
    private void updateTimeEditText(EditText editText, Calendar calendar) {
        String format = "hh:mm a"; // Example: 03:30 PM
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);

        String formattedTime = sdf.format(calendar.getTime());

        editText.setText(formattedTime);
    }

     // Helper method to format Calendar to a readable string for Toast
    private String formatDateTime(Calendar calendar) {
        String format = "MM/dd/yyyy hh:mm a";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        return sdf.format(calendar.getTime());
    }

    // Method to calculate the total cost
    private void calculateTotalCost() {
        // Use the fetched cycle's pricePerHour for calculation
        if (fetchedCycle != null && isValidDateTimeRange()) { // Use the new validation method
            long startTimeMillis = startDateTime.getTimeInMillis();
            long endTimeMillis = endDateTime.getTimeInMillis();

            long diffInMillis = endTimeMillis - startTimeMillis;

            // Calculate hours, rounding up to the nearest hour if there's any duration
            double hours = (double) diffInMillis / (1000 * 60 * 60);
            if (hours > 0 && hours < 1) { // Handle durations less than 1 hour, charge for a full hour
                hours = 1.0;
            } else { // For durations 1 hour or more, round up to the nearest hour
                hours = Math.ceil(hours);
            }

            double totalCost = hours * fetchedCycle.getPricePerHour(); // Use price from fetched cycle

            totalCostTextView.setText("₹" + String.format(Locale.US, "%.2f", totalCost)); // Use Rupee symbol
        } else {
            // If dates are not valid (e.g., end time before start time), reset cost
            totalCostTextView.setText("₹0.00"); // Use Rupee symbol
        }
    }

    // Method to fetch cycle details from Firestore
    private void fetchCycleDetails(String cycleId) {
        db.collection("cycles").document(cycleId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        fetchedCycle = documentSnapshot.toObject(Cycle.class);
                        if (fetchedCycle != null) {
                            // Explicitly set the cycleId from the document ID
                            fetchedCycle.setCycleId(documentSnapshot.getId());

                            // Check if the cycle is available
                            if ("available".equalsIgnoreCase(fetchedCycle.getAvailabilityStatus())) {
                                // Cycle is available, enable booking functionality
                                confirmBookingButton.setEnabled(true);
                                calculateTotalCost(); // Calculate initial cost now that price is available
                            } else {
                                // Cycle is not available
                                Toast.makeText(this, "This cycle is currently not available for booking.", Toast.LENGTH_LONG).show();
                                confirmBookingButton.setEnabled(false);
                            }
                        } else {
                             Toast.makeText(this, "Error fetching cycle data.", Toast.LENGTH_SHORT).show();
                             finish(); // Close if data is null
                        }
                    } else {
                        // Cycle document doesn't exist
                        Toast.makeText(this, "Cycle not found.", Toast.LENGTH_SHORT).show();
                        finish(); // Close if document not found
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching document
                    Toast.makeText(this, "Error loading cycle details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish(); // Close on error
                });
    }

    // Method to perform the booking transaction (save booking and update cycle status)
    private void performBooking() {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // **Prevent booking of the owner's own cycle**
        if (currentUserId != null && fetchedCycle != null && currentUserId.equals(fetchedCycle.getOwnerId())) {
            Toast.makeText(this, "You cannot book your own cycle.", Toast.LENGTH_SHORT).show();
            return; // Stop the booking process
        }

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            // Maybe redirect to login screen
            return;
        }

        if (fetchedCycle == null || fetchedCycle.getCycleId() == null) {
            Toast.makeText(this, "Cycle information not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new booking object
        Booking newBooking = new Booking();
        // Firebase will generate the bookingId (document ID)
        newBooking.setCycleId(fetchedCycle.getCycleId());
        newBooking.setUserId(currentUserId);
        // Optional: Set ownerId from fetchedCycle if needed for easier queries
        newBooking.setOwnerId(fetchedCycle.getOwnerId());
        newBooking.setStartTime(startDateTime.getTime()); // Convert Calendar to Date
        newBooking.setEndTime(endDateTime.getTime()); // Convert Calendar to Date
        // Get total cost from the calculated value (remove currency symbol for saving)
        String totalCostString = totalCostTextView.getText().toString().replace("₹", "").replace("$", ""); // Remove currency symbols
        try {
            double totalCost = Double.parseDouble(totalCostString);
            newBooking.setTotalCost(totalCost);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Error calculating cost.", Toast.LENGTH_SHORT).show();
            return;
        }
        newBooking.setBookingStatus("active"); // Set initial status
        newBooking.setPaymentStatus("pending"); // Assuming payment integration is separate
        // Set createdAt and updatedAt using server timestamps
        // newBooking.setCreatedAt(new Date()); // Removed manual setting
        // newBooking.setUpdatedAt(new Date()); // Removed manual setting

        // Get references for the new booking document and the cycle document
        DocumentReference bookingRef = db.collection("bookings").document(); // Let Firestore generate the ID
        DocumentReference cycleRef = db.collection("cycles").document(fetchedCycle.getCycleId());

        // Use a WriteBatch to perform atomic writes
        WriteBatch batch = db.batch();

        // Add the new booking to the batch
        // Use a Map to include server timestamps for creation and update
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("cycleId", newBooking.getCycleId());
        bookingData.put("userId", newBooking.getUserId());
        bookingData.put("ownerId", newBooking.getOwnerId());
        bookingData.put("startTime", newBooking.getStartTime());
        bookingData.put("endTime", newBooking.getEndTime());
        bookingData.put("totalCost", newBooking.getTotalCost());
        bookingData.put("bookingStatus", newBooking.getBookingStatus());
        bookingData.put("paymentStatus", newBooking.getPaymentStatus());
        bookingData.put("createdAt", FieldValue.serverTimestamp()); // Set server timestamp for creation
        bookingData.put("updatedAt", FieldValue.serverTimestamp()); // Set server timestamp for initial update

        batch.set(bookingRef, bookingData); // Use the Map for setting

        // Update the cycle status and bookedUntilTimestamp in the batch
        // Need to update only specific fields without overwriting the entire document
        Map<String, Object> cycleUpdates = new HashMap<>();
        cycleUpdates.put("availabilityStatus", "booked");
        cycleUpdates.put("bookedUntilTimestamp", endDateTime.getTime()); // Save end time for availability check

        batch.update(cycleRef, cycleUpdates);

        // Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Transaction successful!
                    Toast.makeText(this, "Booking confirmed successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate to Transaction History after successful booking
                    Intent intent = new Intent(BookingActivity.this, TransactionHistoryActivity.class);
                    // Optional: Add flags to clear the activity stack depending on desired navigation flow
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish(); // Close BookingActivity
                })
                .addOnFailureListener(e -> {
                    // Handle transaction failure
                    Toast.makeText(this, "Error confirming booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Keep isValidDateTimeRange for calculateTotalCost and final booking confirmation
    private boolean isValidDateTimeRange() {
        if (startDateTime == null || endDateTime == null) {
            return false;
        }

        Calendar fiveMinutesAgo = Calendar.getInstance();
        fiveMinutesAgo.add(Calendar.MINUTE, -5);
        fiveMinutesAgo.set(Calendar.SECOND, 0); // Clear seconds for precise comparison
        fiveMinutesAgo.set(Calendar.MILLISECOND, 0); // Clear milliseconds


        // Rule 3: Start time cannot be more than 5 minutes in the past
        // Compare using milliseconds for precision
        if (startDateTime.getTimeInMillis() < fiveMinutesAgo.getTimeInMillis()) {
            return false;
        }

        // Rule 4: End time must be at least 1 hour after start time
        Calendar minimumEndTime = (Calendar) startDateTime.clone();
        minimumEndTime.add(Calendar.HOUR_OF_DAY, 1);
         // No need to clear seconds/milliseconds here if startDateTime already has them cleared

        // Check if endDateTime is strictly before minimumEndTime
         // Compare using milliseconds for precision
        if (endDateTime.getTimeInMillis() < minimumEndTime.getTimeInMillis()) {
            return false;
        }

        return true; // All checks passed
    }
}