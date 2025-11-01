package student.inti.foodtracking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReportActivity extends AppCompatActivity {

    PieChart chartOverall, chartConsumed, chartBurned;
    TextView tvDate, tvFoodList, tvExerciseList, tvNoRecord, tvOverallValues;
    Button btnViewDetails;
    CardView boxOverall, boxConsumed, boxBurned, btnBackBox;

    DatabaseReference userRef;
    String uid;
    Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        tvDate = findViewById(R.id.tvDate);
        chartOverall = findViewById(R.id.chartOverall);
        chartConsumed = findViewById(R.id.chartConsumed);
        chartBurned = findViewById(R.id.chartBurned);
        tvFoodList = findViewById(R.id.tvFoodList);
        tvExerciseList = findViewById(R.id.tvExerciseList);
        tvOverallValues = findViewById(R.id.tvOverallValues);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        boxOverall = findViewById(R.id.boxOverall);
        boxConsumed = findViewById(R.id.boxConsumed);
        boxBurned = findViewById(R.id.boxBurned);
        tvNoRecord = findViewById(R.id.tvNoRecord);
        btnBackBox = findViewById(R.id.btnBackBox);

        //  Firebase
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("ActivityRecord");

        //  Date
        calendar = Calendar.getInstance();
        setDateButtonText();

        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(ReportActivity.this, HomeActivity.class));
            finish();
        });

        tvDate.setOnClickListener(v -> showDatePicker());

        btnViewDetails.setOnClickListener(v ->
                startActivity(new Intent(ReportActivity.this, ReportDetailsActivity.class)));

        //  Load initial data
        loadDataForDate(tvDate.getText().toString());
    }

    private void setDateButtonText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
    }

    // Select Date
    private void showDatePicker() {
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            setDateButtonText();
            loadDataForDate(tvDate.getText().toString());
        }, y, m, d).show();
    }

    // Load data from the database based on the date chosen
    private void loadDataForDate(String date) {
        userRef.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    boxOverall.setVisibility(android.view.View.GONE);
                    boxConsumed.setVisibility(android.view.View.GONE);
                    boxBurned.setVisibility(android.view.View.GONE);
                    tvNoRecord.setVisibility(android.view.View.VISIBLE);
                    tvNoRecord.setText("No records for " + date);
                    return;
                }

                boxOverall.setVisibility(android.view.View.VISIBLE);
                boxConsumed.setVisibility(android.view.View.VISIBLE);
                boxBurned.setVisibility(android.view.View.VISIBLE);
                tvNoRecord.setVisibility(android.view.View.GONE);

                double dailyConsumed = safeGetNumber(snapshot, "dailyCalorieConsumed");
                double dailyBurned = safeGetNumber(snapshot, "dailyCalorieBurned");
                double dailyMin = safeGetNumber(snapshot, "dailyMinimumCalorie");
                double dailyMax = safeGetNumber(snapshot, "dailyMaximumCalorie");

                tvOverallValues.setText(Html.fromHtml(
                        "Calories Consumed: " + "<b>" +(int) dailyConsumed + " kcal</b><br>" +
                                "Calories Burned: " + "<b>" +(int) dailyBurned + " kcal</b><br>" +
                                "Minimum: " + "<b>" +(int) dailyMin + " kcal</b><br>" +
                                "Maximum: " + "<b>" +(int) dailyMax + " kcal</b>"
                ));

                setupOverallChart(dailyConsumed, dailyMin, dailyBurned, dailyMax);
                setupConsumedChart(snapshot.child("Foods"), dailyMax);
                setupBurnedChart(snapshot.child("Exercises"), dailyMax);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Safely retrieves a numeric value (double) from a Firebase DataSnapshot.
    private double safeGetNumber(DataSnapshot snap, String key) {
        if (snap == null || !snap.hasChild(key)) return 0.0;
        Object o = snap.child(key).getValue();
        if (o == null) return 0.0;
        try {
            if (o instanceof Number) return ((Number) o).doubleValue();
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Overall Chart
    private void setupOverallChart(double consumed, double min, double burned, double max) {
        if (max <= 0) max = 1;

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) consumed, "Consumed"));
        entries.add(new PieEntry((float) burned, "Burned"));

        double remaining = max - (consumed + burned);
        entries.add(new PieEntry((float) Math.max(0, remaining), remaining >= 0 ? "Remaining" : "Over Limit"));

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(ColorTemplate.MATERIAL_COLORS);
        ds.setValueTextSize(12f);
        ds.setSliceSpace(2f);
        ds.setValueFormatter(kcalOnlyFormatter());

        PieData data = new PieData(ds);
        chartOverall.setData(data);
        chartOverall.getDescription().setEnabled(false);
        chartOverall.setUsePercentValues(false);
        chartOverall.setCenterText("Overall Calories\nMin: " + (int) min + " kcal");
        chartOverall.setCenterTextSize(14f);
        chartOverall.setRotationAngle(270f);
        chartOverall.setDrawEntryLabels(true);
        chartOverall.invalidate();
    }

    // Consumed chart
    private void setupConsumedChart(DataSnapshot foodsSnapshot, double maximum) {
        chartConsumed.clear();
        LinkedHashMap<String, Double> mealTotals = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        double totalCalories = 0;

        if (foodsSnapshot.exists()) {
            for (DataSnapshot child : foodsSnapshot.getChildren()) {
                String meal = safeGetString(child, "meal", "Extra");
                double kcal = safeGetNumber(child, "calories");
                String foodName = safeGetString(child, "foodName", "Food");

                mealTotals.put(meal, mealTotals.getOrDefault(meal, 0.0) + kcal);

                sb.append(foodName)
                        .append(" | Calories Consumed: ")
                        .append("<b>")
                        .append((int) kcal)
                        .append(" kcal</b><br>");

                totalCalories += kcal;
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> e : mealTotals.entrySet()) {
            entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
        }
        if (entries.isEmpty()) entries.add(new PieEntry(1f, "No food"));

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(ColorTemplate.COLORFUL_COLORS);
        ds.setValueTextSize(12f);
        ds.setValueFormatter(kcalOnlyFormatter());

        chartConsumed.setData(new PieData(ds));
        chartConsumed.getDescription().setEnabled(false);
        chartConsumed.setCenterText("Calories Consumed");
        chartConsumed.setCenterTextSize(13f);
        chartConsumed.invalidate();

        tvFoodList.setText(Html.fromHtml(sb.length() == 0 ? "No food records" : sb.toString()));
    }

    // Burned chart
    private void setupBurnedChart(DataSnapshot exercisesSnapshot, double maximum) {
        chartBurned.clear();
        List<PieEntry> entries = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        double totalBurned = 0;

        if (exercisesSnapshot.exists()) {
            for (DataSnapshot child : exercisesSnapshot.getChildren()) {
                String name = safeGetString(child, "exerciseName", "Exercise");
                double kcal = safeGetNumber(child, "totalCalories");
                entries.add(new PieEntry((float) kcal, name));

                sb.append(name)
                        .append(" | Calories Burned: ")
                        .append("<b>")
                        .append((int) kcal)
                        .append(" kcal</b><br>");

                totalBurned += kcal;
            }
        }

        if (entries.isEmpty()) entries.add(new PieEntry(1f, "No exercise"));

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(ColorTemplate.JOYFUL_COLORS);
        ds.setValueTextSize(12f);
        ds.setValueFormatter(kcalOnlyFormatter());

        chartBurned.setData(new PieData(ds));
        chartBurned.getDescription().setEnabled(false);
        chartBurned.setCenterText("Calories Burned");
        chartBurned.setCenterTextSize(13f);
        chartBurned.invalidate();

        tvExerciseList.setText(Html.fromHtml(sb.length() == 0 ? "No exercise records" : sb.toString()));
    }

    // Formatting
    private ValueFormatter kcalOnlyFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f kcal", value);
            }
        };
    }

    // Safely retrieves a string value from a Firebase DataSnapshot.
    private String safeGetString(DataSnapshot s, String key, String fallback) {
        if (s == null || !s.hasChild(key)) return fallback;
        Object o = s.child(key).getValue();
        return o != null ? o.toString() : fallback;
    }
}