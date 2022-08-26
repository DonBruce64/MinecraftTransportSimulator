package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Radio tile entity.
 *
 * @author don_bruce
 */
public class TileEntityRadio extends TileEntityDecor {

    public TileEntityRadio(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);
        //Set position here as we don't tick so the radio won't get update() calls.
        radio.position.set(position);
    }

    @Override
    public boolean hasRadio() {
        return true;
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        return radio.interact(player);
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        //Radio-specific variables.
        switch (variable) {
            case ("radio_active"):
                return radio.isPlaying() ? 1 : 0;
            case ("radio_volume"):
                return radio.volume;
            case ("radio_preset"):
                return radio.preset;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }
}
