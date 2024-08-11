package mcinterface1165.mixin.common;

import net.minecraft.block.BlockState;
import net.minecraft.block.ConcretePowderBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConcretePowderBlock.class)
public interface ConcretePowderBlockMixin {
    @Accessor("hardenedState")
    BlockState getHardenedState();
}
