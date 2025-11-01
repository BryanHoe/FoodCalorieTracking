package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;

public class DeleteAccountActivity extends AppCompatActivity {

    Button btnDelete, btnBack;
    CardView btnBackBox;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);

        // Initialize
        mAuth = FirebaseAuth.getInstance();
        btnDelete = findViewById(R.id.btnDeleteAccount);
        btnBack = findViewById(R.id.btnBackToInfo);
        btnBackBox = findViewById(R.id.btnBackBox);

        // (Small) Back to UserInformationActivity
        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(DeleteAccountActivity.this, UserInformationActivity.class));
            finish();
        });

        // (Big) Back to UserInformationActivity
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(DeleteAccountActivity.this, UserInformationActivity.class));
            finish();
        });

        // Delete account permanently
        btnDelete.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(DeleteAccountActivity.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(DeleteAccountActivity.this, LoginActivity.class));
                                finish();
                            } else {
                                Toast.makeText(DeleteAccountActivity.this, "Failed to delete account: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(DeleteAccountActivity.this, "No user is signed in.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}