package mcinterface1182.mixin.common;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import mcinterface1182.BuilderEntityExisting;
import mcinterface1182.BuilderEntityLinkedSeat;
import mcinterface1182.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(Entity.class)
public abstract class EntityMixin {
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
     */
    @Redirect(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    public List<VoxelShape> redirect_collide(Level accessor, @Nullable Entity pEntity, AABB pCollisionBox) {
        if (pEntity != null) {
            List<VoxelShape> otherShapes = null;
            for (BuilderEntityExisting builder : pEntity.level.getEntitiesOfClass(BuilderEntityExisting.class, pCollisionBox)) {
                if (builder.collisionBoxes != null) {
                    if (builder.collisionBoxes.intersects(pCollisionBox)) {
                        for (BoundingBox box : builder.collisionBoxes.getBoxes()) {
                            AABB convertedBox = WrapperWorld.convert(box);
                            if (convertedBox.intersects(pCollisionBox)) {
                                if (otherShapes == null) {
                                    otherShapes = new ArrayList<>();
                                }
                                otherShapes.add(Shapes.create(convertedBox));
                            }
                        }
                    }
                }
            }
            if (otherShapes != null) {
                List<VoxelShape> oldImmutableList = accessor.getEntityCollisions(pEntity, pCollisionBox);
                Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(oldImmutableList.size() + otherShapes.size());
                builder.addAll(oldImmutableList);
                builder.addAll(otherShapes);
                return builder.build();
            }
        }
        return accessor.getEntityCollisions(pEntity, pCollisionBox);
    }
}
