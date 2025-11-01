package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.*;

import java.util.*;

public class SearchManuallyActivity extends AppCompatActivity {

    SearchView searchView;
    ListView listView;
    CardView btnBackBox;

    DatabaseReference foodRef;
    FoodAdapter adapter;
    List<FoodItem> foodList = new ArrayList<>();
    List<String> displayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_manually);

        searchView = findViewById(R.id.searchView);
        listView = findViewById(R.id.listView);
        btnBackBox = findViewById(R.id.btnBackBox);

        adapter = new FoodAdapter(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        foodRef = FirebaseDatabase.getInstance().getReference("Food");

        //  Back button to Home
        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(SearchManuallyActivity.this, HomeActivity.class));
            finish();
        });

        loadFoods();

        //  Search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFoods(newText);
                return true;
            }
        });

        //  On Food Click → Pass to LogFoodActivity
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String foodName = adapter.getItem(position);

            for (FoodItem item : foodList) {
                if (item.foodName.equals(foodName)) {
                    Intent intent = new Intent(SearchManuallyActivity.this, LogFoodActivity.class);
                    intent.putExtra("category", item.category);
                    intent.putExtra("foodName", item.foodName);
                    intent.putExtra("serving", item.serving);
                    intent.putExtra("calories", String.valueOf(item.calories));
                    startActivity(intent);
                    finish();
                    break;
                }
            }
        });
    }

    // Load ALL foods from all categories
    private void loadFoods() {
        foodRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                foodList.clear();
                displayList.clear();

                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    String category = categorySnap.getKey();

                    for (DataSnapshot foodSnap : categorySnap.getChildren()) {
                        String foodName = foodSnap.getKey();
                        String serving = foodSnap.child("serving").getValue(String.class);
                        String calStr = foodSnap.child("calories").getValue().toString();

                        double calories = 0;
                        try {
                            calories = Double.parseDouble(calStr.replaceAll("[^0-9.]", ""));
                        } catch (Exception ignored) {}

                        FoodItem item = new FoodItem(category, foodName, serving, calories);
                        foodList.add(item);
                        displayList.add(foodName);
                    }
                }

                adapter.updateOriginalList(displayList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SearchManuallyActivity.this, "Error loading foods", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Filter foods by name OR category
    private void filterFoods(String query) {
        query = query.toLowerCase(Locale.ROOT).trim();
        List<String> filtered = new ArrayList<>();

        if (query.isEmpty()) {
            for (FoodItem item : foodList) {
                filtered.add(item.foodName);
            }
        } else {
            for (FoodItem item : foodList) {
                if (item.foodName.toLowerCase().contains(query) ||
                        item.category.toLowerCase().contains(query)) {
                    filtered.add(item.foodName);
                }
            }
        }

        displayList.clear();
        displayList.addAll(filtered);
        adapter.updateOriginalList(displayList);
        adapter.notifyDataSetChanged();
    }

    // Helper class for food
    static class FoodItem {
        String category, foodName, serving;
        double calories;
        public FoodItem(String category, String foodName, String serving, double calories) {
            this.category = category;
            this.foodName = foodName;
            this.serving = serving;
            this.calories = calories;
        }
    }

    // Custom ArrayAdapter with case-insensitive filter (not used for logic, just to display)
    static class FoodAdapter extends ArrayAdapter<String> {
        private List<String> originalList;
        private List<String> filteredList;

        public FoodAdapter(@NonNull android.content.Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            this.originalList = new ArrayList<>(objects);
            this.filteredList = objects;
        }

        public void updateOriginalList(List<String> newList) {
            this.originalList = new ArrayList<>(newList);
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public String getItem(int position) {
            return filteredList.get(position);
        }
    }
}