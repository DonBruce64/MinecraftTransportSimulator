package mcinterface1211.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;

@Mixin(ConcretePowderBlock.class)
public interface ConcretePowderBlockMixin {
    @Accessor("concrete")
    Block getConcrete();
}
