
// One triangle covering the bound viewport, made from gl_VertexID alone (no vertex buffer), for every fullscreen
// dynamic light pass. texCoord runs 0..1 across the viewport.

VARYING vec2 texCoord;

void main() {
    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    texCoord = p;
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}
