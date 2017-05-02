package minecrafttransportsimulator.helpers;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Helpers for the world
 *
 * Should wrap more than one method or is complicated to write
 */
public class WorldHelper {

    public static boolean isPositionInLiquid(World world, double x, double y, double z){
        return world.getBlockState(new BlockPos(Math.floor(x), Math.floor(y), Math.floor(z))).getMaterial().isLiquid();
    }
}
