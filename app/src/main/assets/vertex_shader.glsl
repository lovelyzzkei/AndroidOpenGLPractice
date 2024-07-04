uniform mat4 uMVPMatrix;
attribute vec4 aPosition;
attribute vec3 aNormal;
varying vec3 vNormal;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vNormal = aNormal;
}