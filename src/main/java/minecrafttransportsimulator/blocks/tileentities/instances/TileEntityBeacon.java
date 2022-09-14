package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;

/**
 * Beacon tile entity.  Contains code for handling interfacing with
 * the global world saved data and information of the beacon states.
 * Note that the variables for this beacon are saved as textLines
 * in the main decor class to ensure they both display properly,
 * and to allow a common interface for all GUI operations.
 *
 * @author don_bruce
 */
public class TileEntityBeacon extends TileEntityDecor {
    public NavBeacon currentBeacon;

    public TileEntityBeacon(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);
    }

    @Override
    protected void initializeAnimations() {
        super.initializeAnimations();
        for (JSONText textDef : text.keySet()) {
            currentBeacon = NavBeacon.getByNameFromWorld(world, text.get(textDef));
            return;
        }
    }

    @Override
    public void destroy(BoundingBox box) {
        super.destroy(box);
        if (currentBeacon != null) {
            NavBeacon.removeFromWorld(world, currentBeacon.name);
        }
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
        return true;
    }

    @Override
    public void updateText(LinkedHashMap<String, String> textLines) {
        if (currentBeacon != null) {
            NavBeacon.removeFromWorld(world, currentBeacon.name);
            currentBeacon = null;
        }
        try {
            //Try to create the beacon before setting text.  If it's invalid text, we don't want to save it.
            //If the object can be created, then we just call super and let it handle this.
            String name = null;
            double glideSlope = 0;
            double bearing = 0;
            int entry = 0;
            for (Entry<String, String> textEntry : textLines.entrySet()) {
                switch (entry++) {
                    case 0:
                        name = textEntry.getValue();
                        break;
                    case 1:
                        glideSlope = Double.parseDouble(textEntry.getValue());
                        break;
                    case 2:
                        bearing = Double.parseDouble(textEntry.getValue());
                        break;
                    default: //Don't do anything, we don't use this text here.
                }
            }

            NavBeacon newBeacon = new NavBeacon(world, name, glideSlope, bearing, position);
            super.updateText(textLines);
            currentBeacon = newBeacon;
        } catch (Exception e) {
            //Don't update text.  It's entered invalid.
        }
    }
}