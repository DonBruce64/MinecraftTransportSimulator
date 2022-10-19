package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Core;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_StreetLight;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_TrafficSignal;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.PackParser;

public class ItemPoleComponent extends AItemSubTyped<JSONPoleComponent> implements IItemBlock {

    public ItemPoleComponent(JSONPoleComponent definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean onBlockClicked(AWrapperWorld world, IWrapperPlayer player, Point3D position, Axis axis) {
        if (definition.pole.type.equals(PoleComponentType.CORE)) {
            return this.placeBlock(world, player, position, axis);
        } else {
            return false;
        }
    }

    @Override
    public Class<? extends ABlockBase> getBlockClass() {
        return BlockPole.class;
    }

    public enum PoleComponentType {
        @JSONDescription("The base of any pole system is the core type. This is the central structure that connects to other pole bits and allows placement of components on it.  You cannot place other components without placing one of these first.\nThe pole model you'll make will look like nothing you'll see in-game.  This is because it contains all possible model components that could be rendered.  Each of these components has a specific object name, and should only be rendered in specific conditions based on what the pole is connected to (other poles, solids, slabs, etc.).")
        CORE,
        @JSONDescription("Perhaps the most standard of lights, traffic signals consist of a main model (named anything you like), plus the lights (see the lights section).  You may omit any or all lights should you wish to change your sinal's behavior.  This may include making fewer or more bulbs than the standard 3 light.  Say a 2-light unit for a crossing signal.")
        TRAFFIC_SIGNAL,
        @JSONDescription("These are the simplest type of lights.  Designed for street accents, the lights normally don't change state.  However, signal controllers can turn their light off via a redstone input.")
        STREET_LIGHT,
        @JSONDescription("Signs are the third pole component you can create, and perhaps one of the most overlooked pack-based things in MTS.  Signs may have lights on them as well, and behave the same as street lights; the only exception being that their lights cannot be controlled by signal controllers.  If a sign has textObjects in its rendering section, then it will allow for editing that text via GUI.  This allows for dynamic route and speed limit signs, among others.")
        SIGN;

        /**
         * Helper method to create a component for the passed-in pole.  Does not add the component
         * to the pole, only creates it.
         */
        public static ATileEntityPole_Component createComponent(TileEntityPole pole, IWrapperPlayer placingPlayer, Axis axis, IWrapperNBT data) {
            ItemPoleComponent item = PackParser.getItem(data.getString("packID"), data.getString("systemName"), data.getString("subName"));
            switch (item.definition.pole.type) {
                case CORE:
                    return new TileEntityPole_Core(pole, placingPlayer, axis, data);
                case TRAFFIC_SIGNAL:
                    return new TileEntityPole_TrafficSignal(pole, placingPlayer, axis, data);
                case STREET_LIGHT:
                    return new TileEntityPole_StreetLight(pole, placingPlayer, axis, data);
                case SIGN:
                    return new TileEntityPole_Sign(pole, placingPlayer, axis, data);
            }
            //We'll never get here.
            return null;
        }
    }
}
