package mcinterface1201.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Biome.class)
public interface BiomeInvokerMixin {

    @Invoker
    float getTemperature(BlockPos pos);
}
