package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityInteractGUI;

/**
 * Radio tile entity.
 *
 * @author don_bruce
 */
public class TileEntityRadio extends TileEntityDecor {

    public TileEntityRadio(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
    }

    @Override
    public boolean hasRadio() {
        return true;
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        if (radio.interact(player)) {
            playersInteracting.add(player);
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityInteractGUI(this, player, true));
            return true;
        } else {
            return false;
        }
    }
}
