package mcinterface1165.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.BlockState;
import net.minecraft.block.ConcretePowderBlock;

@Mixin(ConcretePowderBlock.class)
public interface ConcretePowderBlockMixin {
    @Accessor("concrete")
    BlockState getConcrete();
}
