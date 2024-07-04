package com.example.openglpractice;

public class Obj3D {
    private float[] vertices;
    private float[] normals;

    public Obj3D(float[] vertices, float[] normals) {
        this.vertices = vertices;
        this.normals = normals;
    }

    public float[] getVertices() {
        return vertices;
    }

    public float[] getNormals() {
        return normals;
    }

    public int getVertexCount() {
        return vertices.length / 3;
    }
}
