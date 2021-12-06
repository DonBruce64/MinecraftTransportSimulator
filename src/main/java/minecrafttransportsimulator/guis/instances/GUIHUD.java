package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.systems.ConfigSystem;

/**A GUI that is used to render the HUG.  This is used in {@link GUIInstruments}
 * as well as the {@link InterfaceRender} to render the HUD.  Note that when
 * the HUD is rendered in the vehicle it will NOT inhibit key inputs as the
 * HUD there is designed to be an overlay rather than an actual GUI.
 * 
 * @author don_bruce
 */
public class GUIHUD extends AGUIBase{
	private static final int HUD_WIDTH = 400;
	private static final int HUD_HEIGHT = 140;
	private final EntityVehicleF_Physics vehicle;

	public GUIHUD(EntityVehicleF_Physics vehicle){
		this.vehicle = vehicle;
	}
	
	@Override
	public final void setupComponents(int guiLeft, int guiTop){
		//Add instruments.  These go wherever they are specified in the JSON.
		for(Integer instrumentNumber : vehicle.instruments.keySet()){
			if(!vehicle.definition.instruments.get(instrumentNumber).placeOnPanel){
				addComponent(new GUIComponentInstrument(guiLeft, guiTop, instrumentNumber, vehicle));
			}
		}
		//Now add part instruments.
		for(APart part : vehicle.parts){
			for(Integer instrumentNumber : part.instruments.keySet()){
				if(!part.definition.instruments.get(instrumentNumber).placeOnPanel){
					addComponent(new GUIComponentInstrument(guiLeft, guiTop, instrumentNumber, part));
				}
			}
		}
	}

	@Override
	public void setStates(){}
	
	@Override
	public boolean renderBackground(){
		return InterfaceClient.inFirstPerson() ? !ConfigSystem.configObject.clientRendering.transpHUD_1P.value : !ConfigSystem.configObject.clientRendering.transpHUD_3P.value;
	}
	
	@Override
	public GUILightingMode getGUILightMode(){
		return vehicle.renderTextLit() ? GUILightingMode.LIT : GUILightingMode.DARK;
	}
	
	@Override
	public EntityVehicleF_Physics getGUILightSource(){
		return vehicle;
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
	public boolean renderTranslucent(){
		return true;
	}
	
	@Override
	public String getTexture(){
		return vehicle.definition.motorized.hudTexture != null ? vehicle.definition.motorized.hudTexture : "mts:textures/guis/hud.png";
	}
}
