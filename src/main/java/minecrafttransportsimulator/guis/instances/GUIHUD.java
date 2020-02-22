package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.rendering.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;

/**A GUI that is used to render the HUG.  This is used in {@link GUIInstruments}
 * as well as the {@link ClientEventSystem} to render the HUD.  Note that when
 * the HUD is rendered in the vehicle it will NOT inhibit key inputs as the
 * HUD there is designed to be an overlay rather than an actual GUI.
 * 
 * @author don_bruce
 */
public class GUIHUD extends AGUIBase{
	public static final int HUD_WIDTH = 400;
	public static final int HUD_HEIGHT = 140;
	private final EntityVehicleE_Powered vehicle;

	public GUIHUD(EntityVehicleE_Powered vehicle){
		this.vehicle = vehicle;
	}
	
	@Override
	public final void setupComponents(int guiLeft, int guiTop){
		//Add instruments.  These go wherever they are specified in the JSON.
		for(Byte instrumentNumber : vehicle.instruments.keySet()){
			//Only add instruments that don't have an optionaEngineNumber.
			if(vehicle.definition.motorized.instruments.get(instrumentNumber).optionalEngineNumber == 0){
				addInstrument(new GUIComponentInstrument(guiLeft, guiTop, instrumentNumber, vehicle));
			}
		}
	}

	@Override
	public void setStates(){}
	
	@Override
	public GUILightingMode getGUILightMode(){
		return RenderVehicle.isVehicleIlluminated(vehicle) ? GUILightingMode.LIT : GUILightingMode.DARK;
	}
	
	@Override
	public int getWidth(){
		return HUD_WIDTH;
	}
	
	@Override
	public int getHeight(){
		return HUD_HEIGHT;
	}
	
	@Override
	public boolean renderFlushBottom(){
		return true;
	}
	
	@Override
	public String getTexture(){
		return vehicle.definition.rendering.hudTexture != null ? vehicle.definition.rendering.hudTexture : "mts:textures/guis/hud.png";
	}
}
