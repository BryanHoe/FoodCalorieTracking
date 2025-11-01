package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UserInformationActivity extends AppCompatActivity {

    TextView tvUsername, tvEmail, tvGender, tvBirth, tvAge, tvHeight, tvWeight, tvActivityLevel;
    TextView tvMinimumCalories, tvMaximumCalories;
    Button btnEditProfile, btnViewDetails, btnDeleteAccount;
    CardView btnBackBox;

    FirebaseAuth mAuth;
    DatabaseReference userRef;
    FirebaseUser currentUser;

    final DecimalFormat df = new DecimalFormat("0.00"); // ✅ for 2 decimals

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_information);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        // Init UI
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvGender = findViewById(R.id.tvGender);
        tvBirth = findViewById(R.id.tvBirth);
        tvAge = findViewById(R.id.tvAge);
        tvHeight = findViewById(R.id.tvHeight);
        tvWeight = findViewById(R.id.tvWeight);
        tvActivityLevel = findViewById(R.id.tvActivityLevel);
        tvMinimumCalories = findViewById(R.id.tvMinimumCalories);
        tvMaximumCalories = findViewById(R.id.tvMaximumCalories);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnBackBox = findViewById(R.id.btnBackBox);

        // Load User Data
        loadUserData();

        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(UserInformationActivity.this, ProfileActivity.class));
            finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(UserInformationActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnViewDetails.setOnClickListener(v -> {
            startActivity(new Intent(UserInformationActivity.this, CalorieDetailsActivity.class));
        });

        btnDeleteAccount.setOnClickListener(v -> {
            startActivity(new Intent(UserInformationActivity.this, DeleteAccountActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    // Load user data from database
    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String birth = snapshot.child("birthDate").getValue(String.class);
                    String height = snapshot.child("height").getValue(String.class);
                    String weight = snapshot.child("weight").getValue(String.class);
                    String activityLevel = snapshot.child("activityLevel").getValue(String.class);

                    int age = calculateAge(birth);

                    tvUsername.setText("Username: " + (username != null ? username : ""));
                    tvEmail.setText("Email: " + (email != null ? email : ""));
                    tvGender.setText("Gender: " + (gender != null ? gender : ""));
                    tvBirth.setText("Birth: " + (birth != null ? birth : ""));
                    tvAge.setText("Age: " + age);
                    tvHeight.setText("Height: " + (height != null ? height : "") + " cm");
                    tvWeight.setText("Weight: " + (weight != null ? weight : "") + " kg");
                    tvActivityLevel.setText("Activity Level: " + (activityLevel != null ? activityLevel : ""));

                    //  Fetch directly from Users
                    Double minCalories = snapshot.child("dailyMinimumCalorie").getValue(Double.class);
                    Double maxCalories = snapshot.child("dailyMaximumCalorie").getValue(Double.class);

                    if (minCalories == null) minCalories = 0.0;
                    if (maxCalories == null) maxCalories = 0.0;

                    tvMinimumCalories.setText("Minimum: " + df.format(minCalories) + " kcal");
                    tvMaximumCalories.setText("Maximum: " + df.format(maxCalories) + " kcal");

                } else {
                    Toast.makeText(UserInformationActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserInformationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Age Calculation
    private int calculateAge(String birthDateString) {
        if (birthDateString == null || birthDateString.isEmpty()) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Calendar dob = Calendar.getInstance();
            dob.setTime(sdf.parse(birthDateString));
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }
}