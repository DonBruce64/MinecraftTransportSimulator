package mcinterface1201.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcinterface1201.BuilderEntityExisting;
import mcinterface1201.WrapperAABBCollective;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Pseudo
@Mixin(targets = "com.tacz.guns.util.HitboxHelper", remap = false)
public abstract class TaczHitboxMixin {
    @Inject(method = "getFixedBoundingBox", at = @At("HEAD"), cancellable = true, require = 0)
    private static void useVehicleProjectileHitboxes(Entity target, Entity shooter, CallbackInfoReturnable<AABB> callback) {
        //TaCZ's lag correction creates a single AABB and loses the vehicle's individual hitboxes.
        if (target instanceof BuilderEntityExisting builder && builder.isPickable()
            && target.getBoundingBox() instanceof WrapperAABBCollective) {
            callback.setReturnValue(target.getBoundingBox());
        }
    }
}
