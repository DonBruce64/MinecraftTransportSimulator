
// A depth texture into the bound target, texel for texel: a tile's blocks into the shadow atlas
// (DynamicLightShadowAtlas#copyBlocks), the world's depth for the light pass (DynamicLightRenderer#copyDepth).

uniform sampler2D Sampler0; // the depth to copy, laid out as the target

void main() {
    gl_FragDepth = texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).r;
    fragColor = vec4(0.0);
}
