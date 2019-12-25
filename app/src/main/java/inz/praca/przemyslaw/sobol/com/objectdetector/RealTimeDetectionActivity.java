package inz.praca.przemyslaw.sobol.com.objectdetector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class RealTimeDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, Runnable {

    private static final String TAG = "BodyDetector";
    private CameraBridgeViewBase cameraBridgeViewBase;

    private volatile boolean running = false;
    private volatile int qtdFaces;
    private volatile Mat matTmpProcessingFace;

    private CascadeClassifier cascadeClassifier;
    private File mCascadeFile;
    private TextView infoFaces;


    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                cameraBridgeViewBase.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_real_time_detection);
        infoFaces = findViewById(R.id.real_time_id);
        cameraBridgeViewBase = findViewById(R.id.main_surface);
        loadHaarCascadeFile();
        checkPermissions();
    }

    private void checkPermissions() {
        if (isPermissionGranted()) {
            loadCameraBridge();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    private boolean isPermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }

    private void loadCameraBridge() {
        cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
    }

    private void loadHaarCascadeFile() {
        try {
            File cascadeDir = getDir("haarcascade_frontalface_alt", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");

            if (!mCascadeFile.exists()) {
                FileOutputStream os = new FileOutputStream(mCascadeFile);
                InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to load Haar Cascade file");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        disableCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isPermissionGranted()) return;
        resumeOCV();
    }

    private void resumeOCV() {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        cascadeClassifier.load(mCascadeFile.getAbsolutePath());
        startFaceDetect();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (matTmpProcessingFace == null) {
            matTmpProcessingFace = inputFrame.gray();
        }
        return inputFrame.rgba();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    public void startFaceDetect() {
        if (running) return;
        new Thread(this).start();
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                if (matTmpProcessingFace != null) {
                    MatOfRect matOfRect = new MatOfRect();
                    cascadeClassifier.detectMultiScale(matTmpProcessingFace, matOfRect);
                    int newQtdFaces = matOfRect.toList().size();
                    if (qtdFaces != newQtdFaces) {
                        qtdFaces = newQtdFaces;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                infoFaces.setText(String.format(getString(R.string.faces_detects), qtdFaces));
                            }
                        });
                    }
                    Thread.sleep(500);//if you want an interval
                    matTmpProcessingFace = null;
                }
                Thread.sleep(50);
            } catch (Throwable t) {
                try {
                    Thread.sleep(10_000);
                } catch (Throwable tt) {
                }
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        disableCamera();
    }

    private void disableCamera() {
        running = false;
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }


}
