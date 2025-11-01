package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class EditExerciseActivity extends AppCompatActivity {

    TextView tvTitle, tvName, tvBaseCalories, tvTotalCalories;
    Spinner spWeightType;
    EditText etHours;
    Button btnSave;
    CardView btnBackBox;

    // Firebase references
    DatabaseReference userRef, exerciseRef;
    FirebaseUser currentUser;

    String id, selectedDate;
    String exerciseName, weightType;
    double hours, caloriesPerHour = 0, oldTotalCalories = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_exercise);

        // Initialize Firebase authentication and database references
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        exerciseRef = FirebaseDatabase.getInstance().getReference("Exercise");

        tvTitle = findViewById(R.id.tvTitle);
        tvName = findViewById(R.id.tvName);
        tvBaseCalories = findViewById(R.id.tvBaseCalories);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        spWeightType = findViewById(R.id.spWeightType);
        etHours = findViewById(R.id.etHours);
        btnSave = findViewById(R.id.btnSave);
        btnBackBox = findViewById(R.id.btnBackBox);

        // Back button to previous page
        btnBackBox.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Retrieve intent extras passed from previous activity
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        exerciseName = intent.getStringExtra("exerciseName");
        weightType = intent.getStringExtra("weightType");
        hours = intent.getDoubleExtra("hours", 0);
        oldTotalCalories = intent.getDoubleExtra("totalCalories", 0);
        caloriesPerHour = intent.getDoubleExtra("caloriesPerHour", -1);
        selectedDate = intent.getStringExtra("selectedDate");

        // Display exercise name
        tvName.setText(exerciseName);

        // Populate spinner with weight options
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"130lb", "155lb", "180lb", "205lb"});
        spWeightType.setAdapter(adapter);

        // Pre-select weight type if available
        if (weightType != null) {
            int pos = adapter.getPosition(weightType);
            if (pos >= 0) spWeightType.setSelection(pos);
        }

        // Set the current number of exercise hours
        etHours.setText(String.valueOf(hours));

        // Display base calories per hour if provided; otherwise, load from Firebase
        if (caloriesPerHour != -1) {
            tvBaseCalories.setText("Calories per hour: " + caloriesPerHour);
            updateCalories();
        } else {
            loadCaloriesPerHour(weightType);
        }

        // Update total calories when the user changes hours
        etHours.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCalories();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Reload calories per hour when the weight type changes
        spWeightType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                weightType = parent.getItemAtPosition(pos).toString();
                loadCaloriesPerHour(weightType);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Save changes when the Save button is clicked
        btnSave.setOnClickListener(v -> saveChanges());
    }

    // Load the calorie burn rate per hour from Firebase based on exercise name and weight type.
    private void loadCaloriesPerHour(String weightType) {
        if (exerciseName == null || weightType == null) return;

        exerciseRef.child(exerciseName).child(weightType)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            try {
                                caloriesPerHour = Double.parseDouble(snapshot.getValue().toString());
                                tvBaseCalories.setText("Calories per hour: " + caloriesPerHour);
                                updateCalories();
                            } catch (Exception e) {
                                Toast.makeText(EditExerciseActivity.this,
                                        "Error parsing calories", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    //Recalculate and update the total calories burned based on hours and calories per hour.
    private void updateCalories() {
        String hStr = etHours.getText().toString().trim();
        double h = 0;
        try { h = Double.parseDouble(hStr); } catch (Exception ignored) {}
        double total = caloriesPerHour * h;
        tvTotalCalories.setText("Total Calories: " + total);
    }

    // Save the updated exercise record and recalculate the total burned calories for the day.
    private void saveChanges() {
        String newWeightType = spWeightType.getSelectedItem().toString();
        String hourStr = etHours.getText().toString().trim();
        double newHours = 0;
        try { newHours = Double.parseDouble(hourStr); } catch (Exception ignored) {}
        double newTotalCalories = caloriesPerHour * newHours;

        // Prepare the update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("exerciseName", exerciseName);
        updates.put("weightType", newWeightType);
        updates.put("hours", newHours);
        updates.put("caloriesPerHour", caloriesPerHour);
        updates.put("totalCalories", newTotalCalories);

        // Reference to the specific exercise entry for the selected date
        DatabaseReference exerciseEntry = userRef.child("ActivityRecord")
                .child(selectedDate).child("Exercises").child(id);

        // Update exercise record and adjust daily calories
        exerciseEntry.updateChildren(updates)
                .addOnSuccessListener(unused -> adjustDailyCalories(oldTotalCalories, newTotalCalories));
    }

    // Adjust only the user's daily calorie burned total by calculating the difference between the old and new exercise calorie totals.
    private void adjustDailyCalories(double oldCalories, double newCalories) {
        DatabaseReference recordRef = userRef.child("ActivityRecord").child(selectedDate);
        recordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                double burned = 0;
                if (snapshot.child("dailyCalorieBurned").getValue() != null)
                    burned = Double.parseDouble(snapshot.child("dailyCalorieBurned").getValue().toString());

                // Compute the difference and update
                double diff = newCalories - oldCalories;
                double newBurned = Math.max(0, burned + diff);

                Map<String, Object> updates = new HashMap<>();
                updates.put("dailyCalorieBurned", newBurned);

                recordRef.updateChildren(updates)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(EditExerciseActivity.this, "Exercise updated", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}