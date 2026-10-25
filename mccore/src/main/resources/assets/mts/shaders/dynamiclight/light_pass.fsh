// Dynamic light pass. Reads a copy of the depth buffer taken after the world is drawn,
// reconstructs each pixel's camera-relative position and normal, and accumulates up to 256 point/spot lights (the
// budgeted DynamicLightSystem frame, capped by the DynL_MaxLights config), read from a float texture a row a light
// (DynamicLightFrame.lightData). One fullscreen draw: every pixel pays lightCount iterations of accumulate() however
// few pixels are lit.
//
// The result is the light alone, unclamped; light_composite lays it over the picture (DynamicLightRenderer).

uniform sampler2D Sampler0; // copied scene depth
uniform sampler2D Sampler2; // DynamicLightShadowAtlas: blocks' and bodies' depth from the lights, 8 x 8 tiles
// DynamicLightFrame.lightData, a row a light: 0 position (camera-relative) and radius; 1 color and intensity; 2 spot
// direction and cos of its outer half angle; 3 cos of its inner half angle, its shadow slot (a spot's tile; a
// point's first of six (+X -X +Y -Y +Z -Z) plus CUBE; or -1) and the clearance round the lamp where nothing shadows
// it (blocks); 4 its tile's lamp (camera-relative) and tan of half its view; 5 its tile's axis and far plane. A tile is read through the view it was drawn with, not the lamp's pose now.
uniform sampler2D Sampler3;

uniform mat4 invViewProj;   // undoes this frame's ProjMat*ModelViewMat (camera-relative space)
uniform int lightCount;

uniform int shadowsEnabled;         // 0: the DynL_shadows config is off, nothing is shadowed

#include voxel.glsl

vec3 reconstructPos(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 posH = invViewProj * ndc;
    return posH.xyz / posH.w;
}

float depthAt(ivec2 p) {
    return texelFetch(Sampler0, p, 0).r;
}

vec3 posAt(ivec2 p, float depth) {
    return reconstructPos((vec2(p) + 0.5) / vec2(textureSize(Sampler0, 0)), depth);
}

// Four steps of a 24-bit depth: what quantising leaves of a plane's second difference.
const float PLANE_EPS = 2.4e-7;

// The surface tangent along one screen axis, toward the neighbour on this pixel's own face.
// Window depth is affine in screen space across a plane. Where the face runs on k pixels both ways (8, 4, then 2) the
// tangent spans them all: far off the depth steps only a few millimetres from one pixel to the next, so a tangent to
// the next pixel strays by degrees, and light grazing the ground (headlights, wide cones) turns that into stripes.
// Nearer an edge, the side whose second difference is smaller did not cross it. dFdx/dFdy cannot choose: they span
// a fixed 2x2 quad, and on a face a few pixels wide that quad straddles a cube's edge, giving the next face's normal.
vec3 tangent(ivec2 c, float dc, vec3 pc, ivec2 axis) {
    ivec2 size = textureSize(Sampler0, 0);
    for (int k = 8; k >= 2; k /= 2) {
        ivec2 r = c + k * axis;
        ivec2 l = c - k * axis;
        if (any(greaterThanEqual(r, size)) || any(lessThan(l, ivec2(0)))) continue;
        float dr = depthAt(r);
        float dl = depthAt(l);
        float hr = depthAt(c + (k / 2) * axis);
        float hl = depthAt(c - (k / 2) * axis);
        float eps = PLANE_EPS + 0.02 * abs(dr - dl);
        if (abs(dr + dl - 2.0 * dc) < eps && abs(2.0 * hr - dc - dr) < eps && abs(2.0 * hl - dc - dl) < eps) {
            return posAt(r, dr) - posAt(l, dl);
        }
    }
    bool hasR = all(lessThan(c + 2 * axis, size));
    bool hasL = all(greaterThanEqual(c - 2 * axis, ivec2(0)));
    float r1 = hasR ? depthAt(c + axis) : 1.0;
    float l1 = hasL ? depthAt(c - axis) : 1.0;
    float errR = hasR ? abs(2.0 * r1 - depthAt(c + 2 * axis) - dc) : 1.0e9;
    float errL = hasL ? abs(2.0 * l1 - depthAt(c - 2 * axis) - dc) : 1.0e9;
    if (errR <= errL) return posAt(c + axis, r1) - pc;
    return pc - posAt(c - axis, l1);
}

// Whether a block stands between pos (camera-relative, already off its face) and the light: a grid walk
// (Amanatides-Woo) stopping short of the lamp by its clearance, so a lamp touching a block still lights. A block with a shape stops it
// only where the ray meets one of its boxes; pos's own block is tested so, as a stair's upper step shades its lower
// one, but never as a full block, whose face pos may lie on.
const int MARCH_STEPS = 160;
// A lamp's lens radius, blocks: the light comes from a disc, so a shadow's edge widens with its distance behind the
// thing that casts it.
const float LAMP_SIZE = 0.3;

bool blocked(vec3 pos, vec3 lightPos, float clearance) {
    vec3 a = pos + voxelCamFrac;
    vec3 d = lightPos - pos;
    float len = length(d) - clearance;
    if (len <= 0.0) return false;
    d /= len + clearance;
    ivec3 cell = ivec3(floor(a));
    ivec3 stepDir = ivec3(sign(d));
    vec3 inv = 1.0 / max(abs(d), vec3(1e-6));
    vec3 ray = voxelInverse(d);
    vec3 next = (vec3(cell) + max(vec3(stepDir), vec3(0.0)) - a) * sign(d) * inv;
    next = mix(next, vec3(1e9), vec3(equal(stepDir, ivec3(0))));
    float t = min(min(next.x, next.y), next.z);
    if (voxelAt(cell + voxelCamBlock) != 255 && voxelHit(cell, a, ray, 0.0, min(t, len)) >= 0.0) return true;
    for (int i = 0; i < MARCH_STEPS; i++) {
        t = min(min(next.x, next.y), next.z);
        if (t > len) return false;
        if (next.x <= t) { cell.x += stepDir.x; next.x += inv.x; }
        else if (next.y <= t) { cell.y += stepDir.y; next.y += inv.y; }
        else { cell.z += stepDir.z; next.z += inv.z; }
        if (voxelHit(cell, a, ray, t, min(min(min(next.x, next.y), next.z), len)) >= 0.0) return true;
    }
    return false;
}

// The share of the lamp's disc pos sees past the blocks: four rays to points round the lens, and only inside a
// shadow's edge four more between them.
float blockLight(vec3 pos, vec3 lightPos, float clearance) {
    vec3 d = normalize(lightPos - pos);
    vec3 u = normalize(cross(d, abs(d.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
    vec3 v = cross(d, u) * LAMP_SIZE;
    u *= LAMP_SIZE;
    float lit = 0.0;
    lit += blocked(pos, lightPos + u, clearance) ? 0.0 : 1.0;
    lit += blocked(pos, lightPos - u, clearance) ? 0.0 : 1.0;
    lit += blocked(pos, lightPos + v, clearance) ? 0.0 : 1.0;
    lit += blocked(pos, lightPos - v, clearance) ? 0.0 : 1.0;
    if (lit == 0.0 || lit == 4.0) return lit * 0.25;
    vec3 a = (u + v) * 0.7071;
    vec3 b = (u - v) * 0.7071;
    lit += blocked(pos, lightPos + a, clearance) ? 0.0 : 1.0;
    lit += blocked(pos, lightPos - a, clearance) ? 0.0 : 1.0;
    lit += blocked(pos, lightPos + b, clearance) ? 0.0 : 1.0;
    lit += blocked(pos, lightPos - b, clearance) ? 0.0 : 1.0;
    return lit * 0.125;
}

// DynamicLightShadowAtlas' near plane.
const float SHADOW_NEAR = 0.05;
const float SHADOW_BIAS = 0.05;

const vec2 DISC[12] = vec2[12](
    vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696, 0.457), vec2(-0.203, 0.621),
    vec2(0.962, -0.195), vec2(0.473, -0.480), vec2(0.519, 0.767), vec2(0.185, -0.893),
    vec2(0.507, 0.064), vec2(0.896, 0.412), vec2(-0.322, -0.933), vec2(-0.792, -0.598));

// The texel holding tile point t, as the tile traced it: through its centre. Clamped as floats: GLSL 1.20 has no
// integer clamp, and below 0 floor and the int cast meet the same bound.
vec2 texelCentre(vec2 t, ivec2 size) {
    return clamp(floor(t), vec2(0.0), vec2(size - 1)) + 0.5;
}

float tileDepth(ivec2 origin, ivec2 size, vec2 t, float far) {
    ivec2 q = ivec2(clamp(floor(t), vec2(0.0), vec2(size - 1)));
    float z = texelFetch(Sampler2, origin + q, 0).r * 2.0 - 1.0;
    return 2.0 * SHADOW_NEAR * far / (far + SHADOW_NEAR - z * (far - SHADOW_NEAR));
}

// A tile's view: DynamicLightShadowAtlas#place's lookAt from the lamp along f.
mat3 tileBasis(vec3 f) {
    vec3 up = abs(f.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 s = normalize(cross(f, up));
    vec3 u = cross(s, f);
    return transpose(mat3(s, u, -f));
}

// DynamicLightShadowAtlas: the atlas's tiles a row, a point light's code offset, a cube face's tan of half its view.
const int TILE_COLUMNS = 8;
const int CUBE = 1000;
const float FACE_TAN = 1.1106125;

// How far along tile s's texel ray at ndc the receiver's own plane (point pl, normal nl, lamp view) lies; -1 when the
// ray meets its back. A blocker is anything the tile holds nearer than that, so a surface never shades itself however
// grazing the light.
float planeDepth(vec2 ndc, vec3 pl, vec3 nl, float tanHalf) {
    vec3 r = vec3(ndc * tanHalf, -1.0);
    float nr = dot(nl, r);
    if (nr >= -1e-5) return -1.0;
    return dot(nl, pl) / nr;
}

// The share of the lamp's disc tile s (from lamp along axis) shows pos unhidden by a block or a body: those in
// front found first, then the edge filtered as wide as the lens would blur it at that gap (PCSS).
float tileLight(vec3 pos, vec3 normal, vec3 lamp, vec3 axis, float tanHalf, float far, int s, bool face) {
    mat3 basis = tileBasis(axis);
    vec3 pl = basis * (pos - lamp);
    vec3 nl = basis * normal;
    float w = -pl.z;
    if (w <= SHADOW_NEAR) return 1.0;
    vec2 uv = pl.xy / (w * tanHalf) * 0.5 + 0.5;
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return 1.0;
    ivec2 full = textureSize(Sampler2, 0) / TILE_COLUMNS;
    ivec2 origin = ivec2(s % TILE_COLUMNS, s / TILE_COLUMNS) * full;
    // DynamicLightShadowAtlas' FACE_TILE: a point light's face fills its tile's corner, half as wide.
    ivec2 size = face ? full / 2 : full;
    vec2 t = uv * vec2(size);
    // One texel's width in blocks at the receiver's distance.
    float texel = 2.0 * w * tanHalf / float(size.x);
    float search = clamp(LAMP_SIZE / texel, 2.0, 24.0);
    float blockers = 0.0;
    float found = 0.0;
    for (int i = 0; i < 12; i++) {
        vec2 q = texelCentre(t + DISC[i] * search, size);
        float plane = planeDepth(q / vec2(size) * 2.0 - 1.0, pl, nl, tanHalf);
        float body = tileDepth(origin, size, q, far);
        if (plane > 0.0 && body < plane - SHADOW_BIAS) {
            blockers += body;
            found += 1.0;
        }
    }
    if (found == 0.0) return 1.0;
    float blocker = blockers / found;
    float penumbra = LAMP_SIZE * (w - blocker) / max(blocker, SHADOW_NEAR);
    float radius = clamp(penumbra / texel, 1.0, 24.0);
    float lit = 0.0;
    for (int i = 0; i < 13; i++) {
        vec2 q = texelCentre(t + (i == 12 ? vec2(0.0) : DISC[i] * radius), size);
        float plane = planeDepth(q / vec2(size) * 2.0 - 1.0, pl, nl, tanHalf);
        lit += plane > 0.0 && tileDepth(origin, size, q, far) < plane - SHADOW_BIAS ? 0.0 : 1.0;
    }
    return lit / 13.0;
}

// DynamicLightFrame's MAX_LIGHTS (CAPACITY).
const int MAX_LIGHTS = 256;

vec4 lightTexel(int light, int texel) {
    return texelFetch(Sampler3, ivec2(texel, light), 0);
}

vec3 accumulate(vec3 pos, vec3 normal, int light) {
    vec4 posRadius = lightTexel(light, 0);
    float radius = posRadius.w;
    if (radius <= 0.0) return vec3(0.0);
    vec3 toLight = posRadius.xyz - pos;
    float dist = length(toLight);
    if (dist > radius) return vec3(0.0);
    vec4 colorIntensity = lightTexel(light, 1);
    vec4 dirCone = lightTexel(light, 2);
    vec4 coneShadow = lightTexel(light, 3);
    float coneInner = coneShadow.x;
    int shadow = int(coneShadow.y);
    float clearance = coneShadow.z;
    vec3 l = toLight / max(dist, 0.001);
    float ndotl = max(dot(normal, l), 0.0);
    float falloff = clamp(1.0 - dist / radius, 0.0, 1.0);
    falloff *= falloff;
    // DynamicLightAim#cone: lamp-to-surface direction against the spot's axis.
    float cone = smoothstep(dirCone.w, coneInner, dot(-l, dirCone.xyz));
    float k = colorIntensity.a * falloff * ndotl * cone;
    if (k <= 0.0) return vec3(0.0);
    if (shadowsEnabled == 0) return colorIntensity.rgb * k;
    if (shadow >= 0) {
        vec4 tileAt = lightTexel(light, 4);
        vec4 tileAxis = lightTexel(light, 5);
        if (shadow >= CUBE) {
            // A point light: the cube face the point lies in, seen from where its tiles were drawn.
            vec3 d = pos - tileAt.xyz;
            vec3 a = abs(d);
            int face = a.x >= a.y && a.x >= a.z ? (d.x > 0.0 ? 0 : 1) : a.y >= a.z ? (d.y > 0.0 ? 2 : 3) : (d.z > 0.0 ? 4 : 5);
            vec3 axis = face < 2 ? vec3(sign(d.x), 0.0, 0.0) : face < 4 ? vec3(0.0, sign(d.y), 0.0) : vec3(0.0, 0.0, sign(d.z));
            k *= tileLight(pos, normal, tileAt.xyz, axis, FACE_TAN, tileAxis.w, shadow - CUBE + face, true);
        } else {
            k *= tileLight(pos, normal, tileAt.xyz, tileAxis.xyz, tileAt.w, tileAxis.w, shadow, false);
        }
    } else {
        // Marched from just off the face, so the pixel's own block does not stop its light.
        k *= blockLight(pos + normal * 0.02, posRadius.xyz, clearance);
    }
    return colorIntensity.rgb * k;
}

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    float depth = depthAt(pixel);
    // Sky (or nothing copied yet) — contribute nothing rather than lighting the void.
    if (depth >= 0.9999) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 pos = posAt(pixel, depth);
    // A degenerate tangent (a one-pixel sliver) would normalize to NaN, and NaN blends to black.
    vec3 face = cross(tangent(pixel, depth, pos, ivec2(1, 0)), tangent(pixel, depth, pos, ivec2(0, 1)));
    vec3 normal = dot(face, face) > 1.0e-20 ? normalize(face) : normalize(-pos);
    // Camera sits at the origin in this camera-relative space, so -pos is the view direction.
    if (dot(normal, -pos) < 0.0) normal = -normal;

    vec3 accum = vec3(0.0);
    int count = lightCount < MAX_LIGHTS ? lightCount : MAX_LIGHTS;
    for (int i = 0; i < count; i++) {
        accum += accumulate(pos, normal, i);
    }

    // The light alone: light_composite lays it over the picture.
    fragColor = vec4(accum, 0.0);
}
