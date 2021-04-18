package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public class RenderBullet extends ARenderEntity<EntityBullet>{
	
	@Override
	public String getTexture(EntityBullet bullet){
		return bullet.definition.getTextureLocation(bullet.subName);
	}
}
