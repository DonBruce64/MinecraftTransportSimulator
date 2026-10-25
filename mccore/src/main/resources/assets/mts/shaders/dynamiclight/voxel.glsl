// The block grid (DynamicLightVoxelWindow), shared by light_pass.fsh and lamp_trace.fsh through DynamicLightShaders'
// #include: 128 x 64 x 128 blocks round the camera, toroidal, a byte each in an R8 atlas (texelX/texelY): 0 nothing,
// 255 a full block, else the id of the block's shape in DynamicLightBlockShapes' table, a row of RGBA32F texels an id:
// up to SHAPE_BOXES boxes in block space, two texels each (its least corner, then its greatest), the first's w the row's
// box count.

uniform sampler2D Sampler1; // DynamicLightVoxelWindow
uniform sampler2D Sampler4; // DynamicLightBlockShapes
uniform ivec3 voxelCamBlock; // the camera's block
uniform vec3 voxelCamFrac;   // the camera within it
uniform ivec3 voxelMin;      // the grid window's least block

// DynamicLightBlockShapes.MAX_BOXES.
const int SHAPE_BOXES = 8;

// Block b's byte: 0 nothing (or outside the window), 255 a full block, else its shape's id.
int voxelAt(ivec3 b) {
    ivec3 r = b - voxelMin;
    if (any(lessThan(r, ivec3(0))) || any(greaterThanEqual(r, ivec3(128, 64, 128)))) return 0;
    ivec3 m = b & ivec3(127, 63, 127);
    return int(texelFetch(Sampler1, ivec2((m.y & 7) * 128 + m.x, (m.y >> 3) * 128 + m.z), 0).r * 255.0 + 0.5);
}

// 1 / d with no zero component, for voxelHit.
vec3 voxelInverse(vec3 d) {
    return 1.0 / mix(d, vec3(1e-7), vec3(equal(d, vec3(0.0))));
}

// Where the ray o + d * t (o relative to the camera's block, inv = voxelInverse(d)) first meets what block cell (relative
// to the camera's block) holds within t0 .. t1: t0 for a full block, the nearest of its shape's boxes otherwise; -1 if
// it meets nothing.
float voxelHit(ivec3 cell, vec3 o, vec3 inv, float t0, float t1) {
    int v = voxelAt(cell + voxelCamBlock);
    if (v == 0) return -1.0;
    if (v == 255) return t0;
    float boxes = texelFetch(Sampler4, ivec2(0, v), 0).w;
    float hit = -1.0;
    for (int i = 0; i < SHAPE_BOXES; i++) {
        if (float(i) >= boxes) break;
        vec3 lo = (vec3(cell) + texelFetch(Sampler4, ivec2(2 * i, v), 0).xyz - o) * inv;
        vec3 hi = (vec3(cell) + texelFetch(Sampler4, ivec2(2 * i + 1, v), 0).xyz - o) * inv;
        vec3 enters = min(lo, hi);
        vec3 leaves = max(lo, hi);
        float enter = max(max(enters.x, enters.y), max(enters.z, t0));
        float leave = min(min(leaves.x, leaves.y), min(leaves.z, t1));
        if (enter <= leave && (hit < 0.0 || enter < hit)) hit = enter;
    }
    return hit;
}
