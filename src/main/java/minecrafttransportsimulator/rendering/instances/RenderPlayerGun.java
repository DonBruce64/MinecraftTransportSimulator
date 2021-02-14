package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;
import minecrafttransportsimulator.vehicles.parts.APart;

public class RenderPlayerGun extends ARenderEntity<EntityPlayerGun>{
	
	@Override
	public void renderModel(EntityPlayerGun entity, float partialTicks){
		//Just render the gun.  We don't have an actual model.
		for(APart part : entity.parts){
			part.getRenderer().render(part, partialTicks);
		}
	}
}
