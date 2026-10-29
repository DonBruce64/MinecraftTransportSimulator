package minecrafttransportsimulator.rendering.dynamiclight;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a frame's lit lamps into the small ranked list the light pass can afford: frustum-culled by
 * their reach, nearby point lights merged into one, then the N strongest by {@code intensity / distance^2}.
 * The emission budget.  Pure logic, render thread only.
 */
public final class DynamicLightBudget {
    private final List<DynamicLightEmitter> visible = new ArrayList<>();
    private final List<DynamicLightEmitter> merged = new ArrayList<>();
    private final List<DynamicLightEmitter> pool = new ArrayList<>();
    private int poolUsed;
    private double viewerX;
    private double viewerY;
    private double viewerZ;

    /**
     * Fills {@code out} (cleared first) with at most {@code maxCount} of {@code submitted}, strongest first.
     * The returned emitters belong to this budget and are valid until the next call.
     *
     * @param mergeDistance point lights within this many blocks of each other combine into one; {@code <= 0} disables merging
     */
    public void select(List<DynamicLightEmitter> submitted, double viewerX, double viewerY, double viewerZ, DynamicLightFrustum frustum, int maxCount, float mergeDistance, List<DynamicLightEmitter> out) {
        out.clear();
        poolUsed = 0;
        if (submitted.isEmpty() || maxCount <= 0) {
            return;
        }
        visible.clear();
        for (DynamicLightEmitter e : submitted) {
            //A light off screen still lights the ground on screen: bound by its reach.
            if (frustum.isSphereVisible(e.x, e.y, e.z, e.radius)) {
                visible.add(e);
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        merge(mergeDistance);
        this.viewerX = viewerX;
        this.viewerY = viewerY;
        this.viewerZ = viewerZ;
        merged.sort((a, b) -> Float.compare(b.scoreFrom(this.viewerX, this.viewerY, this.viewerZ), a.scoreFrom(this.viewerX, this.viewerY, this.viewerZ)));
        for (int i = 0; i < merged.size() && i < maxCount; ++i) {
            out.add(merged.get(i));
        }
    }

    /**
     * Greedy clustering: each still-unmerged point light absorbs every later one within {@code mergeDistance}.
     * Light combines additively, color blended by intensity share, radius grows to cover the cluster.
     * A spot never merges: each keeps its own shadow tile and beam.
     */
    private void merge(float mergeDistance) {
        merged.clear();
        int n = visible.size();
        boolean[] absorbed = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (absorbed[i]) {
                continue;
            }
            DynamicLightEmitter seed = visible.get(i);
            if (seed.spot || mergeDistance <= 0) {
                merged.add(seed);
                continue;
            }
            DynamicLightEmitter combined = null;
            float extraRadius = 0;
            for (int j = i + 1; j < n; j++) {
                DynamicLightEmitter other = visible.get(j);
                if (absorbed[j] || other.spot) {
                    continue;
                }
                float d = (float) seed.distanceTo(other.x, other.y, other.z);
                if (d > mergeDistance) {
                    continue;
                }
                absorbed[j] = true;
                if (combined == null) {
                    combined = obtain().set(seed);
                }
                float total = combined.intensity + other.intensity;
                if (total > 0) {
                    float wa = combined.intensity / total;
                    float wb = other.intensity / total;
                    combined.red = combined.red * wa + other.red * wb;
                    combined.green = combined.green * wa + other.green * wb;
                    combined.blue = combined.blue * wa + other.blue * wb;
                }
                combined.intensity = total;
                combined.radius = Math.max(combined.radius, other.radius);
                combined.clearance = Math.max(combined.clearance, other.clearance);
                extraRadius = Math.max(extraRadius, d);
            }
            if (combined != null) {
                combined.radius = Math.max(combined.radius, extraRadius);
                merged.add(combined);
            } else {
                merged.add(seed);
            }
        }
    }

    private DynamicLightEmitter obtain() {
        if (poolUsed == pool.size()) {
            pool.add(new DynamicLightEmitter());
        }
        return pool.get(poolUsed++);
    }
}
