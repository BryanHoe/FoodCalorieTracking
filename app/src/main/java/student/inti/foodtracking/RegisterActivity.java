package student.inti.foodtracking;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Calendar;

public class RegisterActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etHeight, etWeight, etPassword, etConfirmPassword;
    Spinner spGender, spActivityLevel;
    TextView tvBirthDate, tvLogin;
    Button btnRegister;
    FirebaseAuth mAuth;

    String selectedBirthDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spGender = findViewById(R.id.spGender);
        spActivityLevel = findViewById(R.id.spActivityLevel);
        tvBirthDate = findViewById(R.id.tvBirthDate);
        tvLogin = findViewById(R.id.tvLogin);
        btnRegister = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();

        // Birthdate Picker
        tvBirthDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        selectedBirthDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        tvBirthDate.setText(selectedBirthDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        tvLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));

        // Setup Gender Spinner
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"Select Gender", "Male", "Female"});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(genderAdapter);
        spGender.setSelection(0, false);

        // Setup Activity Level Spinner
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"Select Activity Level", "Sedentary (little or no exercise)",
                        "Lightly active (light exercise 1–3 days/week)",
                        "Moderately active (moderate exercise 3–5 days/week)",
                        "Very active (hard exercise 6–7 days/week)",
                        "Extra active (very hard exercise or physical job)"});
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spActivityLevel.setAdapter(activityAdapter);
        spActivityLevel.setSelection(0, false);

        // Register Button
        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                String password = etPassword.getText().toString().trim();

                // Password Policy Check
                if (!isPasswordValid(password)) {
                    showPasswordPolicyDialog();
                    return;
                }

                if (spGender.getSelectedItemPosition() == 0) {
                    Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (spActivityLevel.getSelectedItemPosition() == 0) {
                    Toast.makeText(this, "Please select your activity level", Toast.LENGTH_SHORT).show();
                    return;
                }

                String username = etUsername.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String heightStr = etHeight.getText().toString().trim();
                String weightStr = etWeight.getText().toString().trim();
                String gender = spGender.getSelectedItem().toString();
                String activityLevel = spActivityLevel.getSelectedItem().toString();

                double height = Double.parseDouble(heightStr);
                double weight = Double.parseDouble(weightStr);
                int age = calculateAge(selectedBirthDate);

                double dailyMax = roundToTwoDecimals(calculateMaxCalories(gender, age));
                double dailyMin = roundToTwoDecimals(calculateMinCalories(gender, weight, height, age, activityLevel));

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Intent intent = new Intent(RegisterActivity.this, OTPActivity.class);
                                intent.putExtra("username", username);
                                intent.putExtra("email", email);
                                intent.putExtra("birthDate", selectedBirthDate);
                                intent.putExtra("height", heightStr);
                                intent.putExtra("weight", weightStr);
                                intent.putExtra("gender", gender);
                                intent.putExtra("activityLevel", activityLevel);
                                intent.putExtra("DailyMaximumCalorie", dailyMax);
                                intent.putExtra("DailyMinimumCalorie", dailyMin);
                                intent.putExtra("password", password);
                                startActivity(intent);
                                finish();
                            } else {
                                String errorMessage = task.getException().getMessage();

                                // Email already used popup
                                if (errorMessage != null && errorMessage.contains("The email address is already in use")) {
                                    new AlertDialog.Builder(RegisterActivity.this)
                                            .setTitle("Email Already Used")
                                            .setMessage("This email is already associated with another account. Please try using a different email address.")
                                            .setPositiveButton("OK", null)
                                            .show();
                                } else {
                                    Toast.makeText(RegisterActivity.this,
                                            "Registration failed: " + errorMessage,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }

    // Validation
    private boolean validateInputs() {
        if (etUsername.getText().toString().trim().isEmpty()) {
            etUsername.setError("Enter username");
            return false;
        }
        if (etEmail.getText().toString().trim().isEmpty()) {
            etEmail.setError("Enter email");
            return false;
        }
        if (selectedBirthDate.isEmpty()) {
            Toast.makeText(this, "Select birth date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etHeight.getText().toString().trim().isEmpty()) {
            etHeight.setError("Enter height");
            return false;
        }
        if (etWeight.getText().toString().trim().isEmpty()) {
            etWeight.setError("Enter weight");
            return false;
        }
        if (etPassword.getText().toString().trim().isEmpty()) {
            etPassword.setError("Enter password");
            return false;
        }
        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    // Password Policy Check
    private boolean isPasswordValid(String password) {
        String upperCasePattern = ".*[A-Z].*";
        String lowerCasePattern = ".*[a-z].*";
        String numberPattern = ".*[0-9].*";
        String specialPattern = ".*[!@#$%^&*(),.?\":{}|<>].*";

        return password.length() >= 12 &&
                password.length() <= 4096 &&
                password.matches(upperCasePattern) &&
                password.matches(lowerCasePattern) &&
                password.matches(numberPattern) &&
                password.matches(specialPattern);
    }

    // Password Policy Popup
    private void showPasswordPolicyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Password Policy")
                .setMessage("Your password must meet the following requirements:\n\n" +
                        "• Minimum length: 12 characters\n" +
                        "• Maximum length: 4096 characters\n" +
                        "• At least 1 uppercase letter (A–Z)\n" +
                        "• At least 1 lowercase letter (a–z)\n" +
                        "• At least 1 number (0–9)\n" +
                        "• At least 1 special character (!@#$%^&*)")
                .setPositiveButton("OK", null)
                .show();
    }

    // Age Calculation
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
            return 25;
        }
    }

    // Daily Max Calories
    private double calculateMaxCalories(String gender, int age) {
        if (gender.equalsIgnoreCase("Male")) {
            if (age >= 2 && age <= 4) return 1600;
            else if (age >= 5 && age <= 8) return 2000;
            else if (age >= 9 && age <= 13) return 2600;
            else if (age >= 14 && age <= 18) return 3200;
            else if (age >= 19 && age <= 60) return 3000;
            else if (age >= 61) return 2600;
        } else if (gender.equalsIgnoreCase("Female")) {
            if (age >= 2 && age <= 4) return 1400;
            else if (age >= 5 && age <= 8) return 1800;
            else if (age >= 9 && age <= 13) return 2200;
            else if (age >= 14 && age <= 18) return 2400;
            else if (age >= 19 && age <= 30) return 2400;
            else if (age >= 31 && age <= 60) return 2200;
            else if (age >= 61) return 2000;
        }
        return 0;
    }

    // Daily Min Calories
    private double calculateMinCalories(String gender, double weight, double height, int age, String activityLevel) {
        double bmr;
        if (gender.equalsIgnoreCase("Male")) {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        }

        double multiplier = 1.2;

        if (activityLevel.contains("Sedentary")) multiplier = 1.2;
        else if (activityLevel.contains("Lightly active")) multiplier = 1.375;
        else if (activityLevel.contains("Moderately active")) multiplier = 1.55;
        else if (activityLevel.contains("Very active")) multiplier = 1.725;
        else if (activityLevel.contains("Extra active")) multiplier = 1.9;

        return bmr * multiplier;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

