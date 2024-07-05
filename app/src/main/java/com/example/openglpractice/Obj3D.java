package com.example.openglpractice;

public class Obj3D {
    private float[] vertices, normals, textures;

    public Obj3D(float[] vertices, float[] normals, float[] textures) {
        this.vertices = vertices;
        this.normals = normals;
        this.textures = textures;
    }

    public float[] getVertices() {
        return vertices;
    }

    public float[] getNormals() {
        return normals;
    }

    public float[] getTextures() {
        return textures;
    }

    public int getVertexCount() {
        return vertices.length / 3;
    }
}
