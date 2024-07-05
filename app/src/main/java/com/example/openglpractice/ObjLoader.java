package com.example.openglpractice;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ObjLoader {
    public static Obj3D load(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> textures = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\s+");
            if (parts.length == 0) continue;

            switch (parts[0]) {
                case "v":  // Vertex
                    vertices.add(Float.parseFloat(parts[1]));
                    vertices.add(Float.parseFloat(parts[2]));
                    vertices.add(Float.parseFloat(parts[3]));
                    break;
                case "vn":  // Normal
                    normals.add(Float.parseFloat(parts[1]));
                    normals.add(Float.parseFloat(parts[2]));
                    normals.add(Float.parseFloat(parts[3]));
                    break;
                case "vt":  // Texture
                    textures.add(Float.parseFloat(parts[1]));
                    textures.add(Float.parseFloat(parts[2]));
                    break;
                case "f":  // Face
                    for (int i = 1; i <= 3; i++) {
                        String[] vertexData = parts[i].split("/");
                        faces.add(Integer.parseInt(vertexData[0]) - 1);
                    }
                    break;
            }
        }

        reader.close();

        float[] vertexArray = new float[faces.size() * 3];
        float[] normalArray = new float[faces.size() * 3];
        float[] textureArray = new float[faces.size() * 2];

        for (int i = 0; i < faces.size(); i++) {
            int index = faces.get(i);
            vertexArray[i * 3] = vertices.get(index * 3);
            vertexArray[i * 3 + 1] = vertices.get(index * 3 + 1);
            vertexArray[i * 3 + 2] = vertices.get(index * 3 + 2);

            normalArray[i * 3] = normals.get(index * 3);
            normalArray[i * 3 + 1] = normals.get(index * 3 + 1);
            normalArray[i * 3 + 2] = normals.get(index * 3 + 2);

            textureArray[i * 2] = textures.get(index * 2);
            textureArray[i * 2 + 1] = textures.get(index * 2 + 1);
        }

        return new Obj3D(vertexArray, normalArray, textureArray);
    }
}
