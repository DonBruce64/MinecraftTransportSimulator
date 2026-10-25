package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    /**
     * A client block change: the dynamic light block grid reads its section again, so shadows follow it.
     */
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void inject_sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (oldState != newState) {
            DynamicLightSystem.markBlock(pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
