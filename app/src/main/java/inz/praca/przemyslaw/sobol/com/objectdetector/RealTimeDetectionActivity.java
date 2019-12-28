package inz.praca.przemyslaw.sobol.com.objectdetector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class RealTimeDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "BodyDetector";
    private CameraBridgeViewBase OpenCvCameraView;

    private volatile Rect[] numberOfDetectedObjects1;
    private volatile Rect[] numberOfDetectedObjects2;
    private volatile Mat mRgba;
    private volatile Mat frame;

    private CascadeClassifier cascadeClassifier1;
    private File cascadeFile1;

    private CascadeClassifier cascadeClassifier2;
    private File cascadeFile2;

    Point point = new Point();

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                OpenCvCameraView.enableView();
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
        OpenCvCameraView = findViewById(R.id.main_surface);
        loadHaarCascadeFile1("haarcascade_frontalface_alt", R.raw.haarcascade_frontalface_alt);
        loadHaarCascadeFile2("haarcascade_eye", R.raw.haarcascade_eye);

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
        OpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        OpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        OpenCvCameraView.setCvCameraViewListener(this);
        OpenCvCameraView.setMaxFrameSize(900, 600);
    }

    private void loadHaarCascadeFile1(String classifierName, int classifierPosition) {
        try {
            File cascadeDir = getDir(classifierName, Context.MODE_PRIVATE);
            cascadeFile1 = new File(cascadeDir, classifierName + ".xml");

            if (!cascadeFile1.exists()) {
                FileOutputStream os = new FileOutputStream(cascadeFile1);
                InputStream is = getResources().openRawResource(classifierPosition);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to load Haar Cascade file" + classifierName);
        }
    }

    private void loadHaarCascadeFile2(String classifierName, int classifierPosition) {
        try {
            File cascadeDir = getDir(classifierName, Context.MODE_PRIVATE);
            cascadeFile2 = new File(cascadeDir, classifierName + ".xml");

            if (!cascadeFile2.exists()) {
                FileOutputStream os = new FileOutputStream(cascadeFile2);
                InputStream is = getResources().openRawResource(classifierPosition);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to load Haar Cascade file" + classifierName);
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
        cascadeClassifier1 = new CascadeClassifier(cascadeFile1.getAbsolutePath());
        cascadeClassifier1.load(cascadeFile1.getAbsolutePath());

        cascadeClassifier2 = new CascadeClassifier(cascadeFile2.getAbsolutePath());
        cascadeClassifier2.load(cascadeFile2.getAbsolutePath());
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if(frame != null){
            frame.release();
        }
        frame = mRgba.clone();

        if (frame != null) {
            MatOfRect matOfRect1 = new MatOfRect();
            MatOfRect matOfRect2 = new MatOfRect();
            cascadeClassifier1.detectMultiScale(frame, matOfRect1, 1.2, 5, 0, new Size(80, 80));
            numberOfDetectedObjects1 = matOfRect1.toArray();
            if (numberOfDetectedObjects1.length > 0) {
                //Log.d("P1 numDetectedObjects", numberOfDetectedObjects.length+"");
                for (int i = 0; i < numberOfDetectedObjects1.length; i++) {
                    Imgproc.rectangle(frame, numberOfDetectedObjects1[i].tl(), numberOfDetectedObjects1[i].br(), new Scalar(255,0,0), 3);

                    point.x = numberOfDetectedObjects1[i].tl().x - 5.0;
                    point.y = numberOfDetectedObjects1[i].tl().y - 5.0;

                    Imgproc.putText(frame, "Face", point, 2, 1, new Scalar(255, 0, 0));
                }
            }
            cascadeClassifier2.detectMultiScale(frame, matOfRect2, 1.2, 5, 0, new Size(80, 80));
            numberOfDetectedObjects2 = matOfRect2.toArray();
            if (numberOfDetectedObjects2.length > 0) {
                //Log.d("P1 numDetectedObjects", numberOfDetectedObjects.length+"");
                for (int i = 0; i < numberOfDetectedObjects2.length; i++) {
                    Imgproc.rectangle(frame, numberOfDetectedObjects2[i].tl(), numberOfDetectedObjects2[i].br(), new Scalar(0,255,0), 3);

                    point.x = numberOfDetectedObjects2[i].tl().x - 5.0;
                    point.y = numberOfDetectedObjects2[i].tl().y - 5.0;

                    Imgproc.putText(frame, "Eye", point, 2, 1, new Scalar(0, 255, 0));
                }
            }
        }
        return frame;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    public void onDestroy() {
        super.onDestroy();
        disableCamera();
    }

    private void disableCamera() {
        if (OpenCvCameraView != null)
            OpenCvCameraView.disableView();
    }
}