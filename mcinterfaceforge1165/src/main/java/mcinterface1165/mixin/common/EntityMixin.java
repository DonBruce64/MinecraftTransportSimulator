package mcinterface1165.mixin.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcinterface1165.BuilderEntityExisting;
import mcinterface1165.BuilderEntityLinkedSeat;
import mcinterface1165.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

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
    @Redirect(method = "collide(Lnet/minecraft/util/math/vector/Vector3d;)Lnet/minecraft/util/math/vector/Vector3d;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEntityCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    public Stream<VoxelShape> redirect_collide(World world, @Nullable Entity pEntity, AxisAlignedBB pArea, Predicate<Entity> pFilter) {
        if (pEntity != null) {
            List<AxisAlignedBB> otherBoxes = null;
            for (BuilderEntityExisting builder : pEntity.level.getEntitiesOfClass(BuilderEntityExisting.class, pArea)) {
                if (builder.collisionBoxes != null) {
                    if (builder.collisionBoxes.intersects(pArea)) {
                        for (BoundingBox box : builder.collisionBoxes.getBoxes()) {
                            AxisAlignedBB convertedBox = WrapperWorld.convert(box);
                            if (convertedBox.intersects(pArea)) {
                                if (otherBoxes == null) {
                                    otherBoxes = new ArrayList<>();
                                }
                                otherBoxes.add(convertedBox);
                            }
                        }
                    }
                }
            }
            if (otherBoxes != null) {
                final List<AxisAlignedBB> allBoxes = otherBoxes;
                world.getEntities(pEntity, pArea, pFilter.and((testEntity) -> testEntity.getBoundingBox().intersects(pArea) && pEntity.canCollideWith(testEntity))).stream().map(entity -> allBoxes.add(entity.getBoundingBox()));
                return allBoxes.stream().map(VoxelShapes::create);
            }
        }
        return world.getEntityCollisions(pEntity, pArea, pFilter);
    }
}
