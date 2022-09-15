package minecrafttransportsimulator.blocks.components;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;

/**
 * Base Block class.  This type is used in the constructor of the interface block to allow us to use
 * completely custom code that is not associated with MC's standard block code that changes EVERY FREAKING VERSION.
 * Seriously guys, you make a game about blocks.  How many times you gonna re-invent them?
 * Anyways... This code contains methods for the block's hardness, blast resistance, and rotation.
 *
 * @author don_bruce
 */
public abstract class ABlockBase {
    public final float hardness;
    public final float blastResistance;

    public ABlockBase(float hardness, float blastResistance) {
        this.hardness = hardness;
        this.blastResistance = blastResistance;
    }

    /**
     * Called when this block is removed from the world.  This occurs when the block is broken
     * by a player, explosion, vehicle, etc.  This method is called prior to the Tile Entity being
     * removed, as logic may be needed to be performed that requires the data from the TE.
     * This is ONLY called on the server, so if you have data to sync, do it via packets.
     */
    public void onBroken(AWrapperWorld world, Point3D position) {
    }

    /**
     * Enums for side-specific stuff.
     */
    public enum Axis {
        NONE(0, 0, 0, 0, false, false),
        UP(0, 1, 0, 0, true, false),
        DOWN(0, -1, 0, 0, true, false),
        NORTH(0, 0, -1, 180, true, true),
        SOUTH(0, 0, 1, 0, true, true),
        EAST(1, 0, 0, 90, true, true),
        WEST(-1, 0, 0, 270, true, true),

        NORTHEAST(1, 0, -1, 135, false, true),
        SOUTHEAST(1, 0, 1, 45, false, true),
        NORTHWEST(-1, 0, -1, 225, false, true),
        SOUTHWEST(-1, 0, 1, 315, false, true);

        public final int xOffset;
        public final int yOffset;
        public final int zOffset;
        public final RotationMatrix rotation;
        public final boolean blockBased;
        public final boolean xzPlanar;

        Axis(int xOffset, int yOffset, int zOffset, int yRotation, boolean blockBased, boolean xzPlanar) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            this.rotation = new RotationMatrix().setToAngles(new Point3D(0, yRotation, 0));
            this.blockBased = blockBased;
            this.xzPlanar = xzPlanar;
        }

        public Point3D getOffsetPoint(Point3D point) {
            return point.copy().add(xOffset, yOffset, zOffset);
        }

        public Axis getOpposite() {
            switch (this) {
                case UP:
                    return DOWN;
                case DOWN:
                    return UP;
                case NORTH:
                    return SOUTH;
                case SOUTH:
                    return NORTH;
                case EAST:
                    return WEST;
                case WEST:
                    return EAST;
                case NORTHEAST:
                    return SOUTHWEST;
                case SOUTHEAST:
                    return NORTHWEST;
                case NORTHWEST:
                    return SOUTHEAST;
                case SOUTHWEST:
                    return NORTHEAST;
                default:
                    return NONE;
            }
        }

        public static Axis getFromRotation(double rotation, boolean checkDiagonals) {
            rotation = rotation % 360;
            if (rotation < 0) {
                rotation += 360;
            }
            int degRotation = (checkDiagonals ? (int) (Math.round(rotation / 45) * 45) : (int) (Math.round(rotation / 90) * 90)) % 360;
            for (Axis axis : values()) {
                if (axis.xzPlanar && axis.rotation.angles.y == degRotation) {
                    return axis;
                }
            }
            return Axis.NONE;
        }
    }

    /**
     * Enums for block material properties.  Not used by any of our blocks,
     * but instead are materials that blocks in the world may be made of.
     */
    public enum BlockMaterial {
        NORMAL,
        NORMAL_WET,
        DIRT,
        DIRT_WET,
        SAND,
        SAND_WET,
        SNOW,
        ICE
    }
}
