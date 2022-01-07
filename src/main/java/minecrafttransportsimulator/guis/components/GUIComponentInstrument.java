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
		position.x += packInstrument.hudX;
    	//Need to offset in opposite Y position due to inverted coords.
		position.y -= packInstrument.hudY;
		this.instrumentPackIndex = instrumentPackIndex;
		this.entity = entity;
	}

    @Override
	public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks){
    	if(visible){
	    	GL11.glPushMatrix();
			GL11.glTranslated(position.x, position.y, position.z + MODEL_DEFAULT_ZOFFSET*0.5);
			RenderInstrument.drawInstrument(instrument, packInstrument.optionalPartNumber, entity, packInstrument.hudScale, blendingEnabled, partialTicks);
			GL11.glPopMatrix();
    	}
    }
}
