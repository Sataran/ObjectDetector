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

    private volatile Mat mRgba;
    private volatile Mat frame;

    private CascadeClassifier eyeClassifier;
    private CascadeClassifier eyeGlassesClassifier;
    private CascadeClassifier faceClassifier;
    private CascadeClassifier lowerBodyClassifier;
    private CascadeClassifier upperBodyClassifier;

    private File eyeClassifierFile;
    private File eyeGlassesClassifierFile;
    private File faceClassifierFile;
    private File lowerBodyClassifierFile;
    private File upperBodyClassifierFile;


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
        init();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_real_time_detection);
        OpenCvCameraView = findViewById(R.id.main_surface);
        loadHaarCascadeFile(eyeClassifierFile,"haarcascade_eye", R.raw.haarcascade_eye);
        loadHaarCascadeFile(eyeGlassesClassifierFile,"haarcascade_eyeglasses", R.raw.haarcascade_eyeglasses);
        loadHaarCascadeFile(faceClassifierFile,"haarcascade_frontalface_alt", R.raw.haarcascade_frontalface_alt);
        loadHaarCascadeFile(lowerBodyClassifierFile,"haarcascade_lowerbody", R.raw.haarcascade_lowerbody);
        loadHaarCascadeFile(upperBodyClassifierFile,"haarcascade_upperbody", R.raw.haarcascade_upperbody);


        checkPermissions();
    }

    private void init(){
        File eyeDir = getDir("haarcascade_eye", Context.MODE_PRIVATE);
        eyeClassifierFile = new File(eyeDir, "haarcascade_eye.xml");

        File eyeGlassesDir = getDir("haarcascade_eyeglasses", Context.MODE_PRIVATE);
        eyeGlassesClassifierFile = new File(eyeGlassesDir, "haarcascade_eyeglasses.xml");

        File faceDir = getDir("haarcascade_frontalface_alt", Context.MODE_PRIVATE);
        faceClassifierFile = new File(faceDir, "haarcascade_frontalface_alt.xml");

        File lowerBodyDir = getDir("haarcascade_lowerbody", Context.MODE_PRIVATE);
        lowerBodyClassifierFile = new File(lowerBodyDir, "haarcascade_lowerbody.xml");

        File upperBodyDir = getDir("haarcascade_upperbody", Context.MODE_PRIVATE);
        upperBodyClassifierFile = new File(upperBodyDir, "haarcascade_upperbody.xml");

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
        OpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        OpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        OpenCvCameraView.setCvCameraViewListener(this);
        OpenCvCameraView.setMaxFrameSize(900, 600);
    }

    private void loadHaarCascadeFile(File classifierName, String classifierXmlFileName, int classifierPosition) {
        try {
            if (!classifierName.exists()) {
                FileOutputStream os = new FileOutputStream(classifierName);
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
            throw new RuntimeException("Failed to load Haar Cascade file" + classifierXmlFileName);
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
        resumeOpenCv();
    }

    private void resumeOpenCv() {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
        eyeClassifier = new CascadeClassifier(eyeClassifierFile.getAbsolutePath());
        eyeClassifier.load(eyeClassifierFile.getAbsolutePath());

        eyeGlassesClassifier = new CascadeClassifier(eyeGlassesClassifierFile.getAbsolutePath());
        eyeGlassesClassifier.load(eyeGlassesClassifierFile.getAbsolutePath());

        faceClassifier = new CascadeClassifier(faceClassifierFile.getAbsolutePath());
        faceClassifier.load(faceClassifierFile.getAbsolutePath());

        lowerBodyClassifier = new CascadeClassifier(lowerBodyClassifierFile.getAbsolutePath());
        lowerBodyClassifier.load(lowerBodyClassifierFile.getAbsolutePath());

        upperBodyClassifier = new CascadeClassifier(upperBodyClassifierFile.getAbsolutePath());
        upperBodyClassifier.load(upperBodyClassifierFile.getAbsolutePath());

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if(frame != null){
            frame.release();
        }
        frame = mRgba.clone();

        if (frame != null) {
            MatOfRect matOfRect = new MatOfRect();

            detectObject(eyeClassifier, matOfRect, new Size(30, 30),"Eye", new Scalar(0, 255, 0));
            //detectObject(eyeGlassesClassifier, matOfRect, new Size(80, 80),"Eye", new Scalar(0, 0, 255));
            detectObject(faceClassifier, matOfRect, new Size(50, 50),"Face", new Scalar(255, 0, 0));
            detectObject(lowerBodyClassifier, matOfRect, new Size(50, 50),"Lower Body", new Scalar(0, 0, 255));
            detectObject(upperBodyClassifier, matOfRect, new Size(80, 80),"Upper Body", new Scalar(255, 255, 0));

        }
        return frame;
    }

    private void detectObject(CascadeClassifier cascadeClassifier, MatOfRect matOfRect,Size objectSize, String classifierDescription, Scalar frameColor){
        Rect[] numberOfDetectedObjects;

        cascadeClassifier.detectMultiScale(frame, matOfRect, 1.2, 5, 0, objectSize);
        numberOfDetectedObjects = matOfRect.toArray();
        if (numberOfDetectedObjects.length > 0) {
            for (int i = 0; i < numberOfDetectedObjects.length; i++) {
                Imgproc.rectangle(frame, numberOfDetectedObjects[i].tl(), numberOfDetectedObjects[i].br(), frameColor, 3);

                point.x = numberOfDetectedObjects[i].tl().x - 5.0;
                point.y = numberOfDetectedObjects[i].tl().y - 5.0;

                Imgproc.putText(frame, classifierDescription, point, 2, 1, frameColor);
            }
        }
        matOfRect.release();
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