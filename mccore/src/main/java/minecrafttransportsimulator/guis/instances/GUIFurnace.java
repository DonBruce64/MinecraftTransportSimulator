package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.EntityFurnace;

/**
 * A GUI that is used to interface with furnaces.
 *
 * @author don_bruce
 */
public class GUIFurnace extends AGUICrafter {

    public GUIFurnace(EntityFurnace furnace, String texture) {
        super(furnace, texture != null ? texture : "mts:textures/guis/furnace.png", new int[] { 79, 53, 51, 20, 110, 21 }, new int[] { 81, 38, 14, 14, 176, 0, 1 }, new int[] { 77, 20, 24, 17, 176, 14, 0 });
    }
}
