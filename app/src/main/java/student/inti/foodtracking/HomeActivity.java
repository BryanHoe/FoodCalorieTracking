package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    Button btnProfile, btnReport, btnRecommendFood, btnActivity, btnScan, btnExercise;
    CardView cardProfile, cardReport, cardRecommendFood, cardActivity, cardScan, cardExercise;
    TextView tvCalorieProgress, tvCalorieMessage;

    DatabaseReference userRef;
    String userId, todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Buttons
        btnProfile = findViewById(R.id.btnProfile);
        btnReport = findViewById(R.id.btnReport);
        btnRecommendFood = findViewById(R.id.btnRecommendFood);
        btnActivity = findViewById(R.id.btnActivity);
        btnScan = findViewById(R.id.btnScan);
        btnExercise = findViewById(R.id.btnExercise);

        // CardViews
        cardProfile = (CardView) btnProfile.getParent().getParent();
        cardReport = (CardView) btnReport.getParent().getParent();
        cardRecommendFood = (CardView) btnRecommendFood.getParent().getParent();
        cardActivity = (CardView) btnActivity.getParent().getParent();
        cardScan = (CardView) btnScan.getParent().getParent();
        cardExercise = (CardView) btnExercise.getParent().getParent();

        // TextViews
        tvCalorieProgress = findViewById(R.id.tvCalorieProgress);
        tvCalorieMessage = findViewById(R.id.tvCalorieMessage);

        // Firebase setup
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        // Get today's date (e.g., "10-10-2025")
        todayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        // Start listening for today's calorie data
        fetchTodayCalorieData();

        // Setup button navigation
        setupNavigation();
    }

    // Fetch Daily Calorie Data from the database
    private void fetchTodayCalorieData() {
        // Prevent crash or unnecessary fetch if user logs out
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        DatabaseReference todayRef = userRef.child("ActivityRecord").child(todayDate);

        todayRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvCalorieProgress.setText("You haven't logged any record today.");
                    tvCalorieMessage.setText("Start logging your meals and exercises!");
                    return;
                }

                Double consumed = snapshot.child("dailyCalorieConsumed").getValue(Double.class);
                Double minimum = snapshot.child("dailyMinimumCalorie").getValue(Double.class);

                if (consumed == null) consumed = 0.0;
                if (minimum == null || minimum == 0) minimum = 2000.0;

                updateCalorieCard(consumed, minimum);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Only show toast if user is still logged in
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Toast.makeText(HomeActivity.this, "Failed to fetch calorie data.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Update Daily Calorie Data and Show Small Report
    private void updateCalorieCard(double consumed,  double minimum) {
        if (consumed == 0) {
            tvCalorieProgress.setText("You haven't logged any record today.");
            tvCalorieMessage.setText("Start logging your meals and exercises!");
            return;
        }

        double progress = consumed  / minimum;
        int percentage = (int) (progress * 100);

        if (percentage < 0) percentage = 0;
        if (percentage > 100) percentage = 100;

        // Bold, larger font, and black percentage text
        String styledText = "You have reached <b><font color='#000000'><big>" + percentage +
                "%</big></font></b> of your minimum daily calorie intake.";
        tvCalorieProgress.setText(android.text.Html.fromHtml(styledText));

        if (percentage < 25) {
            tvCalorieMessage.setText("You’re just getting started — keep it up!");
        } else if (percentage < 50) {
            tvCalorieMessage.setText("Good effort! You’re making steady progress.");
        } else if (percentage < 75) {
            tvCalorieMessage.setText("Almost there! Stay consistent!");
        } else if (percentage < 100) {
            tvCalorieMessage.setText("You’re so close! Keep pushing!");
        } else {
            tvCalorieMessage.setText("Great job! You've met your goal today!");
        }
    }

    // Buttons
    private void setupNavigation() {
        View.OnClickListener profileClick = v ->
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        btnProfile.setOnClickListener(profileClick);
        cardProfile.setOnClickListener(profileClick);

        View.OnClickListener reportClick = v ->
                startActivity(new Intent(HomeActivity.this, ReportActivity.class));
        btnReport.setOnClickListener(reportClick);
        cardReport.setOnClickListener(reportClick);

        View.OnClickListener recommendClick = v ->
                startActivity(new Intent(HomeActivity.this, RecommendActivity.class));
        btnRecommendFood.setOnClickListener(recommendClick);
        cardRecommendFood.setOnClickListener(recommendClick);

        View.OnClickListener activityClick = v ->
                startActivity(new Intent(HomeActivity.this, FoodActivity.class));
        btnActivity.setOnClickListener(activityClick);
        cardActivity.setOnClickListener(activityClick);

        View.OnClickListener scanClick = v ->
                startActivity(new Intent(HomeActivity.this, CameraActivity.class));
        btnScan.setOnClickListener(scanClick);
        cardScan.setOnClickListener(scanClick);

        View.OnClickListener exerciseClick = v ->
                startActivity(new Intent(HomeActivity.this, SearchExerciseActivity.class));
        btnExercise.setOnClickListener(exerciseClick);
        cardExercise.setOnClickListener(exerciseClick);
    }
}