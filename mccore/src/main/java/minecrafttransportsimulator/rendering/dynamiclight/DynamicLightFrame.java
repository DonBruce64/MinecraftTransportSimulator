package minecrafttransportsimulator.rendering.dynamiclight;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything one frame's dynamic light pass draws, packed for {@link DynamicLightRenderer}: the budgeted
 * lights as rows of the light shader's data texture (camera-relative, intensity already dimmed by daylight at the
 * lamp),
 * the same lights as emitters for their shadow tiles, the beams and coronas, and the block grid sections
 * that changed this frame.  Filled by {@link DynamicLightSystem}, read by
 * {@link DynamicLightRenderer}.  Render thread only, reused every frame.
 */
public final class DynamicLightFrame {
    /**
     * Most lights one frame draws, and the top of the DynL_MaxLights config: the rows of the light shader's data
     * texture (light_pass.fsh's MAX_LIGHTS).
     */
    public static final int CAPACITY = 256;
    /**
     * RGBA texels per light in {@link #lightData}: 0 camera-relative position and radius; 1 color and dimmed intensity;
     * 2 spot direction and cos of its outer half angle (a point light gets (0, 0, -1, -2), which never cuts); 3 cos of
     * its inner half angle (-1 for a point light), its shadow slot and the lamp's shadow clearance in blocks; 4 and 5
     * its shadow tile's view.  The pack fills 0 to 3 with no shadow (-1); the renderer fills the slot and the tile's
     * view.
     */
    public static final int TEXELS = 6;

    public double cameraX;
    public double cameraY;
    public double cameraZ;
    public float partialTicks;
    /** Camera left and up vectors (unit, world space). */
    public final float[] cameraLeft = new float[3];
    public final float[] cameraUp = new float[3];

    /** How much rain thickens this frame's air for the beams, 0..1. */
    public float rain;
    /** Whether the DynL_shadows config is on. */
    public boolean shadows;

    /**
     * The budgeted lights, strongest first, at most {@link #CAPACITY}; each one's intensity is already dimmed by the
     * daylight at its lamp.  Index-aligned with {@link #lightData}'s rows.
     */
    public final List<DynamicLightEmitter> lights = new ArrayList<>();
    /** The lights, {@link #TEXELS} RGBA texels a light, a row each. */
    public final float[] lightData = new float[CAPACITY * TEXELS * 4];

    public final DynamicLightBeams beams = new DynamicLightBeams();
    public final DynamicLightCoronas coronas = new DynamicLightCoronas();

    /** The block grid, and its sections (sx, sy, sz triples) that changed this frame (read, or cleared on entering) for upload. */
    public DynamicLightVoxelWindow voxels;
    public int[] voxelSections = new int[3 * 64];
    public int voxelSectionCount;
    /** Changes whenever a section changes: a shadow tile traced at this version is still right. */
    public int voxelVersion;

    /** Empties the frame for one that draws at most {@code most} lights, beams and coronas. */
    void clear(int most) {
        lights.clear();
        beams.clear(most);
        coronas.clear(most);
        voxelSectionCount = 0;
    }

    void addVoxelSection(int sx, int sy, int sz) {
        int o = voxelSectionCount * 3;
        if (o + 3 > voxelSections.length) {
            int[] grown = new int[voxelSections.length * 2];
            System.arraycopy(voxelSections, 0, grown, 0, voxelSections.length);
            voxelSections = grown;
        }
        voxelSections[o] = sx;
        voxelSections[o + 1] = sy;
        voxelSections[o + 2] = sz;
        voxelSectionCount++;
    }

    /** Packs {@link #lights} into {@link #lightData}, without shadows. */
    void pack() {
        for (int i = 0; i < lights.size(); i++) {
            DynamicLightEmitter e = lights.get(i);
            int o = i * TEXELS * 4;
            lightData[o] = (float) (e.x - cameraX);
            lightData[o + 1] = (float) (e.y - cameraY);
            lightData[o + 2] = (float) (e.z - cameraZ);
            lightData[o + 3] = e.radius;
            lightData[o + 4] = e.red;
            lightData[o + 5] = e.green;
            lightData[o + 6] = e.blue;
            lightData[o + 7] = e.intensity;
            if (e.spot) {
                lightData[o + 8] = e.dx;
                lightData[o + 9] = e.dy;
                lightData[o + 10] = e.dz;
                lightData[o + 11] = e.cosOuter;
                lightData[o + 12] = e.cosInner;
            } else {
                //smoothstep(-2, -1, cos) is 1 for any cos >= -1.
                lightData[o + 8] = 0;
                lightData[o + 9] = 0;
                lightData[o + 10] = -1;
                lightData[o + 11] = -2;
                lightData[o + 12] = -1;
            }
            lightData[o + 13] = -1;
            lightData[o + 14] = e.clearance;
            for (int k = o + 15; k < o + TEXELS * 4; k++) {
                lightData[k] = 0;
            }
        }
    }
}
