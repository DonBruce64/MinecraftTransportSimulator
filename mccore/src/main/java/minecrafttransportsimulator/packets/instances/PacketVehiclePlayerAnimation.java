package minecrafttransportsimulator.packets.instances;

import java.util.List;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.JSONPlayerAnimation;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.rendering.PlayerAnimationSystem;

/**
 * Server-to-client update for a player action on a vehicle with actor-only
 * animations. The update and actor travel together so a client tick cannot
 * evaluate the animation between the two.
 */
public class PacketVehiclePlayerAnimation extends APacketEntityVariable {
    private final UUID playerID;

    PacketVehiclePlayerAnimation(AEntityD_Definable<?> source, APacketEntityVariable update, UUID playerID) {
        super(source, update);
        this.playerID = playerID;
    }

    public PacketVehiclePlayerAnimation(ByteBuf buf) {
        super(buf, null);
        this.playerID = readUUIDFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        writeUpdate(buf, true);
        writeUUIDToBuffer(playerID, buf);
    }

    @Override
    public void handleFromClient(AWrapperWorld world, IWrapperPlayer sendingPlayer) {
        //Only the server supplies animation attribution.
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityD_Definable<?> entity) {
        if (world.isClient()) {
            ComputedVariable variable = getVariable(entity);
            if (applyUpdate(variable)) {
                PlayerAnimationSystem.recordVariableInteraction(entity, variable, playerID);
            }
        }
        return false;
    }

    static boolean hasActorAnimations(AEntityD_Definable<?> source) {
        EntityVehicleF_Physics vehicle = source instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) source
                : source instanceof APart ? ((APart) source).vehicleOn : null;
        if (vehicle != null && vehicle.definition.rendering != null) {
            List<JSONPlayerAnimation> animations = vehicle.definition.rendering.playerAnimations;
            if (animations != null) {
                for (JSONPlayerAnimation animation : animations) {
                    if (animation.initiatingPlayerOnly) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
