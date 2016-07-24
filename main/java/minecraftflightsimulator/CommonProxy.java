package minecraftflightsimulator;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.sounds.BenchSound;
import minecraftflightsimulator.sounds.EngineSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;


public class CommonProxy{

	public void preInit(){}
	
	public void init(){
		MFSRegistry.instance.init();
	}
		
	public void playSound(Entity noisyEntity, String soundName, float volume, float pitch){}
	public void updateSeatedRider(EntitySeat seat, EntityLivingBase rider){}
	public EngineSound updateEngineSoundAndSmoke(EngineSound sound, EntityEngine engine){return null;}
	public BenchSound updateBenchSound(BenchSound sound, TileEntityPropellerBench bench){return null;}
}
