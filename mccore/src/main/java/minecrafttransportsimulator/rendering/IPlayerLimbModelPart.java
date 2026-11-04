package minecrafttransportsimulator.rendering;

/**
 * Implemented on Minecraft limb model parts by each version interface. The
 * additional scale is applied along the limb's local length axis after its
 * pivot translation and rotation, so the shoulder/hip remains fixed.
 */
public interface IPlayerLimbModelPart {
    void setPlayerLimbLengthScale(float scale);

    float getPlayerLimbLengthScale();

    /** Sets the authored local-axis scale applied around the distal face anchor. */
    void setPlayerLimbScale(float scaleX, float scaleY, float scaleZ);

    float getPlayerLimbScaleX();

    float getPlayerLimbScaleY();

    float getPlayerLimbScaleZ();

    /** Sets the local center of the hand or foot end face used as the scale anchor. */
    void setPlayerLimbScaleAnchor(float anchorX, float anchorY);

    float getPlayerLimbScaleAnchorX();

    float getPlayerLimbScaleAnchorY();

    /**
     * Distance from the model part's rotation pivot to the proximal end of the
     * cuboid, in world/model units.  Arms have two pixels above their pivot;
     * legs begin directly at their pivot.
     */
    void setPlayerLimbProximalOffset(float offset);

    float getPlayerLimbProximalOffset();
}
