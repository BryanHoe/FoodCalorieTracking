package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AppFeaturesActivity extends AppCompatActivity {

    CardView btnBackBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_features);

        btnBackBox = findViewById(R.id.btnBackBox);

        // Back to User Manual
        btnBackBox.setOnClickListener(v ->
                startActivity(new Intent(AppFeaturesActivity.this, UserManualActivity.class)));
    }
}