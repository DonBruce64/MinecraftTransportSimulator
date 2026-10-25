package minecrafttransportsimulator.rendering.dynamiclight;

/**
 * The camera frustum, asked as a plain question so mccore never names a Minecraft frustum.
 * The interface module hands in a lambda wrapping the real one.
 */
public interface DynamicLightFrustum {
    /** Accepts everything: no culling. */
    DynamicLightFrustum ACCEPT_ALL = (minX, minY, minZ, maxX, maxY, maxZ) -> true;

    /** Whether a world-space box is at least partly visible. */
    boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    /** Whether a world-space sphere is at least partly visible (by its bounding box). */
    default boolean isSphereVisible(double x, double y, double z, double radius) {
        return isVisible(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }
}
