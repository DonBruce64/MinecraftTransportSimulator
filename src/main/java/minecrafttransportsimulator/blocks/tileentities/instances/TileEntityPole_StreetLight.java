package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.rendering.components.LightType;

/**Lighted pole component.  Renders a constant beam when turned on.
 * 
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component{
	
	public boolean active;
	
	public TileEntityPole_StreetLight(TileEntityPole core, ItemPoleComponent item){
		super(core, item);
		active = true;
		activeVariables.add(LightType.STREETLIGHT.lowercaseName);
	}

	@Override
	public float lightLevel(){
		return active ? 12F/15F : 0.0F;
	}
}
