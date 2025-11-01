package student.inti.foodtracking;

import android.app.DatePickerDialog;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogExerciseActivity extends AppCompatActivity {

    TextView tvTitle, tvExerciseName, tvCaloriesPerHour, tvTotalCaloriesBurned, btnDate;
    Spinner spWeightType;
    EditText etHours;
    Button btnSave;
    CardView btnBackBox;

    DatabaseReference userRef, exerciseRef;
    FirebaseUser currentUser;

    String exerciseName, selectedDate, selectedWeightType;
    double caloriesPerHour = 0;
    double totalCalories = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_exercise);

        // Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        exerciseRef = FirebaseDatabase.getInstance().getReference("Exercise");

        // UI
        tvTitle = findViewById(R.id.tvTitle);
        btnDate = findViewById(R.id.btnDate);
        tvExerciseName = findViewById(R.id.tvExerciseName);
        spWeightType = findViewById(R.id.spWeightType);
        tvCaloriesPerHour = findViewById(R.id.tvCaloriesPerHour);
        etHours = findViewById(R.id.etHours);
        tvTotalCaloriesBurned = findViewById(R.id.tvTotalCaloriesBurned);
        btnSave = findViewById(R.id.btnSave);
        btnBackBox = findViewById(R.id.btnBackBox);

        // Back button to Home
        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(LogExerciseActivity.this, HomeActivity.class));
            finish();
        });

        tvTitle.setText("Log Your Exercise");

        // Default date = today
        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        btnDate.setText(selectedDate);

        btnDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate = String.format(Locale.getDefault(),
                                "%02d-%02d-%d", dayOfMonth, month + 1, year);
                        btnDate.setText(selectedDate);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // Get exerciseName from Intent
        Intent intent = getIntent();
        if (intent != null) {
            exerciseName = intent.getStringExtra("exerciseName");
            if (exerciseName != null) {
                tvExerciseName.setText(exerciseName);
            }
        }

        // WeightType spinner
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"130lb (58.967 kg)", "155lb (70.306 kg)", "180lb (81.646 kg)", "205lb (92.986 kg)"});
        spWeightType.setAdapter(typeAdapter);

        // Fetch calories per hour based on weight type selection
        spWeightType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedWeightType = parent.getItemAtPosition(position).toString();
                String key = selectedWeightType.split(" ")[0]; // e.g. "130lb"

                // Fetch Calorie from Database
                exerciseRef.child(exerciseName).child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    try {
                                        caloriesPerHour = snapshot.getValue(Double.class);
                                    } catch (Exception e) {
                                        try {
                                            caloriesPerHour = Double.parseDouble(snapshot.getValue().toString());
                                        } catch (Exception ex) {
                                            caloriesPerHour = 0;
                                        }
                                    }
                                    tvCaloriesPerHour.setText("Calories per hour: " + caloriesPerHour);

                                    // Default to 1 hour if empty
                                    if (etHours.getText().toString().trim().isEmpty()) etHours.setText("1");

                                    // Update total calories burned
                                    updateTotalCalories();
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Update Calories when Hours Change
        etHours.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateTotalCalories(); }
        });

        // Save button
        btnSave.setOnClickListener(v -> saveExerciseLog());
    }

    // Calculate Total Calories Burned
    private void updateTotalCalories() {
        String hourStr = etHours.getText().toString().trim();
        double hours = hourStr.isEmpty() ? 0 : Double.parseDouble(hourStr);
        totalCalories = caloriesPerHour * hours;
        tvTotalCaloriesBurned.setText("Total Calories Burned: " + totalCalories);
    }

    // Save Exercise Log
    private void saveExerciseLog() {
        String hourStr = etHours.getText().toString().trim();

        // Input Validation
        if (exerciseName == null || exerciseName.isEmpty()) {
            Toast.makeText(this, "Select an exercise", Toast.LENGTH_SHORT).show();
            return;
        }
        if (caloriesPerHour == 0) {
            Toast.makeText(this, "Select type/weight", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hourStr.isEmpty()) {
            Toast.makeText(this, "Enter hours", Toast.LENGTH_SHORT).show();
            return;
        }

        double hours = Double.parseDouble(hourStr);
        double burned = caloriesPerHour * hours;
        DatabaseReference recordRef = userRef.child("ActivityRecord").child(selectedDate);

        // Step 1: Fetch daily min/max calories from user profile
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double minCalories = snapshot.child("dailyMinimumCalorie").getValue(Double.class);
                Double maxCalories = snapshot.child("dailyMaximumCalorie").getValue(Double.class);

                Map<String, Object> dailyData = new HashMap<>();
                if (minCalories != null) dailyData.put("dailyMinimumCalorie", minCalories);
                if (maxCalories != null) dailyData.put("dailyMaximumCalorie", maxCalories);
                dailyData.put("dailyCalorieBurned", ServerValue.increment(burned));

                // Step 2: Ensure daily node exists
                recordRef.updateChildren(dailyData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Step 3: Push exercise log under Exercises
                        Map<String, Object> exerciseData = new HashMap<>();
                        exerciseData.put("exerciseName", exerciseName);
                        exerciseData.put("weightType", selectedWeightType);
                        exerciseData.put("caloriesPerHour", caloriesPerHour);
                        exerciseData.put("hours", hours);
                        exerciseData.put("totalCalories", burned);

                        recordRef.child("Exercises").push().setValue(exerciseData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(LogExerciseActivity.this, "Exercise logged successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(LogExerciseActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    } else {
                        Toast.makeText(LogExerciseActivity.this, "Failed to initialize daily burned calories", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}