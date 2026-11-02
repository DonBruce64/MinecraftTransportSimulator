
// The lamps' beams seen in the air (DynamicLightBeams): each view ray integrates the air it crosses inside a lamp's cone,
// up to the scene's depth, skipping what the lamp's shadow tile (DynamicLightShadowAtlas) shows hidden from it, so a body or
// a wall in the beam leaves a dark wake. Per unit length the air shows density * along / radius: the open cone mesh it
// replaces (both faces, each density * along * facing) integrated. Drawn at 1/beamScale of the screen into its own
// target, then added over the picture (light_composite), by DynamicLightRenderer.

uniform sampler2D Sampler0; // copied scene depth
uniform sampler2D Sampler2; // DynamicLightShadowAtlas' depth atlas, 8 x 8 tiles
// DynamicLightRenderer's beam texture, a row a beam (DynamicLightBeams.TEXELS): 0 the lens, camera-relative, and the
// beam's length; 1 its axis and the tan of its half angle; 2 its colour times its density and its shadow tile (a cube's
// first face plus CUBE, or -1); 3 its tile's lamp as the tile was drawn, camera-relative, and the tan of half its view;
// 4 its tile's axis as drawn and its far plane.
uniform sampler2D Sampler3;

uniform mat4 invViewProj;
uniform int beamCount;
uniform float beamScale;    // screen pixels per beam pixel

const int SAMPLES = 32;
// DynamicLightSystem.s beam: its radius at the lens, blocks.
const float BEAM_START = 0.06;
const float SHADOW_NEAR = 0.05;
const float SHADOW_BIAS = 0.08;
// DynamicLightShadowAtlas: a cube light's code offset; light_pass.fsh's FACE_TAN.
const int CUBE = 1000;
const float FACE_TAN = 1.1106125;

vec4 beamTexel(int beam, int texel) {
    return texelFetch(Sampler3, ivec2(texel, beam), 0);
}

vec3 reconstructPos(vec2 uv, float depth) {
    vec4 h = invViewProj * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return h.xyz / h.w;
}

// A tile's view: DynamicLightShadowAtlas#place's lookAt from the lamp along f.
mat3 tileBasis(vec3 f) {
    vec3 up = abs(f.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 s = normalize(cross(f, up));
    vec3 u = cross(s, f);
    return transpose(mat3(s, u, -f));
}

// Whether the tile at origin (size texels), seen from the lens at p through basis, holds something nearer than x.
bool hidden(vec3 x, mat3 basis, ivec2 origin, ivec2 size, vec3 p, vec2 tile) {
    vec3 l = basis * (x - p);
    float w = -l.z;
    if (w <= SHADOW_NEAR) return false;
    vec2 uv = l.xy / (w * tile.x) * 0.5 + 0.5;
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return false;
    float z = texelFetch(Sampler2, origin + ivec2(clamp(floor(uv * vec2(size)), vec2(0.0), vec2(size - 1))), 0).r * 2.0 - 1.0;
    float far = tile.y;
    float body = 2.0 * SHADOW_NEAR * far / (far + SHADOW_NEAR - z * (far - SHADOW_NEAR));
    return body < w - SHADOW_BIAS;
}

// Whether light b's tile hides x: a spot's own tile, or a cube's face that x lies in (a point light, or a spot too
// wide for one tile), each through the view it was drawn with.
bool beamHidden(vec3 x, int slot, mat3 basis, vec3 lamp, vec2 tile, ivec2 tileSize) {
    if (slot >= CUBE) {
        vec3 d = x - lamp;
        vec3 a = abs(d);
        int face = a.x >= a.y && a.x >= a.z ? (d.x > 0.0 ? 0 : 1) : a.y >= a.z ? (d.y > 0.0 ? 2 : 3) : (d.z > 0.0 ? 4 : 5);
        vec3 axis = face < 2 ? vec3(sign(d.x), 0.0, 0.0) : face < 4 ? vec3(0.0, sign(d.y), 0.0) : vec3(0.0, 0.0, sign(d.z));
        int s = slot - CUBE + face;
        return hidden(x, tileBasis(axis), ivec2(s % 8, s / 8) * tileSize, tileSize / 2, lamp, vec2(FACE_TAN, tile.y));
    }
    return hidden(x, basis, ivec2(slot % 8, slot / 8) * tileSize, tileSize, lamp, tile);
}

void main() {
    // This pixel's block of screen pixels, at its nearest depth: a beam stops at the nearer edge of a body.
    ivec2 screen = textureSize(Sampler0, 0);
    int k = int(beamScale + 0.5);
    ivec2 px = ivec2(gl_FragCoord.xy - 0.5) * k;
    vec2 uv = (vec2(px) + 0.5 * float(k)) / vec2(screen);
    float depth = 1.0;
    for (int j = 0; j < k; j++) {
        for (int i = 0; i < k; i++) {
            depth = min(depth, texelFetch(Sampler0, ivec2(min(vec2(px + ivec2(i, j)), vec2(screen - 1))), 0).r);
        }
    }
    ivec2 tileSize = textureSize(Sampler2, 0) / 8;
    vec3 scene = reconstructPos(uv, min(depth, 0.9999));
    float dist = depth >= 0.9999 ? 1.0e4 : length(scene);
    vec3 v = normalize(scene);
    // A fixed per-pixel offset into the steps: no bands where a wake begins.
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    vec3 sum = vec3(0.0);
    for (int b = 0; b < beamCount; b++) {
        vec4 at = beamTexel(b, 0);
        vec4 dir = beamTexel(b, 1);
        vec3 p = at.xyz;
        float len = at.w;
        vec3 d = dir.xyz;
        float tn = dir.w;
        // The ray against the cylinder round the cone, then the cone's own length.
        float radius = BEAM_START + len * tn;
        vec3 w = -p;
        float vd = dot(v, d);
        float wd = dot(w, d);
        vec3 vp = v - vd * d;
        vec3 wp = w - wd * d;
        float qa = dot(vp, vp);
        float qb = 2.0 * dot(vp, wp);
        float qc = dot(wp, wp) - radius * radius;
        float t0 = 0.0;
        float t1 = dist;
        if (qa > 1e-6) {
            float disc = qb * qb - 4.0 * qa * qc;
            if (disc <= 0.0) continue;
            float sq = sqrt(disc);
            t0 = (-qb - sq) / (2.0 * qa);
            t1 = (-qb + sq) / (2.0 * qa);
        } else if (qc > 0.0) {
            continue;
        }
        if (abs(vd) > 1e-6) {
            float ta = -wd / vd;
            float tb = (len - wd) / vd;
            t0 = max(t0, min(ta, tb));
            t1 = min(t1, max(ta, tb));
        } else if (wd < 0.0 || wd > len) {
            continue;
        }
        t0 = max(t0, 0.0);
        t1 = min(t1, dist);
        if (t1 <= t0) continue;
        float dt = (t1 - t0) / float(SAMPLES);
        vec4 color = beamTexel(b, 2);
        vec4 tileAt = beamTexel(b, 3);
        vec4 tileDir = beamTexel(b, 4);
        int slot = int(color.w);
        // The tile through the view it was drawn with: it is kept while its lamp barely turns.
        mat3 basis = tileBasis(tileDir.xyz);
        vec3 tileLamp = tileAt.xyz;
        vec2 tile = vec2(tileAt.w, tileDir.w);
        float air = 0.0;
        for (int i = 0; i < SAMPLES; i++) {
            float t = t0 + (float(i) + jitter) * dt;
            vec3 x = v * t;
            vec3 q = x - p;
            float h = dot(q, d);
            float rad = BEAM_START + max(h, 0.0) * tn;
            float r = length(q - h * d);
            float inside = 1.0 - smoothstep(rad * 0.85, rad, r);
            if (inside <= 0.0) continue;
            float s = clamp(h / len, 0.0, 1.0);
            if (slot >= 0 && beamHidden(x, slot, basis, tileLamp, tile, tileSize)) continue;
            // It fades near the camera, as the mesh did.
            air += (1.0 - s) * (1.0 - s) / rad * inside * min(1.0, t * 0.5);
        }
        sum += color.rgb * air * dt;
    }
    fragColor = vec4(sum, 0.0);
}
