package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.LightType;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalGroup;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Traffic signal component for poles.  This doesn't tick, as the state of the light
 * is by default having the unlinked light on until changed by a {@link TileEntitySignalController}.
 *
 * @author don_bruce
 */
public class TileEntityPole_TrafficSignal extends ATileEntityPole_Component {
    public TileEntitySignalController linkedController;

    public TileEntityPole_TrafficSignal(TileEntityPole core, IWrapperPlayer placingPlayer, Axis axis, IWrapperNBT data) {
        super(core, placingPlayer, axis, data);
    }

    @Override
    public void update() {
        super.update();
        if (linkedController != null) {
            //Remove all old lights, then add our new one.
            for (LightType light : LightType.values()) {
                setVariable(light.lowercaseName, 0);
            }
            if (linkedController.isValid && linkedController.controlledSignals.contains(this)) {
                for (SignalGroup group : linkedController.signalGroups.get(axis)) {
                    if (group.currentLight.lowercaseName != null) {
                        setVariable(group.currentLight.lowercaseName, 1);
                    }
                }
            } else {
                linkedController = null;
            }
        }
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("linked"):
                return linkedController != null ? 1 : 0;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }
}
