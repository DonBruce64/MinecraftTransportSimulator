package minecraftflightsimulator.sounds;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

public class BenchSound extends MovingSound{
	private final TileEntityPropellerBench bench;

	public BenchSound(TileEntityPropellerBench bench){
		super(new ResourceLocation("mfs", "bench_running"));
		this.bench = bench;
		this.xPosF= bench.xCoord;
		this.yPosF= bench.yCoord;
		this.zPosF= bench.zCoord;
		this.volume=0.75F;
		this.repeat=true;
	}
	
	@Override
	public void update(){
		this.donePlaying = !bench.isOn;
	}
}
