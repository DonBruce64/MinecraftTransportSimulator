
// The light pass's light over the picture (DynamicLightRenderer): drawn twice from one light_pass result, once blended
// (dst colour, one) so a lit wall keeps its texture, once added flat so a dark surface takes the light's colour; and
// the lamps' beams (lamp_beam, drawn smaller), added and scaled up.

uniform sampler2D Sampler0; // light_pass's light, unclamped
uniform float lightScale;
uniform float sourceScale; // screen pixels per source pixel

void main() {
    // Alpha 0 leaves the target's alpha.
    vec2 uv = gl_FragCoord.xy / (vec2(textureSize(Sampler0, 0)) * sourceScale);
    fragColor = vec4(texture(Sampler0, uv).rgb * lightScale, 0.0);
}
