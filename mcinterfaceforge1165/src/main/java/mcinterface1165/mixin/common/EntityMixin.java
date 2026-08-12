package mcinterface1165.mixin.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mcinterface1165.BuilderEntityExisting;
import mcinterface1165.BuilderEntityLinkedSeat;
import mcinterface1165.WrapperEntity;
import mcinterface1165.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.MouseFlightController;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;

@Mixin(Entity.class)
public abstract class EntityMixin {
    private Vector3d pVec;
    private final Point3D mouseFlightLineOfSight = new Point3D();

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
     * Make vanilla picking follow the visual camera in first-person mouse flight.
     */
    @Inject(method = "getViewVector(F)Lnet/minecraft/util/math/vector/Vector3d;", at = @At(value = "HEAD"), cancellable = true)
    private void inject_getViewVector(float partialTicks, CallbackInfoReturnable<Vector3d> ci) {
        if (MouseFlightController.shouldUseCameraLineOfSight()) {
            IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
            if (clientPlayer != null && clientPlayer.equals(WrapperEntity.getWrapperFor((Entity) (Object) this))) {
                Point3D lineOfSight = MouseFlightController.getCameraLineOfSight(mouseFlightLineOfSight, 1, partialTicks);
                ci.setReturnValue(new Vector3d(lineOfSight.x, lineOfSight.y, lineOfSight.z));
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
        for (BuilderEntityExisting builder : entity.level.getEntitiesOfClass(BuilderEntityExisting.class, pCollisionBox)) {
            if (builder.collisionBoxes != null) {
                if (builder.collisionBoxes.intersects(pCollisionBox)) {
                    for (BoundingBox box : builder.collisionBoxes.getBoxes()) {
                        AxisAlignedBB convertedBox = WrapperWorld.convert(box);
                        if (convertedBox.intersects(pCollisionBox)) {
                            if (vehicleCollisions == null) {
                                vehicleCollisions = new ArrayList<>();
                            }
                            vehicleCollisions.add(convertedBox);
                        }
                    }
                }
            }
        }
        if (vehicleCollisions != null) {
            return Stream.concat(vehicleCollisions.stream().map(VoxelShapes::create), existingCollisions);
        } else {
            return existingCollisions;
        }
    }
}
