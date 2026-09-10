package minecrafttransportsimulator.guis.components;

/**
 * GUI texture cutout intended for small HUD icons.  Unlike ordinary cutouts,
 * icons render above panel chrome and below item models and text.
 *
 * @author don_bruce
 */
public class GUIComponentIcon extends GUIComponentCutout {

    public GUIComponentIcon(AGUIBase gui, int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight) {
        super(gui, x, y, width, height, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
        renderable.setTransucentOverride();
        if (renderableL != null) {
            renderableL.setTransucentOverride();
        }
    }

    @Override
    public int getZOffset() {
        return MODEL_DEFAULT_ZOFFSET - 1;
    }
}
