package minecraftflightsimulator;

import minecraftflightsimulator.entities.core.EntityBase;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityEngineLarge;
import minecraftflightsimulator.entities.parts.EntityEngineSmall;
import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import minecraftflightsimulator.entities.parts.EntityPontoon;
import minecraftflightsimulator.entities.parts.EntityPontoonDummy;
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.entities.parts.EntityWheelLarge;
import minecraftflightsimulator.entities.parts.EntityWheelSmall;
import minecraftflightsimulator.helpers.ControlHelper;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.modelrenders.RenderEngine;
import minecraftflightsimulator.modelrenders.RenderNull;
import minecraftflightsimulator.modelrenders.RenderPlaneChest;
import minecraftflightsimulator.modelrenders.RenderPontoon;
import minecraftflightsimulator.modelrenders.RenderPropeller;
import minecraftflightsimulator.modelrenders.RenderSeat;
import minecraftflightsimulator.modelrenders.RenderSkid;
import minecraftflightsimulator.modelrenders.RenderWheel;
import minecraftflightsimulator.planes.MC172.EntityMC172;
import minecraftflightsimulator.planes.MC172.RenderMC172;
import minecraftflightsimulator.planes.Otter.EntityOtter;
import minecraftflightsimulator.planes.Otter.RenderOtter;
import minecraftflightsimulator.planes.PZLP11.EntityPZLP11;
import minecraftflightsimulator.planes.PZLP11.RenderPZLP11;
import minecraftflightsimulator.planes.Trimotor.EntityTrimotor;
import minecraftflightsimulator.planes.Trimotor.RenderTrimotor;
import minecraftflightsimulator.planes.Vulcanair.EntityVulcanair;
import minecraftflightsimulator.planes.Vulcanair.RenderVulcanair;
import minecraftflightsimulator.sounds.EngineSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy{	
	
	@Override
	public void preInit(){
		super.preInit();
	}
	
	@Override
	public void init(){
		super.init();
		initEntityRenders();
		ControlHelper.init();
		MinecraftForge.EVENT_BUS.register(ClientEventHandler.instance);
		FMLCommonHandler.instance().bus().register(ClientEventHandler.instance);
	}
	
	private void initEntityRenders(){
		//MinecraftForgeClient.registerItemRenderer(this.item, new ItemRender());
		RenderingRegistry.registerEntityRenderingHandler(EntityMC172.class, new RenderMC172());
		RenderingRegistry.registerEntityRenderingHandler(EntityTrimotor.class, new RenderTrimotor());
		RenderingRegistry.registerEntityRenderingHandler(EntityVulcanair.class, new RenderVulcanair());
		RenderingRegistry.registerEntityRenderingHandler(EntityOtter.class, new RenderOtter());
		RenderingRegistry.registerEntityRenderingHandler(EntityPZLP11.class, new RenderPZLP11());
		
		RenderingRegistry.registerEntityRenderingHandler(EntitySeat.class, new RenderSeat());
		RenderingRegistry.registerEntityRenderingHandler(EntityPlaneChest.class, new RenderPlaneChest());
		RenderingRegistry.registerEntityRenderingHandler(EntityWheelSmall.class, new RenderWheel());
		RenderingRegistry.registerEntityRenderingHandler(EntityWheelLarge.class, new RenderWheel());
		RenderingRegistry.registerEntityRenderingHandler(EntitySkid.class, new RenderSkid());
		RenderingRegistry.registerEntityRenderingHandler(EntityPontoon.class, new RenderPontoon());
		RenderingRegistry.registerEntityRenderingHandler(EntityPontoonDummy.class, new RenderNull());
		RenderingRegistry.registerEntityRenderingHandler(EntityPropeller.class, new RenderPropeller());
		RenderingRegistry.registerEntityRenderingHandler(EntityEngineSmall.class, new RenderEngine());
		RenderingRegistry.registerEntityRenderingHandler(EntityEngineLarge.class, new RenderEngine());
		RenderingRegistry.registerEntityRenderingHandler(EntityBase.class, new RenderNull());
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
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView==0 && seat.riddenByEntity.equals(Minecraft.getMinecraft().thePlayer)){
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
					Minecraft.getMinecraft().effectRenderer.addEffect(new EntitySmokeFX(engine.worldObj, engine.posX, engine.posY + 0.5, engine.posZ, 0, 0.15, 0));
					if(engine.engineTemp > 107.222){
						Minecraft.getMinecraft().effectRenderer.addEffect(new EntitySmokeFX(engine.worldObj, engine.posX, engine.posY + 0.5, engine.posZ, 0, 0.15, 0, 2.5F));
					}
					if(engine.engineTemp > 121.111){
						Minecraft.getMinecraft().effectRenderer.addEffect(new EntityFlameFX(engine.worldObj, engine.posX, engine.posY + 0.5, engine.posZ, 0, 0.15, 0));
					}
				}
			}
		}
		return sound;
	}
}
