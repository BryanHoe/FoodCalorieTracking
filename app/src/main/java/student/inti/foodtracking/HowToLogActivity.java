package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HowToLogActivity extends AppCompatActivity {

    CardView btnBackBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_log);

        btnBackBox = findViewById(R.id.btnBackBox);

        // Back button
        btnBackBox.setOnClickListener(v -> {
            startActivity(new Intent(HowToLogActivity.this, UserManualActivity.class));
            finish();
        });
    }
}