package mcinterface1201.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcinterface1201.BuilderEntityExisting;
import mcinterface1201.BuilderEntityLinkedSeat;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
     * Need this to force eye position while in vehicles.
     * Otherwise, MC uses standard position, which will be wrong.
     */
    @Inject(method = "getEyePosition(F)Lnet/minecraft/util/math/vector/Vector3d;", at = @At(value = "HEAD"), cancellable = true)
    private void inject_getEyePosition(float pPartialTicks, CallbackInfoReturnable<Vector3d> ci) {
        Entity entity = (Entity) ((Object) this);
        Entity riding = entity.getVehicle();
        if (riding instanceof BuilderEntityLinkedSeat) {
            BuilderEntityLinkedSeat builder = (BuilderEntityLinkedSeat) riding;
            if(builder.entity != null) {
                ci.setReturnValue(new Vector3d(builder.entity.riderHeadPosition.x, builder.entity.riderHeadPosition.y, builder.entity.riderHeadPosition.z));
            }
        }
    }

    /**
     * Need this to force collision with vehicles.
     */
    @Inject(method = "collide(Lnet/minecraft/util/math/vector/Vector3d;)Lnet/minecraft/util/math/vector/Vector3d;", at = @At(value = "HEAD"), cancellable = true)
    private void inject_collide(Vector3d movement, CallbackInfoReturnable<Vector3d> ci) {
        Entity entity = (Entity) ((Object) this);
        AxisAlignedBB box = entity.getBoundingBox();
        boolean collidedWithVehicle = false;
        for (BuilderEntityExisting builder : entity.level.getEntitiesOfClass(BuilderEntityExisting.class, box.expandTowards(movement))) {
            if (builder.collisionBoxes != null) {
                movement = builder.collisionBoxes.getCollision(movement, box);
                collidedWithVehicle = true;
            }
        }
        if (collidedWithVehicle) {
            ci.setReturnValue(movement);
        }
    }
}
