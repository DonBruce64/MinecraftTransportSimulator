package minecrafttransportsimulator.systems;

import net.minecraft.block.material.Material;
import net.minecraft.client.particle.ParticleDrip;
import net.minecraft.client.particle.ParticleSmokeNormal;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**This class handles all effects for MTS.  Mainly a wrapper used for spawning various
 * particles from parts, but also contains a few custom classes for specific effects
 * as well.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class VehicleEffectsSystem{	
	
	public static class OilDropParticleFX extends ParticleDrip{
		public OilDropParticleFX(World world, double posX, double posY, double posZ){
			super(world, posX, posY, posZ, Material.LAVA);
		}
		
		@Override
		public void onUpdate(){
			super.onUpdate();
			this.setRBGColorF(0, 0, 0);
		}
	}
	
	public static class FuelDropParticleFX extends ParticleDrip{
		public FuelDropParticleFX(World world, double posX, double posY, double posZ){
			super(world, posX, posY, posZ, Material.LAVA);
		}
	}
	
	public static class WhiteSmokeFX extends ParticleSmokeNormal{
		public WhiteSmokeFX(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ){
			super(world, posX, posY, posZ, motionX, motionY, motionZ, 1.0F);
		}
		
		@Override
		public void onUpdate(){
			super.onUpdate();
			this.setRBGColorF(1, 1, 1);
		}
	}
	
	/**Implement this interface on any parts that spawns particles or FX, and do the spawning there.
	 * This will be called after rendering the part, so it's assured to only be on the client.
	 * **/
	public static interface FXPart{
		@SideOnly(Side.CLIENT)
		public abstract void spawnParticles();
	}
}
