package student.inti.foodtracking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class FoodActivity extends AppCompatActivity {

    Button btnDate, btnFilter;
    LinearLayout containerRecords;
    CardView btnBackBox;

    DatabaseReference userRef;
    FirebaseUser currentUser;

    String selectedDate;
    String currentFilter = "None";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food);

        // Initialize UI components
        btnDate = findViewById(R.id.btnDate);
        btnFilter = findViewById(R.id.btnFilter);
        containerRecords = findViewById(R.id.containerRecords);
        btnBackBox = findViewById(R.id.btnBackBox);

        // Ensure user is logged in
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Reference to the logged-in user's data in Firebase
        userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid());

        // Navigate back to HomeActivity
        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(FoodActivity.this, HomeActivity.class));
            finish();
        });

        // Set current date by default
        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        btnDate.setText(selectedDate);

        // Open date picker
        btnDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate = String.format(Locale.getDefault(),
                                "%02d-%02d-%d", dayOfMonth, month + 1, year);
                        btnDate.setText(selectedDate);
                        loadRecords();
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // Open filter dialog
        btnFilter.setOnClickListener(v -> showFilterDialog());

        loadRecords();
    }

    private void showFilterDialog() {
        String[] filters = {"None", "By Category", "By Meal",
                "By Calorie (High → Low)", "By Food Only", "By Exercise Only"};

        new AlertDialog.Builder(this)
                .setTitle("Select Filter")
                .setSingleChoiceItems(filters, Arrays.asList(filters).indexOf(currentFilter),
                        (dialog, which) -> {
                            currentFilter = filters[which];
                            loadRecords();
                        })
                .setPositiveButton("Apply", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Load food and exercise records from Firebase for the selected date
    private void loadRecords() {
        containerRecords.removeAllViews();

        DatabaseReference recordRef = userRef.child("ActivityRecord").child(selectedDate);
        recordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                containerRecords.removeAllViews();

                // If no data for this date, show message
                if (!snapshot.exists()) {
                    showToast("No record found for " + selectedDate);
                    return;
                }

                List<Map<String, Object>> foods = new ArrayList<>();
                List<Map<String, Object>> exercises = new ArrayList<>();

                // If both food and exercise data are missing
                if (!snapshot.hasChild("Foods") && !snapshot.hasChild("Exercises")) {
                    showToast("No record found for " + selectedDate);
                    return;
                }

                // Retrieve Foods
                if (snapshot.hasChild("Foods")) {
                    for (DataSnapshot ds : snapshot.child("Foods").getChildren()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ds.getKey());
                        map.put("foodName", ds.child("foodName").getValue(String.class));
                        map.put("category", ds.child("category").getValue(String.class));
                        map.put("meal", ds.child("meal").getValue(String.class));
                        map.put("portion", String.valueOf(ds.child("portion").getValue()));

                        double calories = 0;
                        try {
                            Object c = ds.child("calories").getValue();
                            if (c != null) calories = Double.parseDouble(c.toString());
                        } catch (Exception ignored) {}
                        map.put("calories", calories);
                        foods.add(map);
                    }
                }

                // Retrieve Exercises
                if (snapshot.hasChild("Exercises")) {
                    for (DataSnapshot ds : snapshot.child("Exercises").getChildren()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ds.getKey());
                        map.put("exerciseName", ds.child("exerciseName").getValue(String.class));
                        map.put("weightType", ds.child("weightType").getValue(String.class));

                        double hours = 0, totalCalories = 0, caloriesPerHour = 0;
                        try {
                            Object h = ds.child("hours").getValue();
                            if (h != null) hours = Double.parseDouble(h.toString());
                        } catch (Exception ignored) {}
                        try {
                            Object t = ds.child("totalCalories").getValue();
                            if (t != null) totalCalories = Double.parseDouble(t.toString());
                        } catch (Exception ignored) {}
                        try {
                            Object cph = ds.child("caloriesPerHour").getValue();
                            if (cph != null) caloriesPerHour = Double.parseDouble(cph.toString());
                        } catch (Exception ignored) {}

                        map.put("hours", hours);
                        map.put("totalCalories", totalCalories);
                        map.put("caloriesPerHour", caloriesPerHour);
                        exercises.add(map);
                    }
                }

                // If still no records after reading
                if (foods.isEmpty() && exercises.isEmpty()) {
                    showToast("No record found for " + selectedDate);
                    return;
                }

                // Apply filters
                if (currentFilter.equals("By Calorie (High → Low)")) {
                    List<Map<String, Object>> combined = new ArrayList<>();
                    combined.addAll(foods);
                    combined.addAll(exercises);
                    combined.sort((a, b) -> {
                        double ca = a.containsKey("calories")
                                ? (double) a.get("calories")
                                : (double) a.get("totalCalories");
                        double cb = b.containsKey("calories")
                                ? (double) b.get("calories")
                                : (double) b.get("totalCalories");
                        return Double.compare(cb, ca);
                    });

                    for (Map<String, Object> item : combined) {
                        if (item.containsKey("foodName")) {
                            addFoodBox((String) item.get("id"),
                                    (String) item.get("foodName"),
                                    (String) item.get("category"),
                                    (String) item.get("meal"),
                                    (String) item.get("portion"),
                                    (double) item.get("calories"));
                        } else {
                            addExerciseBox((String) item.get("id"),
                                    (String) item.get("exerciseName"),
                                    (String) item.get("weightType"),
                                    (double) item.get("hours"),
                                    (double) item.get("totalCalories"),
                                    (double) item.get("caloriesPerHour"));
                        }
                    }
                } else if (currentFilter.equals("By Food Only")) {
                    for (Map<String, Object> item : foods) {
                        addFoodBox((String) item.get("id"),
                                (String) item.get("foodName"),
                                (String) item.get("category"),
                                (String) item.get("meal"),
                                (String) item.get("portion"),
                                (double) item.get("calories"));
                    }
                } else if (currentFilter.equals("By Exercise Only")) {
                    for (Map<String, Object> item : exercises) {
                        addExerciseBox((String) item.get("id"),
                                (String) item.get("exerciseName"),
                                (String) item.get("weightType"),
                                (double) item.get("hours"),
                                (double) item.get("totalCalories"),
                                (double) item.get("caloriesPerHour"));
                    }
                } else {
                    for (Map<String, Object> item : foods) {
                        addFoodBox((String) item.get("id"),
                                (String) item.get("foodName"),
                                (String) item.get("category"),
                                (String) item.get("meal"),
                                (String) item.get("portion"),
                                (double) item.get("calories"));
                    }
                    for (Map<String, Object> item : exercises) {
                        addExerciseBox((String) item.get("id"),
                                (String) item.get("exerciseName"),
                                (String) item.get("weightType"),
                                (double) item.get("hours"),
                                (double) item.get("totalCalories"),
                                (double) item.get("caloriesPerHour"));
                    }
                }

                // If no views were added to container
                if (containerRecords.getChildCount() == 0) {
                    showToast("No record found for " + selectedDate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Error: " + error.getMessage());
            }
        });
    }

    // Show message using Toast instead of TextView
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Add visual box for a food record
    private void addFoodBox(String id, String foodName,
                            String category, String meal,
                            String portion, double calories) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 32, 32, 32);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFE5E5);
        bg.setCornerRadius(40);
        box.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 32);
        box.setLayoutParams(params);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP | Gravity.START);

        TextView tvName = new TextView(this);
        tvName.setText(foodName);
        tvName.setTextSize(18);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));
        header.addView(tvName);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.TOP | Gravity.END);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("Edit");
        btnEdit.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        btnEdit.setPadding(20, 0, 20, 0);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(FoodActivity.this, EditFoodActivity.class);
            intent.putExtra("id", id);
            intent.putExtra("foodName", foodName);
            intent.putExtra("category", category);
            intent.putExtra("meal", meal);
            intent.putExtra("portion", portion);
            intent.putExtra("calories", calories);
            intent.putExtra("selectedDate", selectedDate);
            startActivity(intent);
        });

        TextView btnDelete = new TextView(this);
        btnDelete.setText("Delete");
        btnDelete.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(FoodActivity.this)
                    .setTitle("Delete")
                    .setMessage("Delete " + foodName + "?")
                    .setPositiveButton("Yes", (d, w) -> {
                        DatabaseReference recordRef = userRef.child("ActivityRecord").child(selectedDate);
                        recordRef.child("Foods").child(id).removeValue()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(FoodActivity.this, "Food Deleted", Toast.LENGTH_SHORT).show();
                                    adjustCaloriesAfterFoodDelete(recordRef, calories);
                                });
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        actions.addView(btnEdit);
        actions.addView(btnDelete);
        header.addView(actions);
        box.addView(header);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(category + " | " + meal + " | " + portion + " servings");
        tvDetails.setPadding(0, 12, 0, 0);
        box.addView(tvDetails);

        TextView tvCal = new TextView(this);
        SpannableString calText = new SpannableString("Calories Consumed: " + calories);
        calText.setSpan(new StyleSpan(Typeface.BOLD), "Calories Consumed: ".length(), calText.length(), 0);
        tvCal.setText(calText);
        tvCal.setPadding(0, 12, 0, 0);
        box.addView(tvCal);

        containerRecords.addView(box);
    }

    // Add visual box for an exercise record
    private void addExerciseBox(String id, String exerciseName,
                                String weightType, double hours,
                                double totalCalories, double caloriesPerHour) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 32, 32, 32);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFE5FFE5);
        bg.setCornerRadius(40);
        box.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 32);
        box.setLayoutParams(params);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP | Gravity.START);

        TextView tvName = new TextView(this);
        tvName.setText(exerciseName);
        tvName.setTextSize(18);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));
        header.addView(tvName);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.TOP | Gravity.END);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("Edit");
        btnEdit.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        btnEdit.setPadding(20, 0, 20, 0);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(FoodActivity.this, EditExerciseActivity.class);
            intent.putExtra("id", id);
            intent.putExtra("exerciseName", exerciseName);
            intent.putExtra("weightType", weightType);
            intent.putExtra("hours", hours);
            intent.putExtra("caloriesPerHour", caloriesPerHour);
            intent.putExtra("totalCalories", totalCalories);
            intent.putExtra("selectedDate", selectedDate);
            startActivity(intent);
        });

        TextView btnDelete = new TextView(this);
        btnDelete.setText("Delete");
        btnDelete.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(FoodActivity.this)
                    .setTitle("Delete")
                    .setMessage("Delete " + exerciseName + "?")
                    .setPositiveButton("Yes", (d, w) -> {
                        DatabaseReference recordRef = userRef.child("ActivityRecord").child(selectedDate);
                        recordRef.child("Exercises").child(id).removeValue()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(FoodActivity.this, "Exercise Deleted", Toast.LENGTH_SHORT).show();
                                    adjustCaloriesAfterExerciseDelete(recordRef, totalCalories);
                                });
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        actions.addView(btnEdit);
        actions.addView(btnDelete);
        header.addView(actions);
        box.addView(header);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(weightType + " | " + hours + " hours");
        tvDetails.setPadding(0, 12, 0, 0);
        box.addView(tvDetails);

        TextView tvCal = new TextView(this);
        SpannableString calText = new SpannableString("Calories Burned: " + totalCalories);
        calText.setSpan(new StyleSpan(Typeface.BOLD), "Calories Burned: ".length(), calText.length(), 0);
        tvCal.setText(calText);
        tvCal.setPadding(0, 12, 0, 0);
        box.addView(tvCal);

        containerRecords.addView(box);
    }

    // Adjust calorie values after deleting food
    private void adjustCaloriesAfterFoodDelete(DatabaseReference recordRef, double calories) {
        recordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double consumed = snapshot.child("dailyCalorieConsumed").getValue(Double.class) != null
                        ? snapshot.child("dailyCalorieConsumed").getValue(Double.class) : 0;
                recordRef.child("dailyCalorieConsumed").setValue(Math.max(0, consumed - calories));
                loadRecords();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Adjust calorie values after deleting exercise
    private void adjustCaloriesAfterExerciseDelete(DatabaseReference recordRef, double calories) {
        recordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double burned = snapshot.child("dailyCalorieBurned").getValue(Double.class) != null
                        ? snapshot.child("dailyCalorieBurned").getValue(Double.class) : 0;
                recordRef.child("dailyCalorieBurned").setValue(Math.max(0, burned - calories));
                loadRecords();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
