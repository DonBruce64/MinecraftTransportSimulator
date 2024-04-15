package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;

/**
 * Class for hitting on blocks.
 */
public class BlockHitResult {
    public final Point3D blockPosition;
    public final Point3D hitPosition;
    public final Axis side;

    public BlockHitResult(Point3D blockPosition, Point3D hitPosition, Axis side) {
        this.blockPosition = blockPosition;
        this.hitPosition = hitPosition;
        this.side = side;
    }
}