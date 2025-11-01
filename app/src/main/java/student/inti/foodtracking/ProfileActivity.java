package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    CardView btnBackBox, btnUserInfo, btnUserManual, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();

        btnBackBox = findViewById(R.id.btnBackBox);
        btnUserInfo = findViewById(R.id.btnUserInfo);
        btnUserManual = findViewById(R.id.btnUserManual);
        btnLogout = findViewById(R.id.btnLogout);

        // Back to Home
        btnBackBox.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, HomeActivity.class)));

        // Go to User Information Page
        btnUserInfo.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, UserInformationActivity.class)));

        // Go to User Manual Page
        btnUserManual.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, UserManualActivity.class)));

        // Logout with confirmation popup
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(ProfileActivity.this, "You have logged out successfully.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
