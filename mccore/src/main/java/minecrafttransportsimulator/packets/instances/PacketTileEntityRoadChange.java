package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Packet sent to roads to change their states.  This gets sent when a player clicks a road on the client.
 * Packet is sent to the server to change the road state to match what item the player is holding.
 * If the player isn't holding an item, they may have wreneched the component to remove it.
 *
 * @author don_bruce
 */
public class PacketTileEntityRoadChange extends APacketEntityInteract<TileEntityRoad, IWrapperPlayer> {
    private final RoadComponent componentType;
    private final ItemRoadComponent componentItem;

    public PacketTileEntityRoadChange(TileEntityRoad road, IWrapperPlayer player, RoadComponent componentType, ItemRoadComponent componentItem) {
        super(road, player);
        this.componentType = componentType;
        this.componentItem = componentItem;
    }

    public PacketTileEntityRoadChange(ByteBuf buf) {
        super(buf);
        this.componentType = RoadComponent.values()[buf.readByte()];
        if (buf.readBoolean()) {
            this.componentItem = PackParser.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf));
        } else {
            this.componentItem = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(componentType.ordinal());
        if (componentItem != null) {
            buf.writeBoolean(true);
            writeStringToBuffer(componentItem.definition.packID, buf);
            writeStringToBuffer(componentItem.definition.systemName, buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    protected boolean handle(AWrapperWorld world, TileEntityRoad road, IWrapperPlayer player) {
        if (componentItem != null) {
            //Player clicked with a component.  Add/change it.
            road.components.put(componentType, componentItem);
            if (!player.isCreative()) {
                player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
            }
            return true;
        } else {
            //Player clicked with a wrench, try to remove the component.
            if (road.components.containsKey(componentType)) {
                if (world.isClient() || player.isCreative() || player.getInventory().addStack(road.components.get(componentType).getNewStack(null))) {
                    road.components.remove(componentType);
                    return true;
                }
            }
        }
        return false;
    }
}
