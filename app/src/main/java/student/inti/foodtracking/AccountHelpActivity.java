package student.inti.foodtracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AccountHelpActivity extends AppCompatActivity {

    CardView btnBackBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_help);

        btnBackBox = findViewById(R.id.btnBackBox);

        // Back to User Manual
        btnBackBox.setOnClickListener(v ->
                startActivity(new Intent(AccountHelpActivity.this, UserManualActivity.class)));
    }
}