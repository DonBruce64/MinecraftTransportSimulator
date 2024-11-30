package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

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
    public static final String BEACON_NAME_TEXT_KEY = "Beacon Name";;
    public static final String BEACON_GLIDESLOPE_TEXT_KEY = "Glide Slope (Deg)";
    public static final String BEACON_BEARING_TEXT_KEY = "Bearing (Deg)";

    public TileEntityBeacon(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
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
    public void updateText(String textKey, String textValue) {
        if (currentBeacon != null) {
            NavBeacon.removeFromWorld(world, currentBeacon.name);
            currentBeacon = null;
        }
        try {
            //Try to create the beacon before setting text.  If it's invalid text, we don't want to save it.
            //If the object can be created, then we just call super and let it handle this.
            String beaconName = null;
            String glideslope = null;
            String bearing = null;
            for (Entry<JSONText, String> textEntry : text.entrySet()) {
                String entryKey = textEntry.getKey().fieldName;
                if (entryKey != null) {
                    switch (entryKey) {
                        case BEACON_NAME_TEXT_KEY: {
                            beaconName = textEntry.getValue();
                            break;
                        }
                        case BEACON_GLIDESLOPE_TEXT_KEY: {
                            glideslope = textEntry.getValue();
                            break;
                        }
                        case BEACON_BEARING_TEXT_KEY: {
                            bearing = textEntry.getValue();
                            break;
                        }
                    }
                }
            }
            switch (textKey) {
                case BEACON_NAME_TEXT_KEY: {
                    beaconName = textValue;
                    break;
                }
                case BEACON_GLIDESLOPE_TEXT_KEY: {
                    glideslope = textValue;
                    break;
                }
                case BEACON_BEARING_TEXT_KEY: {
                    bearing = textValue;
                    break;
                }
            }
            currentBeacon = new NavBeacon(world, beaconName, Double.parseDouble(glideslope), Double.parseDouble(bearing), position);
            super.updateText(textKey, textValue);
        } catch (Exception e) {
            //Don't update text.  It's entered invalid.
        }
    }
}