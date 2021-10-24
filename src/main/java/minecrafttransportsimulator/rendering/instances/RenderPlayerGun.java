package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public class RenderPlayerGun extends ARenderEntity<EntityPlayerGun>{
	
	@Override
	public boolean disableRendering(EntityPlayerGun entity, float partialTicks){
		return true;
	}
}
