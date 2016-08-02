package minecraftflightsimulator;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.sounds.BenchSound;
import minecraftflightsimulator.sounds.EngineSound;
import minecraftflightsimulator.utilities.ClientEventHandler;
import minecraftflightsimulator.utilities.ControlHelper;
import minecraftflightsimulator.utilities.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy{	
	
	@Override
	public void preInit(){
		super.preInit();
		MFSClientRegistry.instance.preInit();
	}
	
	@Override
	public void init(){
		super.init();
		MFSClientRegistry.instance.init();
		ControlHelper.init();
		MinecraftForge.EVENT_BUS.register(ClientEventHandler.instance);
		FMLCommonHandler.instance().bus().register(ClientEventHandler.instance);
	}

	@Override
	public void updateSeatedRider(EntitySeat seat, EntityLivingBase rider){		
		rider.renderYawOffset += seat.parent.rotationYaw - seat.parent.prevRotationYaw;
		if(RenderHelper.lockedView){
			rider.rotationYaw += seat.parent.rotationYaw - seat.parent.prevRotationYaw;
			if(seat.parent.rotationPitch > 90 || seat.parent.rotationPitch < -90){
				rider.rotationPitch -= seat.parent.rotationPitch - seat.parent.prevRotationPitch;
			}else{
				rider.rotationPitch += seat.parent.rotationPitch - seat.parent.prevRotationPitch;
			}
			if((seat.parent.rotationPitch > 90 || seat.parent.rotationPitch < -90) ^ seat.parent.prevRotationPitch > 90 || seat.parent.prevRotationPitch < -90){
				//rider.rotationYaw+=180;
			}
		}
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView==0 && seat.getRider().equals(Minecraft.getMinecraft().thePlayer)){
			RenderHelper.changeCameraRoll(seat.parent.rotationRoll);
		}else{
			RenderHelper.changeCameraRoll(0);
		}
		
		if(rider.equals(Minecraft.getMinecraft().thePlayer)){
			if(!Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen()){
				ControlHelper.controlCamera();
				if(seat.driver){
					if(seat.parent instanceof EntityPlane){
						ControlHelper.controlPlane((EntityPlane) seat.parent);
					}
				}
			}
		}
	}
	
	@Override
	public void playSound(Entity noisyEntity, String soundName, float volume, float pitch){
		if(noisyEntity.worldObj.isRemote){
			double soundDistance = Minecraft.getMinecraft().renderViewEntity.getDistance(noisyEntity.posX, noisyEntity.posY, noisyEntity.posZ);
	        PositionedSoundRecord sound = new PositionedSoundRecord(new ResourceLocation(soundName), volume, pitch, (float)noisyEntity.posX, (float)noisyEntity.posY, (float)noisyEntity.posZ);
	        if(soundDistance > 10.0D){
	        	Minecraft.getMinecraft().getSoundHandler().playDelayedSound(sound, (int)(soundDistance/2));
	        }else{
	        	Minecraft.getMinecraft().getSoundHandler().playSound(sound);
	        }
		}
	}
	
	@Override
	public EngineSound updateEngineSoundAndSmoke(EngineSound sound, EntityEngine engine){
		if(engine.worldObj.isRemote){
			if(sound == null){
				sound = engine.getEngineSound();
			}else{
				SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
				if(!handler.isSoundPlaying(sound)){
					if(engine.fueled || engine.internalFuel > 0){
						sound = engine.getEngineSound();
						handler.playSound(sound);
					}
				}
			}
			
			if(engine.engineTemp > 93.3333){
				if(Minecraft.getMinecraft().effectRenderer != null){
					Minecraft.getMinecraft().theWorld.spawnParticle("smoke", engine.posX, engine.posY + 0.5, engine.posZ, 0, 0.15, 0);
					if(engine.engineTemp > 107.222){
						Minecraft.getMinecraft().theWorld.spawnParticle("largesmoke", engine.posX, engine.posY + 0.5, engine.posZ, 0, 0.15, 0);
					}
					if(engine.engineTemp > 121.111){
						Minecraft.getMinecraft().theWorld.spawnParticle("flame", engine.posX, engine.posY + 0.5, engine.posZ, 0, 0.15, 0);
					}
				}
			}
		}
		return sound;
	}
	
	@Override
	public BenchSound updateBenchSound(BenchSound sound, TileEntityPropellerBench bench){
		if(bench.getWorldObj().isRemote){
			if(sound == null){
				sound = new BenchSound(bench);
			}else{
				SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
				if(!handler.isSoundPlaying(sound)){
					if(bench.isOn){
						sound = new BenchSound(bench);
						handler.playSound(sound);
					}
				}
			}
		}
		return sound;
	}
}
