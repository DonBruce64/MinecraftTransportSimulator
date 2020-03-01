package minecrafttransportsimulator.guis.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.rendering.vehicles.RenderInstrument;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;

/**Custom instrument render class.  This class is designed to render an instrument into
 * the GUI.  This instrument will render as if it was on the vehicle itself, and will have
 * all lighting effects that vehicle may or may not have.
 *
 * @author don_bruce
 */
public class GUIComponentInstrument{
	public final int x;
	public final int y;
	public final byte instrumentPackIndex;
	public final PackInstrument packInstrument;
	public final ItemInstrument itemInstrument;
	public final EntityVehicleE_Powered vehicle;
	
	    	
	public GUIComponentInstrument(int guiLeft, int guiTop, byte instrumentPackIndex, EntityVehicleE_Powered vehicle){
		this.packInstrument = vehicle.definition.motorized.instruments.get(instrumentPackIndex);
		this.itemInstrument = vehicle.instruments.get(instrumentPackIndex);
		this.x = guiLeft + packInstrument.hudX;
		this.y = guiTop + packInstrument.hudY;
		this.instrumentPackIndex = instrumentPackIndex;
		this.vehicle = vehicle;
	}

	
	/**
	 *  Renders the instrument.  Instruments use the code in {@link RenderInstrument}, so this call
	 *  is really just a forwarding call that applies a few GUI-specific transforms prior to calling
	 *  that function.
	 */
    public void renderInstrument(){
    	GL11.glPushMatrix();
		GL11.glTranslated(x, y, 0);
		GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
		RenderInstrument.drawInstrument(itemInstrument, packInstrument.optionalEngineNumber, vehicle);
		GL11.glPopMatrix();
    }
}
