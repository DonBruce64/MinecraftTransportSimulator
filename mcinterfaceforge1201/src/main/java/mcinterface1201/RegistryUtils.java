package mcinterface1201;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * This class is essentially used to replace getRegistryName() on some classes that used to have it in 
 * 1.16.5 like Fluids.
 *
 * @author ajh123
 */
public class RegistryUtils {
    public static ResourceLocation getRegistryName(Fluid type) {
        return ((IForgeRegistry)ForgeRegistries.FLUID_TYPES.get()).getKey(type.getFluidType());
    }
}
