package minecrafttransportsimulator.systems;

import net.minecraft.block.material.Material;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleDrip;
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
			//Override color to always be black.
			this.setRBGColorF(0, 0, 0);
		}
	}
	
	public static class FuelDropParticleFX extends ParticleDrip{
		public FuelDropParticleFX(World world, double posX, double posY, double posZ){
			super(world, posX, posY, posZ, Material.LAVA);
		}
	}
	
	public static class ColoredSmokeFX extends Particle{
		public ColoredSmokeFX(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float red, float green, float blue, float scale, float alpha){
			super(world, posX, posY, posZ);
			this.particleMaxAge = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
			this.motionX = motionX;
			this.motionY = motionY;
			this.motionZ = motionZ;
			this.particleRed = red;
			this.particleGreen = green;
			this.particleBlue = blue;
			this.particleScale = scale;
			this.particleAlpha = alpha;
			setParticleTextureIndex(7);
		}
		
		@Override
		public void onUpdate(){
			super.onUpdate();
			//Need to set the texture to smaller particles as we get older.
			setParticleTextureIndex(7 - particleAge * 8 / particleMaxAge);
			//Need to make sure we only go upwards.
			this.motionX *= 0.9;
			this.motionY += 0.004;
	        this.motionZ *= 0.9;
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
