package minecrafttransportsimulator.guis.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.rendering.instances.RenderInstrument;

/**Custom instrument render class.  This class is designed to render an instrument into
 * the GUI.  This instrument will render as if it was on the entity itself, and will have
 * all lighting effects that entity may or may not have.
 *
 * @author don_bruce
 */
public class GUIComponentInstrument extends AGUIComponent{
	public final int instrumentPackIndex;
	public final JSONInstrumentDefinition packInstrument;
	public ItemInstrument instrument;
	public final AEntityD_Interactable<?> entity;
	    	
	public GUIComponentInstrument(int guiLeft, int guiTop, int instrumentPackIndex, AEntityD_Interactable<?> entity){
		super(guiLeft, guiTop, 0, 0);
		this.packInstrument = entity.definition.instruments.get(instrumentPackIndex);
		this.instrument = entity.instruments.get(instrumentPackIndex);
		this.offsetX = packInstrument.hudX;
		this.offsetY = packInstrument.hudY;
		this.instrumentPackIndex = instrumentPackIndex;
		this.entity = entity;
	}

    @Override
	public void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks){
    	if(visible){
	    	GL11.glPushMatrix();
			GL11.glTranslated(x + offsetX, y + offsetY, 0);
			//Need to scale y by -1 due to inverse coordinates.
			GL11.glScalef(1.0F, -1.0F, 1.0F);
			RenderInstrument.drawInstrument(instrument, packInstrument.optionalPartNumber, entity, packInstrument.hudScale, blendingEnabled, partialTicks);
			GL11.glPopMatrix();
    	}
    }
}
