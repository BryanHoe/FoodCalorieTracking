package student.inti.foodtracking;

import android.app.AlertDialog;
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

public class LogFoodActivity extends AppCompatActivity {

    TextView tvTitle, tvCategory, tvServing, tvCalories, tvTotalCalories, btnDate;
    Button btnFoodName, btnSave;
    EditText etPortion;
    Spinner spMeal;
    CardView btnBackBox;

    DatabaseReference userRef, foodRef;
    FirebaseUser currentUser;

    String category, foodName, serving;
    double calories;
    String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_food);

        // Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        foodRef = FirebaseDatabase.getInstance().getReference("Food");

        // Init UI
        tvTitle = findViewById(R.id.tvTitle);
        btnDate = findViewById(R.id.btnDate);
        tvCategory = findViewById(R.id.tvCategory);
        btnFoodName = findViewById(R.id.btnFoodName);
        tvServing = findViewById(R.id.tvServing);
        tvCalories = findViewById(R.id.tvCalories);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        etPortion = findViewById(R.id.etPortion);
        spMeal = findViewById(R.id.spMeal);
        btnSave = findViewById(R.id.btnSave);
        btnBackBox = findViewById(R.id.btnBackBox);

        // Back button to Home
        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(LogFoodActivity.this, HomeActivity.class));
            finish();
        });

        // Default date = today
        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        btnDate.setText(selectedDate);

        // Open Calender
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

        // Meal options
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Breakfast", "Lunch", "Dinner", "Extra"});
        spMeal.setAdapter(mealAdapter);

        // Get food info from intent
        Intent intent = getIntent();
        if (intent != null) {
            category = intent.getStringExtra("category");
            foodName = intent.getStringExtra("foodName");

            if (foodName != null && category != null) {
                foodRef.child(category).child(foodName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            serving = snapshot.child("serving").getValue(String.class);
                            Object calObj = snapshot.child("calories").getValue();
                            try {
                                calories = Double.parseDouble(calObj.toString());
                            } catch (Exception e) {
                                calories = 0;
                            }

                            tvCategory.setText("Category: " + category);
                            btnFoodName.setText(foodName);
                            tvServing.setText("Serving: " + (serving != null ? serving : "1"));
                            tvCalories.setText("Calories (per serving): " + calories);
                            etPortion.setText("1");
                            updateTotalCalories();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            } else if (category != null) {
                tvCategory.setText("Category: " + category);
                btnFoodName.setText("Select Food");
            }
        }

        btnFoodName.setOnClickListener(v -> showFoodSearchPopup());

        // Update Calories when Portion Change
        etPortion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateTotalCalories(); }
        });

        btnSave.setOnClickListener(v -> saveFoodLog());
    }

    // Food Search Popup
    private void showFoodSearchPopup() {
        if (category == null || category.isEmpty()) {
            Toast.makeText(this, "No category selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search the Food
        foodRef.child(category).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                List<String> foodList = new ArrayList<>();
                for (DataSnapshot foodSnap : snapshot.getChildren()) {
                    foodList.add(foodSnap.getKey());
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(LogFoodActivity.this);
                builder.setTitle("Search Food in " + category);

                LinearLayout layout = new LinearLayout(LogFoodActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                EditText searchBox = new EditText(LogFoodActivity.this);
                searchBox.setHint("Search food...");
                ListView listView = new ListView(LogFoodActivity.this);

                ArrayAdapter<String> adapter = new ArrayAdapter<>(LogFoodActivity.this,
                        android.R.layout.simple_list_item_1, foodList);
                listView.setAdapter(adapter);

                searchBox.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        adapter.getFilter().filter(s);
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });

                layout.addView(searchBox);
                layout.addView(listView);
                builder.setView(layout);

                AlertDialog dialog = builder.create();
                dialog.show();

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    foodName = adapter.getItem(position);
                    btnFoodName.setText(foodName);

                    DataSnapshot foodSnap = snapshot.child(foodName);
                    serving = foodSnap.child("serving").getValue(String.class);
                    Object calObj = foodSnap.child("calories").getValue();
                    try {
                        calories = Double.parseDouble(calObj.toString());
                    } catch (Exception e) {
                        calories = 0;
                    }

                    tvServing.setText("Serving: " + serving);
                    tvCalories.setText("Calories (per serving): " + calories);
                    etPortion.setText("1");
                    updateTotalCalories();

                    dialog.dismiss();
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Update Total Calories
    private void updateTotalCalories() {
        String portionStr = etPortion.getText().toString().trim();
        int portion = portionStr.isEmpty() ? 0 : Integer.parseInt(portionStr);
        double total = calories * portion;
        tvTotalCalories.setText("Total Calories: " + total);
    }

    // Save food log and initialize ActivityRecord if needed
    private void saveFoodLog() {
        String portionStr = etPortion.getText().toString().trim();
        if (foodName == null || foodName.isEmpty()) {
            Toast.makeText(this, "Select a food", Toast.LENGTH_SHORT).show();
            return;
        }
        if (portionStr.isEmpty()) {
            Toast.makeText(this, "Enter portion", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validation
        int portion;
        try {
            portion = Integer.parseInt(portionStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid portion number", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalCalories = calories * portion;
        String meal = spMeal.getSelectedItem().toString();
        DatabaseReference recordRef = userRef.child("ActivityRecord").child(selectedDate);

        // Step 1: Fetch daily min/max from the user profile
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double minCalories = snapshot.child("dailyMinimumCalorie").getValue(Double.class);
                Double maxCalories = snapshot.child("dailyMaximumCalorie").getValue(Double.class);

                Map<String, Object> dailyData = new HashMap<>();
                if (minCalories != null) dailyData.put("dailyMinimumCalorie", minCalories);
                if (maxCalories != null) dailyData.put("dailyMaximumCalorie", maxCalories);
                dailyData.put("dailyCalorieConsumed", ServerValue.increment(totalCalories));

                // Step 2: Ensure daily node exists and update calories
                recordRef.updateChildren(dailyData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Step 3: Push the food log under Foods
                        Map<String, Object> foodData = new HashMap<>();
                        foodData.put("foodName", foodName);
                        foodData.put("category", category);
                        foodData.put("serving", serving);
                        foodData.put("portion", portion);
                        foodData.put("meal", meal);
                        foodData.put("calories", totalCalories);

                        recordRef.child("Foods").push().setValue(foodData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(LogFoodActivity.this, "Food logged successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LogFoodActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(LogFoodActivity.this, "Failed to initialize daily calories", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}