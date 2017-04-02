package minecrafttransportsimulator.sounds;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.systems.SFXSystem;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

public class BenchSound extends MovingSound{
	private final TileEntityPropellerBench bench;

	public BenchSound(TileEntityPropellerBench bench){
		super(new ResourceLocation(MTS.MODID, "bench_running"));
		this.volume=0.75F;
		this.repeat=true;
		this.xPosF = bench.xCoord;
		this.yPosF = bench.yCoord;
		this.zPosF = bench.zCoord;
		this.bench = bench;
		
	}
	
	@Override
	public void update(){
		this.volume = SFXSystem.isPlayerInsideVehicle() ? 0.5F : 1.0F;
		this.donePlaying = bench.isInvalid() ? true : !bench.isRunning();
		if(donePlaying){
			bench.setCurrentSound(null);
		}
	}
}
