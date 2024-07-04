precision mediump float;
varying vec3 vNormal;

void main() {
    vec3 lightDir = normalize(vec3(1.0, 1.0, -1.0));
    float diff = max(dot(normalize(vNormal), lightDir), 0.0);
    gl_FragColor = vec4(diff, diff, diff, 1.0);
}