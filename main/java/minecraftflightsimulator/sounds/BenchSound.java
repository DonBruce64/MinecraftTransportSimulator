package minecraftflightsimulator.sounds;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import net.minecraft.util.ResourceLocation;

public class BenchSound extends DynamicSound{
	private final TileEntityPropellerBench bench;

	public BenchSound(TileEntityPropellerBench bench){
		super(new ResourceLocation("mfs", "bench_running"), bench, 0.75F);
		this.bench = (TileEntityPropellerBench) this.tile;
	}
	
	@Override
	public void update(){
		super.update();
		this.donePlaying = !bench.isOn;
	}
}
