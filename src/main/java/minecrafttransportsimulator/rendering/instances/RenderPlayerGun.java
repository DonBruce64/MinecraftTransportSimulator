package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPlayerGun extends ARenderEntityDefinable<EntityPlayerGun>{
	
	@Override
	public boolean disableRendering(EntityPlayerGun entity, float partialTicks){
		return true;
	}
}
