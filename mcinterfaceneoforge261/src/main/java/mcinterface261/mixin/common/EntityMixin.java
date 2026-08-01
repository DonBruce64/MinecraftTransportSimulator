package mcinterface261.mixin.common;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mcinterface261.BuilderEntityExisting;
import mcinterface261.BuilderEntityLinkedSeat;
import mcinterface261.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(Entity.class)
public abstract class EntityMixin {
    private static final Map<BoundingBox, VoxelShape> mts$vehicleCollisionShapeCache = new IdentityHashMap<>();
    private static long mts$vehicleCollisionShapeCacheTick = -1;

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
     * Need this to force collision with vehicles.
     * Wraps the getEntityCollisions call to append vehicle collision boxes.
     * The AABB passed to getEntityCollisions is already the expanded collision box,
     * so no need to capture pVec separately.
     */
    @WrapOperation(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<VoxelShape> wrap_getEntityCollisions(Level level, Entity entity, AABB pCollisionBox, Operation<List<VoxelShape>> original) {
        List<VoxelShape> existingCollisions = original.call(level, entity, pCollisionBox);
        List<VoxelShape> vehicleCollisions = null;
        long gameTime = entity.level().getGameTime();
        if (mts$vehicleCollisionShapeCacheTick != gameTime) {
            mts$vehicleCollisionShapeCache.clear();
            mts$vehicleCollisionShapeCacheTick = gameTime;
        }
        for (BuilderEntityExisting builder : entity.level().getEntitiesOfClass(BuilderEntityExisting.class, pCollisionBox)) {
            if (builder.collisionBoxes != null) {
                if (builder.collisionBoxes.intersects(pCollisionBox)) {
                    for (BoundingBox box : builder.collisionBoxes.getBoxes()) {
                        VoxelShape vehicleCollision = mts$vehicleCollisionShapeCache.computeIfAbsent(box, cachedBox -> Shapes.create(WrapperWorld.convert(cachedBox)));
                        if (vehicleCollision.bounds().intersects(pCollisionBox)) {
                            if (vehicleCollisions == null) {
                                vehicleCollisions = new ArrayList<>();
                            }
                            vehicleCollisions.add(vehicleCollision);
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
