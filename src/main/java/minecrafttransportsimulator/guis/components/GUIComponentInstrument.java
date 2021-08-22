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
public class GUIComponentInstrument{
	public final int x;
	public final int y;
	public final int instrumentPackIndex;
	public final JSONInstrumentDefinition packInstrument;
	public final ItemInstrument instrument;
	public final AEntityD_Interactable<?> entity;
	
	    	
	public GUIComponentInstrument(int guiLeft, int guiTop, int instrumentPackIndex, AEntityD_Interactable<?> entity){
		this.packInstrument = entity.definition.instruments.get(instrumentPackIndex);
		this.instrument = entity.instruments.get(instrumentPackIndex);
		this.x = guiLeft + packInstrument.hudX;
		this.y = guiTop + packInstrument.hudY;
		this.instrumentPackIndex = instrumentPackIndex;
		this.entity = entity;
	}

	
	/**
	 *  Renders the instrument.  Instruments use the code in {@link RenderInstrument}, so this call
	 *  is really just a forwarding call that applies a few GUI-specific transforms prior to calling
	 *  that function.
	 */
    public void renderInstrument(boolean blendingEnabled, float partialTicks){
    	GL11.glPushMatrix();
		GL11.glTranslated(x, y, 0);
		GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
		RenderInstrument.drawInstrument(instrument, packInstrument.optionalPartNumber, entity, blendingEnabled, partialTicks);
		GL11.glPopMatrix();
    }
}
