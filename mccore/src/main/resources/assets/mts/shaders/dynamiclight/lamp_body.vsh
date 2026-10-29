
// A body into a lamp's shadow tile (DynamicLightShadowAtlas): its depth seen from the lamp.

ATTRIBUTE vec3 Position;
// Where its texture is read, by the same attribute location as MC's UV0 in an IV vertex buffer (2).
ATTRIBUTE vec2 UV0;

uniform mat4 ModelViewMat; // into the lamp's view: the lamp at the origin
uniform mat4 ProjMat;

VARYING vec3 fromLamp;
VARYING vec2 texCoord;

void main() {
    vec4 v = ModelViewMat * vec4(Position, 1.0);
    fromLamp = v.xyz;
    texCoord = UV0;
    gl_Position = ProjMat * v;
}
