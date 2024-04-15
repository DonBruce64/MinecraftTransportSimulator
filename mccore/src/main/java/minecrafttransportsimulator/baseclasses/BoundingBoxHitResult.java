package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;

/**
 * Class for hitting on bounding boxes.
 */
public class BoundingBoxHitResult {
    public final BoundingBox box;
    public final Point3D position;
    public final Axis side;

    public BoundingBoxHitResult(BoundingBox box, Point3D hitPosition, Axis side) {
        this.box = box;
        this.position = hitPosition;
        this.side = side;
    }
}