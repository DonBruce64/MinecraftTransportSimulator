package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.rendering.RenderInstrument;

/**
 * Custom instrument render class.  This class is designed to render an instrument into
 * the GUI.  This instrument will render as if it was on the entity itself, and will have
 * all lighting effects that entity may or may not have.
 *
 * @author don_bruce
 */
public class GUIComponentInstrument extends AGUIComponent {
    public final AEntityE_Interactable<?> entity;
    public final int slot;
    private static final TransformationMatrix transform = new TransformationMatrix();

    public GUIComponentInstrument(int guiLeft, int guiTop, AEntityE_Interactable<?> entity, int slot) {
        super(guiLeft, guiTop, 0, 0);
        this.entity = entity;
        this.slot = slot;
        JSONInstrumentDefinition packInstrument = entity.definition.instruments.get(slot);
        position.x += packInstrument.hudX;
        //Need to offset in opposite Y position due to inverted coords.
        position.y -= packInstrument.hudY;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        transform.setTranslation(position.x, position.y, position.z + MODEL_DEFAULT_ZOFFSET * 0.5);
        RenderInstrument.drawInstrument(entity, transform, slot, true, blendingEnabled, partialTicks);
    }
}
