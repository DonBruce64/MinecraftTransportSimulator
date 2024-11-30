package mcinterface1182.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

@Mixin(Biome.class)
public interface BiomeMixin {
    @Invoker("getTemperature")
    public float invoke_getTemperature(BlockPos pPos);
}
