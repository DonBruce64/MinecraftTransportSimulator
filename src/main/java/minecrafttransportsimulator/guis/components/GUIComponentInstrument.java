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
	
	public boolean visible = true;
	    	
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
    	if(visible){
	    	GL11.glPushMatrix();
			GL11.glTranslated(x, y, 0);
			//Need to scale y by -1 due to inverse coordinates.
			GL11.glScalef(1.0F, -1.0F, 1.0F);
			RenderInstrument.drawInstrument(instrument, packInstrument.optionalPartNumber, entity, packInstrument.hudScale, blendingEnabled, partialTicks);
			GL11.glPopMatrix();
    	}
    }
}
