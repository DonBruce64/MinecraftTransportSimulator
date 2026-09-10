package mcinterface1211.mixin.common;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import mcinterface1211.BuilderEntityExisting;
import mcinterface1211.BuilderEntityLinkedSeat;
import mcinterface1211.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(Entity.class)
public abstract class EntityMixin {
    private Vec3 pVec;

    /**
     * Need this to force eye position while in vehicles.
     * Otherwise, MC uses standard position, which will be wrong.
     */
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "HEAD"), cancellable = true)
    private void inject_getEyePosition(float pPartialTicks, CallbackInfoReturnable<Vec3> ci) {
        Entity entity = (Entity) ((Object) this);
        Entity riding = entity.getVehicle();
        if (riding instanceof BuilderEntityLinkedSeat) {
            BuilderEntityLinkedSeat builder = (BuilderEntityLinkedSeat) riding;
            if(builder.entity != null) {
                ci.setReturnValue(new Vec3(builder.entity.riderHeadPosition.x, builder.entity.riderHeadPosition.y, builder.entity.riderHeadPosition.z));
            }
        }
    }

    /**
     * Need this to force collision with vehicles.  First we get variables when function is called, then
     * we overwrite the collided boxes.
     */
    @Inject(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private void inject_collide(Vec3 pVec, CallbackInfoReturnable<Vec3> ci) {
        this.pVec = pVec;
    }

    @ModifyVariable(method = "collide", at = @At(value = "STORE"), name = "list")
    private List<VoxelShape> modify_collidelist(List<VoxelShape> existingCollisions) {
        Entity entity = (Entity) ((Object) this);
        AABB pCollisionBox = entity.getBoundingBox().expandTowards(pVec);
        List<VoxelShape> vehicleCollisions = null;
        for (BuilderEntityExisting builder : entity.level().getEntitiesOfClass(BuilderEntityExisting.class, pCollisionBox)) {
            if (builder.collisionBoxes != null) {
                if (builder.collisionBoxes.intersects(pCollisionBox)) {
                    for (BoundingBox box : builder.collisionBoxes.getBoxes()) {
                        AABB convertedBox = WrapperWorld.convert(box);
                        if (convertedBox.intersects(pCollisionBox)) {
                            if (vehicleCollisions == null) {
                                vehicleCollisions = new ArrayList<>();
                            }
                            vehicleCollisions.add(Shapes.create(convertedBox));
                        }
                    }
                }
            }
        }
        if (vehicleCollisions != null) {
            if (!existingCollisions.isEmpty()) {
                Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(existingCollisions.size() + vehicleCollisions.size());
                builder.addAll(existingCollisions);
                builder.addAll(vehicleCollisions);
                return builder.build();
            } else {
                return vehicleCollisions;
            }
        } else {
            return existingCollisions;
        }
    }
}
