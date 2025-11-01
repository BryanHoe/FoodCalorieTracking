package student.inti.foodtracking;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OTPActivity extends AppCompatActivity {

    Button btnResendEmail, btnGoToLogin, btnCancel;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    DatabaseReference userRef;

    String username, email, birthDate, height, weight, gender, activityLevel, password;
    double dailyMax, dailyMin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpactivity);

        btnCancel = findViewById(R.id.btnCancel);
        btnResendEmail = findViewById(R.id.btnResendEmail);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        userRef = FirebaseDatabase.getInstance().getReference("Users");

        if (getIntent() != null) {
            username = getIntent().getStringExtra("username");
            email = getIntent().getStringExtra("email");
            birthDate = getIntent().getStringExtra("birthDate");
            height = getIntent().getStringExtra("height");
            weight = getIntent().getStringExtra("weight");
            gender = getIntent().getStringExtra("gender");
            activityLevel = getIntent().getStringExtra("activityLevel");
            dailyMax = getIntent().getDoubleExtra("DailyMaximumCalorie", 0.0);
            dailyMin = getIntent().getDoubleExtra("DailyMinimumCalorie", 0.0);
            password = getIntent().getStringExtra("password");
        }

        if (currentUser != null) {
            sendVerificationEmail(currentUser);
        }

        btnResendEmail.setOnClickListener(v -> {
            if (currentUser != null) {
                sendVerificationEmail(currentUser);
            }
        });

        btnCancel.setOnClickListener(v -> {
            startActivity(new Intent(OTPActivity.this, LoginActivity.class));
            finish();
        });

        btnGoToLogin.setOnClickListener(v -> {
            if (currentUser != null) {
                currentUser.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (currentUser.isEmailVerified()) {
                            saveUserInfoToDatabase(currentUser.getUid());
                        } else {
                            Toast.makeText(OTPActivity.this,
                                    "Please verify your email before proceeding.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(OTPActivity.this,
                                "Failed to reload user. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(OTPActivity.this,
                                "Verification email sent to " + user.getEmail(),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(OTPActivity.this,
                                "Failed to send verification email.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserInfoToDatabase(String userId) {
        User user = new User(username, email, birthDate, height, weight, gender, activityLevel, password, dailyMax, dailyMin);
        userRef.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userRef.child(userId).child("ActivityRecord").setValue("")
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        showSuccessDialog();
                                    } else {
                                        Toast.makeText(OTPActivity.this, "Failed to init ActivityRecord.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(OTPActivity.this, "Failed to save user info.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Popup when data is successfully saved
    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Registration Complete")
                .setMessage("Your account has been verified and saved successfully!")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(OTPActivity.this, LoginActivity.class));
                    finish();
                })
                .show();
    }

    public static class User {
        public String username, email, birthDate, height, weight, gender, activityLevel, password;
        public double dailyMaximumCalorie, dailyMinimumCalorie;

        public User() {}

        public User(String username, String email, String birthDate,
                    String height, String weight, String gender,
                    String activityLevel, String password,
                    double dailyMaximumCalorie, double dailyMinimumCalorie) {
            this.username = username;
            this.email = email;
            this.birthDate = birthDate;
            this.height = height;
            this.weight = weight;
            this.gender = gender;
            this.activityLevel = activityLevel;
            this.password = password;
            this.dailyMaximumCalorie = dailyMaximumCalorie;
            this.dailyMinimumCalorie = dailyMinimumCalorie;
        }
    }
}
