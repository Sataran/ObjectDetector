package inz.praca.przemyslaw.sobol.com.objectdetector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.opencv.core.Core;


public class MainActivity extends AppCompatActivity {

    private Button buttonToRealTimeDetection;
    private Button buttonToPhotoDetection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonToRealTimeDetection = findViewById(R.id.real_time_detection_button);
        buttonToRealTimeDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRealTimeDetectionActivity();
            }
        });

        buttonToPhotoDetection = findViewById(R.id.photo_detection_button);
        buttonToPhotoDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPhotoDetectionActivity();
            }
        });
    }

    public void openRealTimeDetectionActivity(){
        Intent intent = new Intent(this, RealTimeDetectionActivity.class);
        startActivity(intent);
    }

    public void openPhotoDetectionActivity(){
        Intent intent = new Intent(this, PhotoDetectionActivity.class);
        startActivity(intent);
    }
}
