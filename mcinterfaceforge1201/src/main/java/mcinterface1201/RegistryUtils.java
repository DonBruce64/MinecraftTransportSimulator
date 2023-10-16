package mcinterface1201;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

public class RegistryUtils {
    public static ResourceLocation getRegistryName(Fluid type) {
        return ((IForgeRegistry)ForgeRegistries.FLUID_TYPES.get()).getKey(type.getFluidType());
    }
}
