package minecrafttransportsimulator.guis.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.rendering.RenderInstruments;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.wrappers.WrapperGUI;

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
	public final EntityVehicleE_Powered vehicle;
	
	    	
	public GUIComponentInstrument(int guiLeft, int guiTop, byte instrumentPackIndex, EntityVehicleE_Powered vehicle){
		this.packInstrument = vehicle.definition.motorized.instruments.get(instrumentPackIndex);
		this.x = guiLeft + packInstrument.hudX;
		this.y = guiTop + packInstrument.hudY;
		this.instrumentPackIndex = instrumentPackIndex;
		this.vehicle = vehicle;
	}

	
	/**
	 *  Renders the instrument.  Pre-render operations such as coordinate translation should already
	 *  be done at this point, with the only thing for this instrument to do is call the various OpenGL
	 *  functions required to render it.  NO MC-specifc code should be in this method!  All MC-specific
	 *  code belongs in {@link WrapperGUI} instead!
	 */
    public void renderInstrument(){
		if(vehicle.instruments.containsKey(instrumentPackIndex)){
	    	GL11.glPushMatrix();
	    	//Need to translate by negative numbers as the GUI coords are inverted from normal OpenGL coords.
	    	//In this case, y is down rather than up, and X is right rather than left.
			GL11.glTranslated(-x, -y, 0);
			GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
			RenderInstruments.drawInstrument(vehicle, vehicle.instruments.get(instrumentPackIndex), packInstrument.optionalEngineNumber);
			GL11.glPopMatrix();
		}
    }
}
