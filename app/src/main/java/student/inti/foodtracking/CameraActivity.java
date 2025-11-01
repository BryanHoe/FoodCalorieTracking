package student.inti.foodtracking;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import student.inti.foodtracking.ml.Resnet50Food11;

public class CameraActivity extends AppCompatActivity {

    // UI components
    Button camera, gallery, btnYes, btnTryAgain, btnSearchWord, btnSearchManual;
    ImageView imageView;
    TextView result, tvBack, tvTitle;
    LinearLayout confirmationLayout;

    // Model input size and label
    int imageSize = 224;
    String detectedLabel = "";

    // Food class labels
    String[] classes = {
            "Bread", "Dairy Product", "Dessert", "Egg", "Fried Food",
            "Meat", "Noodles-Pasta", "Rice", "Seafood", "Soup", "Vegetable-Fruit"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Initialize views
        tvBack = findViewById(R.id.tvBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Food Detection");

        // Back button to HomeActivity
        tvBack.setOnClickListener(v -> {
            startActivity(new Intent(CameraActivity.this, HomeActivity.class));
            finish();
        });

        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);
        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        confirmationLayout = findViewById(R.id.confirmationLayout);
        btnYes = findViewById(R.id.btnYes);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        btnSearchWord = findViewById(R.id.btnSearchWord);
        btnSearchManual = findViewById(R.id.btnSearchManual);

        // Set button colors (light gray)
        int lightGray = 0xFFD3D3D3;
        btnYes.setBackgroundColor(lightGray);
        btnTryAgain.setBackgroundColor(lightGray);
        btnSearchWord.setBackgroundColor(lightGray);
        btnSearchManual.setBackgroundColor(lightGray);
        camera.setBackgroundColor(lightGray);
        gallery.setBackgroundColor(lightGray);

        // Manual search button to SearchManuallyActivity
        btnSearchManual.setOnClickListener(v ->
                startActivity(new Intent(CameraActivity.this, SearchManuallyActivity.class))
        );

        // Open camera when button clicked
        camera.setOnClickListener(view -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 3);
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        });

        // Open gallery when button clicked
        gallery.setOnClickListener(view -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, 1);
        });

        // Confirm detection to LogFoodActivity
        btnYes.setOnClickListener(v -> {
            if (!detectedLabel.isEmpty()) {
                String mappedCategory = mapToFirebaseCategory(detectedLabel);
                Intent intent = new Intent(CameraActivity.this, LogFoodActivity.class);
                intent.putExtra("category", mappedCategory);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No food detected!", Toast.LENGTH_SHORT).show();
            }
        });

        // Try again → reset detection
        btnTryAgain.setOnClickListener(v -> {
            result.setText("");
            detectedLabel = "";
            imageView.setImageDrawable(null);
            confirmationLayout.setVisibility(LinearLayout.GONE);
            Toast.makeText(this, "Please capture or choose again.", Toast.LENGTH_SHORT).show();
        });

        // Manual search by word
        btnSearchWord.setOnClickListener(v ->
                startActivity(new Intent(CameraActivity.this, SearchManuallyActivity.class))
        );
    }

    // Map detected labels to Firebase category names
    private String mapToFirebaseCategory(String label) {
        switch (label) {
            case "Bread":
                return "Bread";
            case "Dairy Product":
                return "Dairy";
            case "Dessert":
                return "Dessert";
            case "Egg":
                return "Egg";
            case "Fried Food":
                return "Fried Food";
            case "Meat":
                return "Meat";
            case "Noodles-Pasta":
                return "Noodles-Pasta";
            case "Rice":
                return "Rice";
            case "Seafood":
                return "Seafood";
            case "Soup":
                return "Soup";
            case "Vegetable-Fruit":
                return "Vegetable-Fruit";
            default:
                return label;
        }
    }

    // Classify image using TensorFlow Lite model
    public void classifyImage(Bitmap image) {
        try {
            Resnet50Food11 model = Resnet50Food11.newInstance(getApplicationContext());

            // Prepare input buffer
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(
                    new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.f);
                    byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.f);
                    byteBuffer.putFloat((val & 0xFF) / 255.f);
                }
            }
            inputFeature0.loadBuffer(byteBuffer);

            // Run model inference
            Resnet50Food11.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Get label with highest confidence
            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }

            // Display result
            detectedLabel = classes[maxPos];
            result.setText(detectedLabel + " (" + String.format("%.1f", maxConfidence * 100) + "%)");
            confirmationLayout.setVisibility(LinearLayout.VISIBLE);

            model.close();
        } catch (IOException e) {
            Toast.makeText(this, "Model error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Handle image capture or gallery selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap image = null;

            // From camera
            if (requestCode == 3) {
                image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            }
            // From gallery
            else if (requestCode == 1) {
                Uri dat = data.getData();
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Process image
            if (image != null) {
                imageView.setImageBitmap(image);
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
        }
    }
}