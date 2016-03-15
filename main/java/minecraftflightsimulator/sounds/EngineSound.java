package minecraftflightsimulator.sounds;

import minecraftflightsimulator.entities.EntityEngine;
import net.minecraft.util.ResourceLocation;

public class EngineSound extends EntitySoundBase{
	private EntityEngine engine;
	public float pitchFactor;
	
	public EngineSound(ResourceLocation location, EntityEngine engine, float volume, float pitchFactor){
		super(location, engine, volume);
		this.entity = this.engine = engine;
		this.pitchFactor = pitchFactor;
	}
	
	@Override
	public void update(){
		if(engine.fueled || engine.internalFuel > 0){
			super.update();
			this.field_147663_c=((float) engine.engineRPM/pitchFactor);
		}else{
			this.donePlaying=true;
		}
	}
}
