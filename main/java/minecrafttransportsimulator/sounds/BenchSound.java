package minecrafttransportsimulator.sounds;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.systems.SFXSystem;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;

public class BenchSound extends MovingSound{
	private final TileEntityPropellerBench bench;

	public BenchSound(TileEntityPropellerBench bench){
		super(SFXSystem.getSoundEventFromName(MTS.MODID + ":bench_running"), SoundCategory.MASTER);
		this.volume=bench.getVolume();
		this.repeat=true;
		this.xPosF = bench.getPos().getX();
		this.yPosF = bench.getPos().getY();
		this.zPosF = bench.getPos().getZ();
		this.bench = bench;
	}
	
	@Override
	public void update(){
		this.volume = bench.getVolume();
		if(SFXSystem.isPlayerInsideEnclosedVehicle()){
			this.volume /= 2;
		}
		this.donePlaying = !bench.shouldSoundBePlaying();
		if(donePlaying){
			bench.setCurrentSound(null);
		}
	}
}
