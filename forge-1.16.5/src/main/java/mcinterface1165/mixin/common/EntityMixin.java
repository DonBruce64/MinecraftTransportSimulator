package mcinterface1165.mixin.common;

import mcinterface1165.BuilderEntityExisting;
import mcinterface1165.BuilderEntityLinkedSeat;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
     * Need this to force eye position while in vehicles.
     * Otherwise, MC uses standard position, which will be wrong.
     */
    @Inject(method = "getCameraPosVec", at = @At(value = "HEAD"), cancellable = true)
    private void inject_getEyePosition(float pPartialTicks, CallbackInfoReturnable<Vector3d> ci) {
        Entity entity = (Entity) ((Object) this);
        Entity riding = entity.getVehicle();
        if (riding instanceof BuilderEntityLinkedSeat) {
            BuilderEntityLinkedSeat builder = (BuilderEntityLinkedSeat) riding;
            if (builder.entity != null) {
                ci.setReturnValue(new Vector3d(builder.entity.riderHeadPosition.x, builder.entity.riderHeadPosition.y, builder.entity.riderHeadPosition.z));
            }
        }
    }

    /**
     * Need this to force collision with vehicles.
     */
    @Inject(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "HEAD"), cancellable = true)
    private void inject_collide(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
        Entity entity = (Entity) ((Object) this);
        Box box = entity.getBoundingBox();
        boolean collidedWithVehicle = false;
        for (BuilderEntityExisting builder : entity.world.getNonSpectatingEntities(BuilderEntityExisting.class, box.stretch(movement))) {
            if (builder.collisionBoxes != null) {
                movement = builder.collisionBoxes.getCollision(movement, box);
                collidedWithVehicle = true;
            }
        }
        if (collidedWithVehicle) {
            cir.setReturnValue(movement);
        }
    }
}
