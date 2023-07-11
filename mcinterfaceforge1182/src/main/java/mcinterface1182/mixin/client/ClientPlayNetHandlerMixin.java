package mcinterface1182.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1182.BuilderEntityExisting;
import mcinterface1182.BuilderEntityLinkedSeat;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPacketListener;
import net.minecraft.client.world.ClientLevel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.play.server.ClientboundAddEntityPacket;
import net.minecraft.util.registry.Registry;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetHandlerMixin {
    @Shadow
    private Minecraft minecraft;
    @Shadow
    private ClientLevel level;

    /**
     * Need this to spawn our entity on the client.  MC doesn't handle new entities, and Forge didn't make any hooks.
     * The only way we could do this is by extending LivingEntity, but that's a lotta overhead we don't want.
     */
    @SuppressWarnings("deprecation")
    @Inject(method = "handleAddEntity", at = @At(value = "TAIL"))
    public void inject_handleAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        int typeID = Registry.ENTITY_TYPE.getId(packet.getType());
        EntityType<?> type = Registry.ENTITY_TYPE.byId(typeID);
        if (type == BuilderEntityExisting.E_TYPE2.get() || type == BuilderEntityLinkedSeat.E_TYPE3.get()) {
            Entity entity = EntityType.create(typeID, minecraft.level);
            if (entity != null) {
                int i = packet.getId();
                double d0 = packet.getX();
                double d1 = packet.getY();
                double d2 = packet.getZ();

                entity.setPacketCoordinates(d0, d1, d2);
                entity.moveTo(d0, d1, d2);
                entity.xo = packet.getxRot() * 360 / 256.0F;
                entity.yo = packet.getyRot() * 360 / 256.0F;
                entity.setId(i);
                entity.setUUID(packet.getUUID());
                level.putNonPlayerEntity(i, entity);
            } else {
                InterfaceManager.coreInterface.logError("Custom MC-Spawn packet failed to find entity!");
            }
        }
    }
}
