package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class CalorieDetailsActivity extends AppCompatActivity {

    CardView btnBackBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calorie_details);

        btnBackBox = findViewById(R.id.btnBackBox);

        // Back to User Information Page
        btnBackBox.setOnClickListener(v ->
                startActivity(new Intent(CalorieDetailsActivity.this, UserInformationActivity.class)));
    }
}