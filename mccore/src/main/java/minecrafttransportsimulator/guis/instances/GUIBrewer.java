package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.EntityBrewer;

/**
 * A GUI that is used to interface with brewers.
 *
 * @author don_bruce
 */
public class GUIBrewer extends AGUICrafter {

    public GUIBrewer(EntityBrewer brewer, String texture) {
        super(brewer, texture != null ? texture : "mts:textures/guis/brewer.png", new int[] { 17, 16, 56, 50, 79, 57, 102, 50, 79, 16 }, new int[] { 61, 44, 18, 4, 176, 29, 0 }, new int[] { 98, 16, 9, 28, 176, 0, 1 });
    }
}
