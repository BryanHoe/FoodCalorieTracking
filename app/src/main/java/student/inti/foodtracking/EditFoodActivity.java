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

import java.util.*;

public class EditFoodActivity extends AppCompatActivity {

    TextView tvTitle, tvName, tvBaseCalories, tvTotalCalories;
    Spinner spMeal;
    EditText etPortion;
    Button btnSave;
    CardView btnBackBox;

    // Firebase references
    DatabaseReference userRef, foodRef;
    FirebaseUser currentUser;

    String id, selectedDate;
    String foodName, category, meal, portion;
    double caloriesPerServing, oldCalories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_food);

        // Initialize Firebase authentication
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        foodRef = FirebaseDatabase.getInstance().getReference("Food");

        // Initialize UI components
        tvTitle = findViewById(R.id.tvTitle);
        tvName = findViewById(R.id.tvName);
        tvBaseCalories = findViewById(R.id.tvBaseCalories);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        spMeal = findViewById(R.id.spMeal);
        etPortion = findViewById(R.id.etPortion);
        btnSave = findViewById(R.id.btnSave);
        btnBackBox = findViewById(R.id.btnBackBox);

        // Back button
        btnBackBox.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Get data passed from the previous activity
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        foodName = intent.getStringExtra("foodName");
        category = intent.getStringExtra("category");
        meal = intent.getStringExtra("meal");
        portion = intent.getStringExtra("portion");
        oldCalories = intent.getDoubleExtra("calories", 0);
        selectedDate = intent.getStringExtra("selectedDate");

        tvName.setText(foodName);

        // Set up meal dropdown options
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Breakfast", "Lunch", "Dinner", "Extra"});
        spMeal.setAdapter(mealAdapter);

        // Set the spinner to the correct meal
        if (meal != null) {
            int pos = mealAdapter.getPosition(meal);
            if (pos >= 0) spMeal.setSelection(pos);
        }

        etPortion.setText(portion);

        // Retrieve calories per serving from Firebase
        foodRef.child(category).child(foodName).child("calories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            try {
                                caloriesPerServing = Double.parseDouble(snapshot.getValue().toString());
                                tvBaseCalories.setText("Calories per serving: " + caloriesPerServing);
                                updateCalories();
                            } catch (Exception ignored) {}
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Update total calories when portion input changes
        etPortion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCalories();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Save button click handler
        btnSave.setOnClickListener(v -> saveChanges());
    }

    // Calculate total calories based on portion size
    private void updateCalories() {
        String portionStr = etPortion.getText().toString().trim();
        int p = 0;
        try { p = Integer.parseInt(portionStr); } catch (Exception ignored) {}
        double total = caloriesPerServing * p;
        tvTotalCalories.setText("Total Calories: " + total);
    }

    // Save updated food record to Firebase
    private void saveChanges() {
        String newMeal = spMeal.getSelectedItem().toString();
        String newPortion = etPortion.getText().toString().trim();
        int p = 0;
        try { p = Integer.parseInt(newPortion); } catch (Exception ignored) {}
        double newTotalCalories = caloriesPerServing * p;

        Map<String, Object> updates = new HashMap<>();
        updates.put("meal", newMeal);
        updates.put("portion", newPortion);
        updates.put("calories", newTotalCalories);

        DatabaseReference recordRef = userRef.child("ActivityRecord").child(selectedDate);
        recordRef.child("Foods").child(id).updateChildren(updates)
                .addOnSuccessListener(unused -> adjustDailyCalories(recordRef, oldCalories, newTotalCalories));
    }

    // Adjust daily total calories after edit (only affects dailyCalorieConsumed)
    private void adjustDailyCalories(DatabaseReference recordRef, double oldCalories, double newCalories) {
        recordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                double consumed = snap.child("dailyCalorieConsumed").getValue(Double.class) != null ?
                        snap.child("dailyCalorieConsumed").getValue(Double.class) : 0;

                // Calculate new total based on difference
                double diff = newCalories - oldCalories;
                double newConsumed = Math.max(0, consumed + diff);

                Map<String, Object> updates = new HashMap<>();
                updates.put("dailyCalorieConsumed", newConsumed);

                // Update Firebase record
                recordRef.updateChildren(updates)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(EditFoodActivity.this, "Food updated", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}