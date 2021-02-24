package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.rendering.components.ARenderEntityMultipart;

public class RenderPlayerGun extends ARenderEntityMultipart<EntityPlayerGun>{
	
	@Override
	public void renderModel(EntityPlayerGun entity, float partialTicks){
		//Don't render anything, as the player gun doesn't have a model.
	}
}
