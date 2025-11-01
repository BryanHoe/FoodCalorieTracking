package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.*;

import java.util.*;

public class SearchExerciseActivity extends AppCompatActivity {

    SearchView searchView;
    ListView listView;
    CardView btnBackBox;

    DatabaseReference exerciseRef;
    ExerciseAdapter adapter;
    List<String> exerciseList = new ArrayList<>();
    Map<String, ExerciseItem> exerciseMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_exercise);

        searchView = findViewById(R.id.searchView);
        listView = findViewById(R.id.listView);
        btnBackBox = findViewById(R.id.btnBackBox);

        adapter = new ExerciseAdapter(this, android.R.layout.simple_list_item_1, exerciseList);
        listView.setAdapter(adapter);

        exerciseRef = FirebaseDatabase.getInstance().getReference("Exercise");

        //  Back button to Home
        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(SearchExerciseActivity.this, HomeActivity.class));
            finish();
        });

        loadExercises();

        //  Search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        //  Select Exercise → LogExerciseActivity
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String exerciseName = adapter.getItem(position);
            ExerciseItem item = exerciseMap.get(exerciseName);

            if (item != null) {
                Intent intent = new Intent(SearchExerciseActivity.this, LogExerciseActivity.class);
                intent.putExtra("exerciseName", exerciseName);
                startActivity(intent);
                finish();
            }
        });
    }

    // Load ALL exercises
    private void loadExercises() {
        exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                exerciseList.clear();
                exerciseMap.clear();

                for (DataSnapshot exSnap : snapshot.getChildren()) {
                    String exName = exSnap.getKey();

                    if (exName != null) {
                        ExerciseItem item = new ExerciseItem(exName);
                        exerciseList.add(exName);
                        exerciseMap.put(exName, item);
                    }
                }

                adapter.updateOriginalList(exerciseList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SearchExerciseActivity.this, "Error loading exercises", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Simple container
    static class ExerciseItem {
        String exerciseName;
        public ExerciseItem(String exerciseName) {
            this.exerciseName = exerciseName;
        }
    }

    // Custom Adapter with proper filtering
    static class ExerciseAdapter extends ArrayAdapter<String> {
        private List<String> originalList;
        private List<String> filteredList;

        public ExerciseAdapter(@NonNull android.content.Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            this.originalList = new ArrayList<>(objects);
            this.filteredList = new ArrayList<>(objects);
        }

        public void updateOriginalList(List<String> newList) {
            this.originalList = new ArrayList<>(newList);
            this.filteredList = new ArrayList<>(newList);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public String getItem(int position) {
            return filteredList.get(position);
        }

        @NonNull
        @Override
        public android.widget.Filter getFilter() {
            return new android.widget.Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<String> results = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        results.addAll(originalList);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();
                        for (String item : originalList) {
                            if (item.toLowerCase().contains(filterPattern)) {
                                results.add(item);
                            }
                        }
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = results;
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredList.clear();
                    if (results.values != null) {
                        filteredList.addAll((List<String>) results.values);
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }
}