package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
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
        //Set position here as we don't tick so the radio won't get update() calls.
        radio.position.set(position);
    }

    @Override
    public boolean hasRadio() {
        return true;
    }

    @Override
    public void remove() {
        super.remove();
        radio.remove();
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

    @Override
    public ComputedVariable createComputedVariable(String variable) {
        switch (variable) {
            case ("radio_active"):
                return new ComputedVariable(this, variable, partialTicks -> radio.isPlaying() ? 1 : 0, false);
            case ("radio_volume"):
                return new ComputedVariable(this, variable, partialTicks -> radio.volume, false);
            case ("radio_preset"):
                return new ComputedVariable(this, variable, partialTicks -> radio.preset, false);
            default:
                return super.createComputedVariable(variable);
        }
    }
}
