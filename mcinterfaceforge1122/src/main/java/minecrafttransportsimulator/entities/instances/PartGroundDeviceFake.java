package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * A fake ground device that will be added to the vehicle when long ground devices are present.
 * Does not render and will be removed in tandem with the ground device that's linked to it.
 *
 * @author don_bruce
 */
public final class PartGroundDeviceFake extends PartGroundDevice {
    private final PartGroundDevice masterPart;

    public PartGroundDeviceFake(PartGroundDevice masterPart, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data) {
        super(masterPart.entityOn, placingPlayer, placementDefinition, data);
        this.masterPart = masterPart;
    }

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public double getWidth() {
        return masterPart != null ? masterPart.getWidth() : 1.0F;
    }

    @Override
    public double getHeight() {
        return masterPart != null ? masterPart.getHeight() : 1.0F;
    }

    @Override
    public float getLongPartOffset() {
        return -masterPart.getLongPartOffset();
    }
}
