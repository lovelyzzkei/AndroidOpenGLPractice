package com.example.openglpractice;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    /* OPENGL VARIABLES */
    private GLSurfaceView glSurfaceView;
    private ObjRenderer renderer;

    /* CAMERA VARIABLES */
    private TextureView cameraPreview;
    private CameraManager cameraManager;
    private String cameraId;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice cameraDevice;
    private Size optimalSize;

    // Matrices for touch to 3D conversion
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] invertedProjectionMatrix = new float[16];
    private float[] invertedViewMatrix = new float[16];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = findViewById(R.id.cameraPreview);
        glSurfaceView = findViewById(R.id.glSurfaceView);

        // Make GLSurfaceView transparent
        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.setEGLContextClientVersion(2);

        renderer = new ObjRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);   // Only render when requested
        glSurfaceView.setOnTouchListener(this);

        // Set button listeners
        setButtonListeners();

        // Set camera
        setupCamera();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            renderer.setObjectPosition(x, y);
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    // =============== Camera setup functions ===============
    private void setupCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];

            // Get camera permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 100);
                return;
            }

            // Set optimal size of camera preview
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            optimalSize = previewSizes[13]; // (1920, 1080)
//            optimalSize = getOptimalPreviewSize(previewSizes, screenWidth, screenHeight);
            Log.i("APP", String.format("screenWidth: %d screenHeight: %d optimalSize.width: %d optimalSize.height: %d",
                    screenWidth, screenHeight, optimalSize.getWidth(), optimalSize.getHeight()));

            // Set the TextureView size to match the optimal preview size
            cameraPreview.setLayoutParams(new RelativeLayout.LayoutParams(screenWidth, screenHeight));


            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int i) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e("APP", "CameraAccessException");
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = cameraPreview.getSurfaceTexture();
            texture.setDefaultBufferSize(optimalSize.getWidth(), optimalSize.getHeight());
            Surface surface = new Surface(texture);

            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    cameraCaptureSession = session;
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    try {
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getOptimalPreviewSize(Size[] sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Size size : sizes) {
            Log.d("APP", String.format("Width: %d HEight: %d", size.getWidth(), size.getHeight()));
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - height) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - height);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - height);
                }
            }
        }
        return optimalSize;
    }


    // =============== Button listener function ===============
    public void setButtonListeners() {
        Button btnRotate = findViewById(R.id.btnRotate);
        btnRotate.setOnClickListener(v-> {
            renderer.setRotationAngle((renderer.getRotationAngle() + 45) % 360);
        });

        Button btnScale = findViewById(R.id.btnScale);
        btnScale.setOnClickListener(v -> {
            renderer.setScale(renderer.getScale() * 1.1f);
        });

        Button btnTranslateX = findViewById(R.id.btnTranslateX);
        btnTranslateX.setOnClickListener(v -> {
            float[] currentTranslation = renderer.getObjectPosition();
            renderer.setObjectPosition(currentTranslation[0] + 1f, currentTranslation[1], currentTranslation[2]);
        });

        Button btnTranslateY = findViewById(R.id.btnTranslateY);
        btnTranslateY.setOnClickListener(v -> {
            float[] currentTranslation = renderer.getObjectPosition();
            renderer.setObjectPosition(currentTranslation[0], currentTranslation[1] + 1f, currentTranslation[2]);
        });

        Button btnTranslateZ = findViewById(R.id.btnTranslateZ);
        btnTranslateZ.setOnClickListener(v -> {
            float[] currentTranslation = renderer.getObjectPosition();
            renderer.setObjectPosition(currentTranslation[0], currentTranslation[1], currentTranslation[2] + 1f);
        });

        Button btnTranslateNegX = findViewById(R.id.btnTranslateNegX);
        btnTranslateNegX.setOnClickListener(v -> {
            float[] currentTranslation = renderer.getObjectPosition();
            renderer.setObjectPosition(currentTranslation[0] - 1f, currentTranslation[1], currentTranslation[2]);
        });

        Button btnTranslateNegY = findViewById(R.id.btnTranslateNegY);
        btnTranslateNegY.setOnClickListener(v -> {
            float[] currentTranslation = renderer.getObjectPosition();
            renderer.setObjectPosition(currentTranslation[0], currentTranslation[1] - 1f, currentTranslation[2]);
        });

        Button btnTranslateNegZ = findViewById(R.id.btnTranslateNegZ);
        btnTranslateNegZ.setOnClickListener(v -> {
            float[] currentTranslation = renderer.getObjectPosition();
            renderer.setObjectPosition(currentTranslation[0], currentTranslation[1], currentTranslation[2] - 1f);
        });

    }
}
