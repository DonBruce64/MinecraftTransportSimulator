package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Core component for poles.  Allows us to change the core model.
 *
 * @author don_bruce
 */
public class TileEntityPole_Core extends ATileEntityPole_Component {

    public TileEntityPole_Core(TileEntityPole core, IWrapperPlayer placingPlayer, Axis axis, IWrapperNBT data) {
        super(core, placingPlayer, axis, data);
    }
}
