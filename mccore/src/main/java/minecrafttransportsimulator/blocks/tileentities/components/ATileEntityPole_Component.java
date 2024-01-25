package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.Locale;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Base class for components that can go on poles.  Not actually a TE, just sits on one.
 *
 * @author don_bruce
 */
public abstract class ATileEntityPole_Component extends AEntityD_Definable<JSONPoleComponent> {

    public final TileEntityPole core;
    public final Axis axis;

    public ATileEntityPole_Component(TileEntityPole core, IWrapperPlayer placingPlayer, Axis axis, ItemPoleComponent item, IWrapperNBT data) {
        super(core.world, placingPlayer, item, data);
        this.core = core;
        this.axis = axis;
    }

    @Override
    public EntityAutoUpdateTime getUpdateTime() {
        //Our poles update us all at once so their states are correct.
        return EntityAutoUpdateTime.NEVER;
    }

    @Override
    public boolean shouldLinkBoundsToPosition() {
        return false;
    }

    @Override
    public boolean shouldRenderBeams() {
        return ConfigSystem.client.renderingSettings.blockBeams.value;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable) {
        //Check connector variables.
        if (variable.startsWith("neighbor_present_")) {
            final Axis connectionAxis = Axis.valueOf(variable.substring("neighbor_present_".length()).toUpperCase(Locale.ROOT));
            return new ComputedVariable(this, variable, partialTicks -> world.getTileEntity(connectionAxis.getOffsetPoint(position)) instanceof TileEntityPole ? 1 : 0, false);
        }
        if (variable.startsWith("matching_present_")) {
            final Axis connectionAxis = Axis.valueOf(variable.substring("matching_present_".length()).toUpperCase(Locale.ROOT));
            return new ComputedVariable(this, variable, partialTicks -> {
                ATileEntityBase<?> otherTile = world.getTileEntity(connectionAxis.getOffsetPoint(position));
                return otherTile != null && core.definition.systemName.equals(otherTile.definition.systemName) ? 1 : 0;
            }, false);
        }
        //Check solid block variables.
        if (variable.startsWith("solid_present_")) {
            final Axis connectionAxis = Axis.valueOf(variable.substring("solid_present_".length()).toUpperCase(Locale.ROOT));
            return new ComputedVariable(this, variable, partialTicks -> world.isBlockSolid(connectionAxis.getOffsetPoint(position), connectionAxis.getOpposite()) ? 1 : 0, false);
        }

        //Check slab variables.
        switch (variable) {
            case ("slab_present_up"):
                return new ComputedVariable(this, variable, partialTicks -> world.isBlockAboveTopSlab(position) ? 1 : 0, false);
            case ("slab_present_down"):
                return new ComputedVariable(this, variable, partialTicks -> world.isBlockBelowBottomSlab(position) ? 1 : 0, false);
        }
        return ZERO_VARIABLE;
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        //Only render the bounding box for the core component.
        if (axis.equals(Axis.NONE)) {
            core.renderBoundingBoxes(transform);
        }
    }
}
