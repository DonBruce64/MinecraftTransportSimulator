package mcinterface1201.mixin.common;

import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConcretePowderBlock.class)
public interface ConcretePowderBlockMixin {
    @Accessor("concrete")
    BlockState getConcrete();
}
