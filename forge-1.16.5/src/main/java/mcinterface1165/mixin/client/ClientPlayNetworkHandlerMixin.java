package mcinterface1165.mixin.client;

import mcinterface1165.BuilderEntityExisting;
import mcinterface1165.BuilderEntityLinkedSeat;
import mcinterface1165.BuilderEntityRenderForwarder;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientWorld world;

    /**
     * Need this to spawn our entity on the client.  MC doesn't handle new entities, and Forge didn't make any hooks.
     * The only way we could do this is by extending LivingEntity, but that's a lotta overhead we don't want.
     */
    @Inject(method = "onEntitySpawn", at = @At(value = "TAIL"))
    public void handleVehiclesSpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        EntityType<?> type = packet.getEntityTypeId();
        if (type == BuilderEntityExisting.ENTITY_EXISTING.get() || type == BuilderEntityLinkedSeat.ENTITY_SEAT.get() || type == BuilderEntityRenderForwarder.ENTITY_RENDER.get()) {
            Entity entity = type.create(world);
            if (entity != null) {
                int id = packet.getId();
                double x = packet.getX();
                double y = packet.getY();
                double z = packet.getZ();

                entity.updateTrackedPosition(x, y, z);
                entity.refreshPositionAndAngles(x, y, z, packet.getYaw() * 360 / 256.0F, packet.getPitch() * 360 / 256.0F);
                entity.setEntityId(id);
                entity.setUuid(packet.getUuid());
                world.addEntity(id, entity);
            } else {
                InterfaceManager.coreInterface.logError("Custom MC-Spawn packet failed to find entity!");
            }
        }
    }
}
