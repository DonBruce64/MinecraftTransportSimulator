package mcinterface1165.mixin.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

@Mixin(Entity.class)
public abstract class EntityMixin {
    private Vector3d pVec;

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
     * Need this to force collision with vehicles.  First we get variables when function is called, then
     * we overwrite the collided boxes.
     */
    @Inject(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEntityCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private void inject_collide(Vector3d pVec, CallbackInfoReturnable<Vector3d> ci) {
        this.pVec = pVec;
    }

    @ModifyVariable(method = "collide", at = @At(value = "STORE"), name = "stream1")
    private Stream<VoxelShape> modify_collidelist(Stream<VoxelShape> existingCollisions) {
        Entity entity = (Entity) ((Object) this);
        AxisAlignedBB pCollisionBox = entity.getBoundingBox().expandTowards(pVec);
        List<AxisAlignedBB> vehicleCollisions = null;
        WrapperWorld world = WrapperWorld.getWrapperFor(entity.level);
        for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
            vehicleCollisions = addCollisions(vehicle, pCollisionBox, vehicleCollisions);
        }
        for (EntityPlacedPart placedPart : world.getEntitiesOfType(EntityPlacedPart.class)) {
            vehicleCollisions = addCollisions(placedPart, pCollisionBox, vehicleCollisions);
        }
        if (vehicleCollisions != null) {
            return Stream.concat(vehicleCollisions.stream().map(VoxelShapes::create), existingCollisions);
        } else {
            return existingCollisions;
        }
    }

    @Unique
    private static List<AxisAlignedBB> addCollisions(AEntityF_Multipart<?> multipart, AxisAlignedBB pCollisionBox, List<AxisAlignedBB> collisions) {
        BoundingBox encompassingBox = multipart.encompassingBox;
        if (pCollisionBox.maxX > encompassingBox.globalCenter.x - encompassingBox.widthRadius
                && pCollisionBox.minX < encompassingBox.globalCenter.x + encompassingBox.widthRadius
                && pCollisionBox.maxY > encompassingBox.globalCenter.y - encompassingBox.heightRadius
                && pCollisionBox.minY < encompassingBox.globalCenter.y + encompassingBox.heightRadius
                && pCollisionBox.maxZ > encompassingBox.globalCenter.z - encompassingBox.depthRadius
                && pCollisionBox.minZ < encompassingBox.globalCenter.z + encompassingBox.depthRadius) {
            for (BoundingBox box : multipart.allCollisionBoxes) {
                if (box.collisionTypes.contains(JSONCollisionGroup.CollisionType.ENTITY)) {
                    AxisAlignedBB convertedBox = WrapperWorld.convert(box);
                    if (pCollisionBox.intersects(pCollisionBox)) {
                        if (collisions == null) {
                            collisions = new ArrayList<>();
                        }
                        collisions.addAll(VoxelShapes.create(convertedBox).toAabbs());
                    }
                }
            }
        }
        return collisions;
    }
}
