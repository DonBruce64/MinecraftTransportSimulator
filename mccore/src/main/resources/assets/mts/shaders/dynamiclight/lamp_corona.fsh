
// Lamp coronas (DynamicLightCoronas): a hot core in a soft halo at each lit lamp, over the finished picture. A lamp shows
// only where the copied scene depth at its lens point (on the line from the lamp to the camera, so at the lamp on
// screen: where that line crosses a spot's lens face, a point light's bulb face) lies behind that point, so a hull
// between the lens and the camera hides it whole. Additive (one, one), set by
// DynamicLightRenderer.

uniform sampler2D Sampler0; // copied scene depth
// DynamicLightRenderer's corona texture, a row a corona (DynamicLightCoronas.TEXELS): 0 the lamp's screen uv (xy) and
// its size (w, a share of the screen height); 1 its lens point's screen uv and window depth (z); 2 its colour times its
// level.
uniform sampler2D Sampler3;

uniform int coronaCount;
uniform float aspect;        // width / height

VARYING vec2 texCoord;

// The share of five depth taps around the lamp that do not cover it.
float shown(vec4 at) {
    vec2 px = 1.5 / vec2(textureSize(Sampler0, 0));
    float v = step(at.z, texture(Sampler0, at.xy).r);
    v += step(at.z, texture(Sampler0, at.xy + vec2(px.x, 0.0)).r);
    v += step(at.z, texture(Sampler0, at.xy - vec2(px.x, 0.0)).r);
    v += step(at.z, texture(Sampler0, at.xy + vec2(0.0, px.y)).r);
    v += step(at.z, texture(Sampler0, at.xy - vec2(0.0, px.y)).r);
    return v * 0.2;
}

void main() {
    vec3 sum = vec3(0.0);
    for (int i = 0; i < coronaCount; i++) {
        vec4 at = texelFetch(Sampler3, ivec2(0, i), 0);
        // In sizes, square on screen.
        vec2 d = (texCoord - at.xy) / at.w;
        d.x *= aspect;
        float r2 = dot(d, d);
        if (r2 >= 2.56) continue;
        float vis = shown(texelFetch(Sampler3, ivec2(1, i), 0));
        if (vis <= 0.0) continue;
        float r = sqrt(r2);
        float core = exp(-r2 * 30.0);
        float halo = 0.45 / (1.0 + r2 * 10.0) * (1.0 - smoothstep(0.6, 1.6, r));
        sum += texelFetch(Sampler3, ivec2(2, i), 0).rgb * vis * (core + halo);
    }
    fragColor = vec4(sum, 1.0);
}
