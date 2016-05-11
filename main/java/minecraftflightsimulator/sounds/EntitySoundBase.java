package minecraftflightsimulator.sounds;

import minecraftflightsimulator.entities.core.EntityPlane;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class EntitySoundBase extends MovingSound{
	protected Entity entity;

	public EntitySoundBase(ResourceLocation location, Entity entity, float volume){
		super(location);
		this.xPosF=(float) entity.posX;
		this.yPosF=(float) entity.posY;
		this.zPosF=(float) entity.posZ;
		this.volume=volume;
		this.repeat=true;
	}
	
	@Override
	public void update(){
		this.xPosF=(float) entity.posX;
		this.yPosF=(float) entity.posY;
		this.zPosF=(float) entity.posZ;
		this.donePlaying=false;
	}
}
