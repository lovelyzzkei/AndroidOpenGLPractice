uniform mat4 uMVPMatrix;
attribute vec4 aPosition;
attribute vec3 aNormal;
attribute vec2 aTexCoord;
varying vec3 vNormal;
varying vec2 vTexCoord;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vNormal = aNormal;
    vTexCoord = aTexCoord;
}