
// One spot lamp's shadow tile from the blocks (DynamicLightShadowAtlas): each texel walks the block grid (voxel.glsl)
// from the lamp along its own ray and writes the depth where it first meets a full block or a box of a block's shape
// (a slab, stairs, a bell). Bodies are drawn into
// the same depth after it; the light pass reads the nearer of the two against the receiver's own plane. No hit writes
// nothing.

#include voxel.glsl

uniform mat4 lampInvViewProj; // tile ndc -> lamp-relative point
uniform vec3 lampPos;         // camera-relative
uniform vec3 lampForward;
uniform vec4 tile;            // xy its corner in the atlas, z its size, pixels; w its far plane, blocks
uniform vec2 lampDepth;       // the tile projection's m22, m32: window depth of a distance along lampForward
uniform float lampClearance;  // the light's, blocks (DynamicLightEmitter#clearance): a lamp touching a block still lights

const int STEPS = 256;

void main() {
    vec2 ndc = (gl_FragCoord.xy - tile.xy) / tile.z * 2.0 - 1.0;
    vec4 h = lampInvViewProj * vec4(ndc, 1.0, 1.0);
    vec3 d = normalize(h.xyz / h.w);
    float far = tile.w;
    // The ray from the lamp (t 0), relative to the camera's block, walked from the lens's clearance on.
    vec3 o = lampPos + voxelCamFrac;
    vec3 a = o + d * lampClearance;
    ivec3 stepDir = ivec3(sign(d));
    vec3 inv = 1.0 / max(abs(d), vec3(1e-6));
    vec3 ray = voxelInverse(d);
    ivec3 cell = ivec3(floor(a));
    vec3 next = lampClearance + (vec3(cell) + max(vec3(stepDir), vec3(0.0)) - a) * sign(d) * inv;
    next = mix(next, vec3(1e9), vec3(equal(stepDir, ivec3(0))));
    float t = lampClearance;
    float entry = -1.0;
    for (int i = 0; i < STEPS; i++) {
        float leave = min(min(next.x, next.y), next.z);
        float hit = voxelHit(cell, o, ray, t, min(leave, far));
        if (hit >= 0.0) {
            entry = hit;
            break;
        }
        t = leave;
        if (t > far) break;
        if (next.x <= t) { cell.x += stepDir.x; next.x += inv.x; }
        else if (next.y <= t) { cell.y += stepDir.y; next.y += inv.y; }
        else { cell.z += stepDir.z; next.z += inv.z; }
    }
    if (entry < 0.0) discard;
    float w = max(entry * dot(d, lampForward), 0.06);
    gl_FragDepth = clamp((-lampDepth.x + lampDepth.y / w) * 0.5 + 0.5, 0.0, 1.0);
    fragColor = vec4(0.0);
}
