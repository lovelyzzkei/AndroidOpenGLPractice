package com.example.openglpractice;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class ObjRenderer implements GLSurfaceView.Renderer {
    private Context context;
    private Obj3D obj;

    private int programId;
    private int positionAttribute;
    private int normalAttribute;
    private int mvpMatrixUniform;
    private int texturePositionHandle, textureUniformHandle;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] vpMatrix = new float[16];


    private final float[] rotationMatrix = new float[16];
    private final float[] translateMatrix = new float[16];
    private final float[] scaleMatrix = new float[16];

    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer textureBuffer;

    // Properties of object
    private float rotationAngle = 0f;
    private float scale = 1f; // FIXED!!
    private final float[] objectPosition = {0f, 0f, 4f};
    private int screenWidth, screenHeight;
    private float aspectRatio;

    // Properties of camera
    private float[] cameraPosition = new float[3];
    private float[] cameraRotation = new float[3];

    private GLSurfaceView glSurfaceView;
    private int textureId;

    public ObjRenderer(Context context, GLSurfaceView glSurfaceView) {
        this.context = context;
        this.glSurfaceView = glSurfaceView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);    // Transparent
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Create Texture and bind
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wood_color);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Load and compile shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, readShaderFromAssets("vertex_shader.glsl"));
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, readShaderFromAssets("fragment_shader.glsl"));

        // Create and link shader program
        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        GLES20.glLinkProgram(programId);

        // Get attribute and uniform locations
        positionAttribute = GLES20.glGetAttribLocation(programId, "aPosition");
        normalAttribute = GLES20.glGetAttribLocation(programId, "aNormal");
        mvpMatrixUniform = GLES20.glGetUniformLocation(programId, "uMVPMatrix");
        texturePositionHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        textureUniformHandle = GLES20.glGetAttribLocation(programId, "sTexture");

        // Load the .obj file
        try {
            obj = ObjLoader.load(context.getAssets().open("desk2.obj"));
            setupBuffers();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set up view matrix
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 0f, 0f, 0f, 3f, 0f,1.0f, 0.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Log.i("APP", String.format("Width: %d Height: %d", width, height));
        setScreenSize(width, height);

        // Setup projection matrix
        float ratio = (float) width / height;
        // Use a symmetric frustum for the projection
        float near = 1f, far = 10f;
        float top = (float) Math.tan(Math.toRadians(60.0 / 2)) * near;
        float bottom = -top;
        float left = bottom * aspectRatio;
        float right = top * aspectRatio;

//        Matrix.perspectiveM(projectionMatrix,0 , 45.0f, ratio, 0.1f, 7);
//        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 0.2f, 7);
        Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(programId);

        // Draw the object
        // Set up model matrix
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(translateMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setIdentityM(scaleMatrix, 0);

        Matrix.translateM(translateMatrix, 0, objectPosition[0], objectPosition[1], objectPosition[2]);
        Matrix.setRotateM(rotationMatrix, 0, rotationAngle, 1.0f, 0, 0f);
        Matrix.scaleM(scaleMatrix, 0, scale, scale, scale);

        // Scaling first, then rotation, and finally translation (!!ORDER IS IMPORTANT!!)
        float[] tmpMatrix = new float[16];
        Matrix.multiplyMM(tmpMatrix, 0, rotationMatrix, 0, scaleMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, translateMatrix, 0, tmpMatrix, 0);

//        Log.i("APP", String.format("X: %f Y: %f Z: %f", objectPosition[0], objectPosition[1], objectPosition[2]));
//        Log.i("APP", String.format("azimuth: %f pitch: %f roll: %f", azimuth, pitch, roll));

        // Update view matrix based on camera position and rotation
//        Matrix.setIdentityM(viewMatrix, 0);
//        Matrix.rotateM(viewMatrix, 0, cameraRotation[0], 1, 0, 0);
//        Matrix.rotateM(viewMatrix, 0, cameraRotation[1], 0, 1, 0);
//        Matrix.rotateM(viewMatrix, 0, cameraRotation[2], 0, 0, 1);
//        Matrix.translateM(viewMatrix, 0, -cameraPosition[0], -cameraPosition[1], -cameraPosition[2]);

        Log.i("APP", Arrays.toString(viewMatrix));

        // Combine with view matrix
//        float[] combinedMatrix = new float[16];
//        Matrix.multiplyMM(combinedMatrix, 0, viewMatrix, 0, cameraMatrix, 0);

        // Calculate the MVPMatrix
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        // Pass the MVP matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);

        // Enable vertex attribute arrays
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glEnableVertexAttribArray(normalAttribute);
        GLES20.glEnableVertexAttribArray(texturePositionHandle);

        // Set the vertex attribute pointers
        GLES20.glVertexAttribPointer(positionAttribute, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(normalAttribute, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);
        GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);


        // Draw the object
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, obj.getVertexCount());

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(normalAttribute);
    }

    public void setRotationAngle(float angle) {
        this.rotationAngle = angle;
        requestRender();
    }

    public void setScale(float scale) {
        this.scale = scale;
        requestRender();
    }
    public void setObjectPosition(float x, float y, float z) {
        this.objectPosition[0] = x;
        this.objectPosition[1] = y;
        this.objectPosition[2] = z;
        requestRender();
    }

    public void setObjectPosition(float x, float y) {
        // Auxiliary matrix and vectors to deal with openGL
        float[] invertedMatrix = new float[16];
        float[] transformMatrix = new float[16];
        float[] normalizedInPoint = new float[4];
        float[] outPoint = new float[4];

        // Invert y coordinate, as android uses top-left, and openGL bottom-left
        int oglTouchY = (int) (screenHeight - y);

        // Generate random number between 1 and 10
        int randomDepth = (int) (Math.random() * 6) + 1;

        // Transform the screen point to clip space in openGL (-1, 1)
        normalizedInPoint[0] = (float) ((2f * x / screenWidth - 1f));
        normalizedInPoint[1] = (float) (2f * (oglTouchY) / screenHeight - 1f);
        normalizedInPoint[2] = -1.0f;
        normalizedInPoint[3] = 1.0f;

        // Obtain the transform matrix and then the inverse
        Log.i("APP", "Proj: " + Arrays.toString(projectionMatrix));
        Log.i("APP", "Model: " + Arrays.toString(viewMatrix));
        Matrix.multiplyMM(transformMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.invertM(invertedMatrix, 0, transformMatrix, 0);

        // Apply the inverse to the ponit in clip space
        Matrix.multiplyMV(outPoint, 0, invertedMatrix, 0, normalizedInPoint, 0);

        if (outPoint[3] == 0.0) {
            // Avoid division zero error
            Log.e("APP", "World coords ERROR!");
            objectPosition[0] = 0;
            objectPosition[1] = 0;
        }

        // Divide by 3rd component to find out the real position
        objectPosition[0] = (outPoint[0] / outPoint[3]) * randomDepth;
        objectPosition[1] = (outPoint[1] / outPoint[3]) * randomDepth;
        objectPosition[2] = randomDepth;

        requestRender();
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.aspectRatio = (float) width / height;
    }

    public void updateCameraPosition(float x, float y, float z, float rotX, float rotY, float rotZ) {
        cameraPosition[0] = x;
        cameraPosition[1] = y;
        cameraPosition[2] = z;
        cameraRotation[0] = rotX;
        cameraRotation[1] = rotY;
        cameraRotation[2] = rotZ;
        requestRender();
    }


    public float getRotationAngle() { return rotationAngle; }
    public float getScale() { return scale; }
    public float[] getObjectPosition() { return objectPosition; }
    public float[] getProjectionMatrix() { return projectionMatrix; }
    public float[] getViewMatrix() { return viewMatrix; }

    private void requestRender() {
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
        }
    }
    private void setupBuffers() {
        vertexBuffer = ByteBuffer.allocateDirect(obj.getVertices().length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(obj.getVertices()).position(0);

        normalBuffer = ByteBuffer.allocateDirect(obj.getNormals().length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        normalBuffer.put(obj.getNormals()).position(0);

        textureBuffer = ByteBuffer.allocateDirect(obj.getTextures().length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureBuffer.put(obj.getTextures()).position(0);

    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private String readShaderFromAssets(String fileName) {
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            return new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
