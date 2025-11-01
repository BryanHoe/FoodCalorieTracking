package student.inti.foodtracking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    EditText etUsername, etBirth, etHeight, etWeight;
    Spinner spGender, spActivityLevel;
    CardView btnBackBox, btnSave;

    DatabaseReference userRef;
    String uid;

    final String[] genders = {"Male", "Female"};
    final String[] activityLevels = {
            "Select Activity Level",
            "Sedentary (little or no exercise)",
            "Lightly active (light exercise 1–3 days/week)",
            "Moderately active (moderate exercise 3–5 days/week)",
            "Very active (hard exercise 6–7 days/week)",
            "Extra active (very hard exercise or physical job)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Get current user ID from Firebase Authentication
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        // Initialize UI elements
        etUsername = findViewById(R.id.etUsername);
        etBirth = findViewById(R.id.etBirth);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        spGender = findViewById(R.id.spGender);
        spActivityLevel = findViewById(R.id.spActivityLevel);
        btnSave = findViewById(R.id.btnSave);
        btnBackBox = findViewById(R.id.btnBackBox);

        // Setup dropdowns for gender and activity level
        spGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders));
        spActivityLevel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, activityLevels));

        // Load existing user data from Firebase
        loadUserData();

        // Set listeners
        etBirth.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveDataWithRecalculation());
        btnBackBox.setOnClickListener(v -> finish());
    }

    // Load user data from Firebase Realtime Database
    private void loadUserData() {
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Populate basic fields
                etUsername.setText(snapshot.child("username").getValue(String.class));
                etBirth.setText(snapshot.child("birthDate").getValue(String.class));

                // Set height and weight
                Object heightObj = snapshot.child("height").getValue();
                Object weightObj = snapshot.child("weight").getValue();
                if (heightObj != null) etHeight.setText(heightObj.toString());
                if (weightObj != null) etWeight.setText(weightObj.toString());

                // Set gender spinner selection
                String gender = snapshot.child("gender").getValue(String.class);
                if (gender != null) {
                    for (int i = 0; i < genders.length; i++) {
                        if (genders[i].equalsIgnoreCase(gender)) {
                            spGender.setSelection(i);
                            break;
                        }
                    }
                }

                // Set activity level spinner selection
                String activityLevel = snapshot.child("activityLevel").getValue(String.class);
                if (activityLevel != null) {
                    for (int i = 0; i < activityLevels.length; i++) {
                        if (activityLevels[i].equalsIgnoreCase(activityLevel)) {
                            spActivityLevel.setSelection(i);
                            break;
                        }
                    }
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    //Display date picker dialog for selecting birth date
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, (month + 1), year);
                    etBirth.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    // Save updated profile data and recalculate calorie recommendations
    private void saveDataWithRecalculation() {
        String username = etUsername.getText().toString().trim();
        String birth = etBirth.getText().toString().trim();
        String gender = spGender.getSelectedItem().toString();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String activityLevel = spActivityLevel.getSelectedItem().toString();

        // Validate input
        if (username.isEmpty() || birth.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse numeric values
        double height = Double.parseDouble(heightStr);
        double weight = Double.parseDouble(weightStr);
        int age = calculateAge(birth);

        // Calculate calorie limits
        double maxCalories = calculateMaxCalories(gender, age);
        double minCalories = calculateMinCalories(gender, weight, height, age, activityLevel);

        // Update data in user root
        userRef.child("username").setValue(username);
        userRef.child("birthDate").setValue(birth);
        userRef.child("gender").setValue(gender);
        userRef.child("height").setValue(heightStr);
        userRef.child("weight").setValue(weightStr);
        userRef.child("activityLevel").setValue(activityLevel);
        userRef.child("age").setValue(age);
        userRef.child("dailyMaximumCalorie").setValue(maxCalories);
        userRef.child("dailyMinimumCalorie").setValue(minCalories);

        // Also update today's calorie record
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        DatabaseReference todayRef = userRef.child("ActivityRecord").child(today);
        todayRef.child("dailyMaximumCalorie").setValue(maxCalories);
        todayRef.child("dailyMinimumCalorie").setValue(minCalories);

        // Confirmation and redirect
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(EditProfileActivity.this, UserInformationActivity.class));
        finish();
    }

    // Calculate user's age from birth date
    private int calculateAge(String birthDate) {
        try {
            String[] parts = birthDate.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int year = Integer.parseInt(parts[2]);

            Calendar dob = Calendar.getInstance();
            dob.set(year, month, day);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            return 25; // Default fallback age
        }
    }

    // Determine maximum calorie limit based on gender and age
    private double calculateMaxCalories(String gender, int age) {
        if (gender.equalsIgnoreCase("Male")) {
            if (age >= 19 && age <= 60) return 3000;
            else if (age >= 61) return 2600;
            else return 2500;
        } else if (gender.equalsIgnoreCase("Female")) {
            if (age >= 19 && age <= 30) return 2400;
            else if (age >= 31 && age <= 60) return 2200;
            else if (age >= 61) return 2000;
            else return 2000;
        }
        return 0;
    }

    // Calculate minimum calorie need using BMR and activity level multiplier
    private double calculateMinCalories(String gender, double weight, double height, int age, String activityLevel) {
        double bmr;
        if (gender.equalsIgnoreCase("Male")) {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        }

        // Apply multiplier based on activity level
        double multiplier = 1.2; // Default for sedentary

        if (activityLevel.contains("Sedentary (little or no exercise)")) {
            multiplier = 1.2;
        } else if (activityLevel.contains("Lightly active (light exercise 1–3 days/week) ")) {
            multiplier = 1.375;
        } else if (activityLevel.contains("Moderately active (moderate exercise 3–5 days/week)")) {
            multiplier = 1.55;
        } else if (activityLevel.contains("Very active (hard exercise 6–7 days/week)")) {
            multiplier = 1.725;
        } else if (activityLevel.contains("Extra active (very hard exercise or physical job)")) {
            multiplier = 1.9;
        }

        return bmr * multiplier;
    }
}