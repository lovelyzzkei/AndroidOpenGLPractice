package com.example.openglpractice;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private GLSurfaceView glSurfaceView;
    private ObjRenderer renderer;

    // Matrices for touch to 3D conversion
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] invertedProjectionMatrix = new float[16];
    private float[] invertedViewMatrix = new float[16];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);

        renderer = new ObjRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);   // Only render when requested
        glSurfaceView.setOnTouchListener(this);

        // Set button listeners
        setButtonListeners();
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
