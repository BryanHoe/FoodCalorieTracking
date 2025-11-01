package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class RecommendActivity extends AppCompatActivity {

    TextView tvTitle, tvAdvice;
    LinearLayout recommendationContainer;
    CardView btnBackBox;

    DatabaseReference userRef, foodRef, exerciseRef;
    FirebaseUser currentUser;

    String selectedDate;
    double dailyCalorieConsumed = 0;
    double dailyCalorieBurned = 0;
    double minDailyCalorie = 1500;
    double maxDailyCalorie = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend);

        btnBackBox = findViewById(R.id.btnBackBox);
        tvTitle = findViewById(R.id.tvTitle);
        recommendationContainer = findViewById(R.id.recommendationContainer);

        //  Dynamically add advice text below title
        tvAdvice = new TextView(this);
        tvAdvice.setTextSize(16);
        tvAdvice.setTextColor(0xFF000000);
        tvAdvice.setPadding(0, 10, 0, 0);
        ((LinearLayout) tvTitle.getParent()).addView(tvAdvice);

        //  Back button
        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(RecommendActivity.this, HomeActivity.class));
            finish();
        });

        // Ensure current user logged in
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        foodRef = FirebaseDatabase.getInstance().getReference("Food");
        exerciseRef = FirebaseDatabase.getInstance().getReference("Exercise");

        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        loadRecommendations();
    }

    // Load Recommendations based on user activity record
    private void loadRecommendations() {
        recommendationContainer.removeAllViews(); // clear old recommendations

        DatabaseReference recordRef = userRef.child("ActivityRecord").child(selectedDate);
        recordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasExercise = snapshot.child("Exercises").exists();

                dailyCalorieConsumed = snapshot.child("dailyCalorieConsumed").getValue(Double.class) != null
                        ? snapshot.child("dailyCalorieConsumed").getValue(Double.class) : 0.0;

                dailyCalorieBurned = snapshot.child("dailyCalorieBurned").getValue(Double.class) != null
                        ? snapshot.child("dailyCalorieBurned").getValue(Double.class) : 0.0;

                minDailyCalorie = snapshot.child("dailyMinimumCalorie").getValue(Double.class) != null
                        ? snapshot.child("dailyMinimumCalorie").getValue(Double.class) : 1500.0;

                maxDailyCalorie = snapshot.child("dailyMaximumCalorie").getValue(Double.class) != null
                        ? snapshot.child("dailyMaximumCalorie").getValue(Double.class) : 2500.0;

                double progress = dailyCalorieConsumed  / minDailyCalorie * 100;

                StringBuilder advice = new StringBuilder(); // collect multiple advices

                //  Exercise advice
                if (!hasExercise) {
                    advice.append("💡 You haven’t done any exercise today. Try to include at least one activity!\n\n");
                    showExerciseRecommendations();
                }

                //  Calorie advice
                if (progress < 75) {
                    advice.append("💡 Your calorie intake is below 75%. Try some high-calorie meals to reach your goal.");
                    showFoodRecommendations(true); // high calorie
                } else {
                    advice.append("💡 Your calorie intake exceeds 75%. Focus on low-calorie meals and stay active.");
                    showFoodRecommendations(false); // low calorie
                }

                tvAdvice.setText(advice.toString()); // show combined advice
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RecommendActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Exercise Recommendations
    private void showExerciseRecommendations() {
        CardView mainBox = createContainerBox();
        LinearLayout box = (LinearLayout) mainBox.getChildAt(0);

        TextView title = createSectionTitle("Exercise Recommended");
        box.addView(title);

        recommendationContainer.addView(mainBox);

        // Show 3 Random Exercises from the database
        exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> allExercises = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.getKey() != null) allExercises.add(ds.getKey());
                }
                Collections.shuffle(allExercises);
                for (int i = 0; i < Math.min(3, allExercises.size()); i++) {
                    addItemBox(box, allExercises.get(i), true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Food Recommendations
    private void showFoodRecommendations(boolean highCalorie) {
        CardView mainBox = createContainerBox();
        LinearLayout box = (LinearLayout) mainBox.getChildAt(0);

        TextView title = createSectionTitle(highCalorie ? "High Calorie Food Suggestions" : "Low Calorie Food Suggestions");
        box.addView(title);

        recommendationContainer.addView(mainBox);

        // Show 5 Random Foods from database
        foodRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Map<String, Object>> allFoods = new ArrayList<>();
                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    String category = categorySnap.getKey();
                    for (DataSnapshot foodSnap : categorySnap.getChildren()) {
                        Object calObj = foodSnap.child("calories").getValue();
                        if (calObj != null) {
                            double cal = Double.parseDouble(calObj.toString());
                            if ((highCalorie && cal > 300) || (!highCalorie && cal < 300)) {
                                Map<String, Object> map = new HashMap<>();
                                map.put("name", foodSnap.getKey());
                                map.put("category", category);
                                map.put("calories", cal);
                                allFoods.add(map);
                            }
                        }
                    }
                }
                Collections.shuffle(allFoods);
                for (int i = 0; i < Math.min(5, allFoods.size()); i++) {
                    Map<String, Object> food = allFoods.get(i);
                    addFoodItemBox(box,
                            (String) food.get("name"),
                            (String) food.get("category"),
                            (double) food.get("calories"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Reusable: White container box
    private CardView createContainerBox() {
        CardView mainBox = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 20, 0, 30);
        mainBox.setLayoutParams(params);
        mainBox.setRadius(30);
        mainBox.setCardBackgroundColor(0xFFFFFFFF);
        mainBox.setUseCompatPadding(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(30, 30, 30, 30);
        mainBox.addView(box);
        return mainBox;
    }

    // Reusable: Section title
    private TextView createSectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(20);
        title.setTextColor(0xFF000000);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        return title;
    }

    // Exercise item
    private void addItemBox(LinearLayout parentBox, String name, boolean isExercise) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 15, 0, 15);
        card.setLayoutParams(cardParams);
        card.setRadius(20);
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(true);

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(20, 20, 20, 20);

        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextSize(16);
        tv.setTextColor(0xFF000000);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        item.addView(tv);

        TextView btnAdd = new TextView(this);
        btnAdd.setText("Add");
        btnAdd.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        btnAdd.setTextSize(16);
        btnAdd.setPadding(20, 0, 0, 0);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(RecommendActivity.this, LogExerciseActivity.class);
            intent.putExtra("exerciseName", name);
            startActivity(intent);
        });
        item.addView(btnAdd);

        card.addView(item);
        parentBox.addView(card);
    }

    // Food item
    private void addFoodItemBox(LinearLayout parentBox, String name, String category, double calories) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 15, 0, 15);
        card.setLayoutParams(cardParams);
        card.setRadius(20);
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setUseCompatPadding(true);

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(20, 20, 20, 20);

        TextView tv = new TextView(this);
        tv.setText(name + " (" + category + ") - " + calories + " kcal");
        tv.setTextSize(16);
        tv.setTextColor(0xFF000000);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        item.addView(tv);

        TextView btnAdd = new TextView(this);
        btnAdd.setText("Add");
        btnAdd.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        btnAdd.setTextSize(16);
        btnAdd.setPadding(20, 0, 0, 0);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(RecommendActivity.this, LogFoodActivity.class);
            intent.putExtra("foodName", name);
            intent.putExtra("category", category);
            intent.putExtra("calories", calories);
            startActivity(intent);
        });
        item.addView(btnAdd);

        card.addView(item);
        parentBox.addView(card);
    }
}