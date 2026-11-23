
// What of a body lies within the lamp's clearance hides nothing: its own lens, the panel it sits on, whatever a
// shadowStartDistance keeps clear.  Nor does what its texture cuts away: a body shadows only where it shows, as its
// cutout draw discards the same texels.

uniform sampler2D Sampler0; // the body's texture; none bound reads opaque

VARYING vec3 fromLamp;
VARYING vec2 texCoord;

uniform float lampClearance; // the light's, blocks (DynamicLightEmitter#clearance): a lamp touching a body still lights
uniform float alphaCut;      // the run's, DynamicLightBody#runCut: 0.1 as MC's entity cutout shaders, 0.75 for alpha blended types

void main() {
    if (length(fromLamp) < lampClearance) discard;
    if (texture(Sampler0, texCoord).a < alphaCut) discard;
    fragColor = vec4(0.0);
}
