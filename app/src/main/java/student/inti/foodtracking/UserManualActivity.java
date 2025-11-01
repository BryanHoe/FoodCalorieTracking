package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class UserManualActivity extends AppCompatActivity {

    CardView btnBackBox;
    Button btnHowToLog, btnPolicy, btnAccountHelp, btnAppFeatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manual);

        // Initialize buttons
        btnBackBox = findViewById(R.id.btnBackBox);
        btnHowToLog = findViewById(R.id.btnHowToLog);
        btnPolicy = findViewById(R.id.btnPolicy);
        btnAccountHelp = findViewById(R.id.btnAccountHelp);
        btnAppFeatures = findViewById(R.id.btnAppFeatures);


        // Back to Home
        btnBackBox.setOnClickListener(v ->
                startActivity(new Intent(UserManualActivity.this, ProfileActivity.class)));

        // Go to How To Log Page
        btnHowToLog.setOnClickListener(v ->
                startActivity(new Intent(UserManualActivity.this, HowToLogActivity.class)));

        // Go to Policy Statement Page
        btnPolicy.setOnClickListener(v ->
                startActivity(new Intent(UserManualActivity.this, PolicyActivity.class)));

        // Go to Account Help Page
        btnAccountHelp.setOnClickListener(v ->
                startActivity(new Intent(UserManualActivity.this, AccountHelpActivity.class)));

        // Go to App Features Page
        btnAppFeatures.setOnClickListener(v ->
                startActivity(new Intent(UserManualActivity.this, AppFeaturesActivity.class)));

    }
}