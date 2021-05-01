package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.rendering.components.ARenderEntityMultipart;

public class RenderPlayerGun extends ARenderEntityMultipart<EntityPlayerGun>{
	
	@Override
	public boolean disableMainRendering(EntityPlayerGun entity, float partialTicks){
		return true;
	}
}
