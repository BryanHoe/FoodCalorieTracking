package student.inti.foodtracking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportDetailsActivity extends AppCompatActivity {

    Spinner spPeriod, spMonth;
    Button btnViewBackToReport;
    LineChart chartConsumed, chartBurned;
    TextView tvNoRecord;
    CardView btnBackBox;

    DatabaseReference userRef;
    String uid;

    Calendar calendar;

    String PERIOD_MONTH = "By Month";
    String PERIOD_HALF = "Half Year";
    String PERIOD_YEAR = "Year";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_details);

        spPeriod = findViewById(R.id.spPeriod);
        spMonth = findViewById(R.id.spMonth);
        chartConsumed = findViewById(R.id.chartConsumed);
        chartBurned = findViewById(R.id.chartBurned);
        tvNoRecord = findViewById(R.id.tvNoRecord);
        btnViewBackToReport = findViewById(R.id.btnViewBackToReport);
        btnBackBox = findViewById(R.id.btnBackBox);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("ActivityRecord");
        calendar = Calendar.getInstance();

        setupSpinners();

        btnBackBox.setOnClickListener(v -> finish());

        btnViewBackToReport.setOnClickListener(v -> {
            startActivity(new Intent(ReportDetailsActivity.this, ReportActivity.class));
            finish();
        });

        loadForCurrentSelection();
    }

    private void setupSpinners() {
        List<String> periods = Arrays.asList(PERIOD_MONTH, PERIOD_HALF, PERIOD_YEAR);
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPeriod.setAdapter(periodAdapter);

        String[] months = new DateFormatSymbols().getShortMonths();
        List<String> monthList = new ArrayList<>();
        for (int i = 0; i < 12; i++) monthList.add(months[i]);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthList);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonth.setAdapter(monthAdapter);

        int currentMonth = calendar.get(Calendar.MONTH);
        spMonth.setSelection(currentMonth);

        spPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sel = (String) parent.getItemAtPosition(position);
                spMonth.setVisibility(PERIOD_MONTH.equals(sel) ? View.VISIBLE : View.GONE);
                loadForCurrentSelection();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (PERIOD_MONTH.equals(spPeriod.getSelectedItem().toString()))
                    loadForCurrentSelection();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadForCurrentSelection() {
        String period = (String) spPeriod.getSelectedItem();
        if (PERIOD_MONTH.equals(period)) {
            int monthIndex = spMonth.getSelectedItemPosition();
            loadMonth(monthIndex, calendar.get(Calendar.YEAR));
        } else if (PERIOD_HALF.equals(period)) {
            loadRangeAsMonths(getLastNMonthsRange(6));
        } else {
            loadRangeAsMonths(getLastNMonthsRange(12));
        }
    }

    private static class MonthRange {
        int year;
        int month0;
        int days;
        String monthLabel;

        MonthRange(int y, int m0, int days, String label) {
            this.year = y;
            this.month0 = m0;
            this.days = days;
            this.monthLabel = label;
        }
    }

    private List<MonthRange> getLastNMonthsRange(int n) {
        List<MonthRange> out = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            Calendar t = (Calendar) calendar.clone();
            t.add(Calendar.MONTH, -i);
            int y = t.get(Calendar.YEAR);
            int m0 = t.get(Calendar.MONTH);
            int maxDay = t.getActualMaximum(Calendar.DAY_OF_MONTH);
            String label = new SimpleDateFormat("MMM", Locale.getDefault()).format(t.getTime());
            out.add(new MonthRange(y, m0, maxDay, label));
        }
        return out;
    }

    private void loadMonth(int month0, int year) {
        Calendar start = Calendar.getInstance();
        start.set(year, month0, 1);
        Calendar end = Calendar.getInstance();
        end.set(year, month0, start.getActualMaximum(Calendar.DAY_OF_MONTH));

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                int daysInMonth = start.getActualMaximum(Calendar.DAY_OF_MONTH);
                double[] consumed = new double[daysInMonth];
                double[] burned = new double[daysInMonth];
                double[] maxCal = new double[daysInMonth];
                double[] minCal = new double[daysInMonth];
                boolean any = false;

                for (DataSnapshot child : snap.getChildren()) {
                    String key = child.getKey();
                    if (key == null) continue;

                    String[] parts = key.split("-");
                    if (parts.length != 3) continue;
                    int d = tryParseInt(parts[0], -1);
                    int m = tryParseInt(parts[1], -1);
                    int y = tryParseInt(parts[2], -1);

                    if (d < 1 || m < 1 || y < 1900) continue;

                    if (y == year && (m - 1) == month0) {
                        int idx = d - 1;
                        consumed[idx] = getDoubleOrZero(child, "dailyCalorieConsumed");
                        burned[idx] = getDoubleOrZero(child, "dailyCalorieBurned");
                        maxCal[idx] = getDoubleOrZero(child, "dailyMaximumCalorie");
                        minCal[idx] = getDoubleOrZero(child, "dailyMinimumCalorie");
                        any = true;
                    }
                }

                if (!any) {
                    showNoRecords(true);
                    clearCharts();
                    return;
                }

                List<String> labels = new ArrayList<>();
                for (int i = 1; i <= daysInMonth; i++) labels.add(String.valueOf(i));

                plotLineChart(chartConsumed, labels, consumed, maxCal, minCal, "Consumed", "Maximum", "Minimum");
                plotLineChart(chartBurned, labels, burned, maxCal, minCal, "Burned", "Maximum", "Minimum");
                showNoRecords(false);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportDetailsActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Aggregation for monthly data
    private void loadRangeAsMonths(List<MonthRange> ranges) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                int n = ranges.size();
                double[] consumed = new double[n];
                double[] burned = new double[n];
                double[] maxCal = new double[n];
                double[] minCal = new double[n];
                boolean any = false;

                for (DataSnapshot child : snap.getChildren()) {
                    String key = child.getKey();
                    if (key == null) continue;
                    String[] parts = key.split("-");
                    if (parts.length != 3) continue;

                    int d = tryParseInt(parts[0], -1);
                    int m = tryParseInt(parts[1], -1);
                    int y = tryParseInt(parts[2], -1);
                    if (d < 1 || m < 1 || y < 1900) continue;

                    int month0 = m - 1;
                    for (int i = 0; i < ranges.size(); i++) {
                        MonthRange mr = ranges.get(i);
                        if (mr.year == y && mr.month0 == month0) {
                            consumed[i] += getDoubleOrZero(child, "dailyCalorieConsumed");
                            burned[i] += getDoubleOrZero(child, "dailyCalorieBurned");
                            maxCal[i] += getDoubleOrZero(child, "dailyMaximumCalorie"); // sum instead of max
                            minCal[i] += getDoubleOrZero(child, "dailyMinimumCalorie"); // sum instead of min
                            any = true;
                        }
                    }
                }

                if (!any) {
                    showNoRecords(true);
                    clearCharts();
                    return;
                }

                List<String> labels = new ArrayList<>();
                for (MonthRange mr : ranges) labels.add(mr.monthLabel);

                plotLineChart(chartConsumed, labels, consumed, maxCal, minCal, "Consumed", "Maximum", "Minimum");
                plotLineChart(chartBurned, labels, burned, maxCal, minCal, "Burned", "Maximum", "Minimum");
                showNoRecords(false);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportDetailsActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearCharts() {
        chartConsumed.clear();
        chartConsumed.invalidate();
        chartBurned.clear();
        chartBurned.invalidate();
    }

    private void showNoRecords(boolean show) {
        tvNoRecord.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void plotLineChart(LineChart chart, List<String> xLabels, double[] series, double[] maxSeries, double[] minSeries,
                               String labelSeries, String labelMax, String labelMin) {

        int n = xLabels.size();
        List<Entry> entriesSeries = new ArrayList<>();
        List<Entry> entriesMax = new ArrayList<>();
        List<Entry> entriesMin = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            float v = i < series.length ? (float) series[i] : 0f;
            float m = i < maxSeries.length ? (float) maxSeries[i] : 0f;
            float mn = i < minSeries.length ? (float) minSeries[i] : 0f;
            entriesSeries.add(new Entry(i, v));
            entriesMax.add(new Entry(i, m));
            entriesMin.add(new Entry(i, mn));
        }

        LineDataSet dsSeries = new LineDataSet(entriesSeries, labelSeries);
        dsSeries.setColor(0xFFe53935);
        dsSeries.setCircleColor(0xFFe53935);

        LineDataSet dsMax = new LineDataSet(entriesMax, labelMax);
        dsMax.setColor(0xFF1976d2);
        dsMax.setCircleColor(0xFF1976d2);

        LineDataSet dsMin = new LineDataSet(entriesMin, labelMin);
        dsMin.setColor(0xFF43A047);
        dsMin.setCircleColor(0xFF43A047);

        LineData data = new LineData(dsMax, dsMin, dsSeries);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);

        chart.getLegend().setEnabled(true);
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    private double getDoubleOrZero(DataSnapshot node, String key) {
        if (node == null || !node.hasChild(key)) return 0.0;
        Object o = node.child(key).getValue();
        if (o == null) return 0.0;
        try {
            if (o instanceof Number) return ((Number) o).doubleValue();
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int tryParseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }
}
