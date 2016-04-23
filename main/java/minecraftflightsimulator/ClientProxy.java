package minecraftflightsimulator;

import minecraftflightsimulator.entities.EntityBase;
import minecraftflightsimulator.entities.EntityEngine;
import minecraftflightsimulator.entities.EntityEngineLarge;
import minecraftflightsimulator.entities.EntityEngineSmall;
import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.entities.EntityPlaneChest;
import minecraftflightsimulator.entities.EntityPropeller;
import minecraftflightsimulator.entities.EntitySeat;
import minecraftflightsimulator.entities.EntityWheelLarge;
import minecraftflightsimulator.entities.EntityWheelSmall;
import minecraftflightsimulator.helpers.ControlHelper;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.modelrenders.RenderEngine;
import minecraftflightsimulator.modelrenders.RenderNull;
import minecraftflightsimulator.modelrenders.RenderPlaneChest;
import minecraftflightsimulator.modelrenders.RenderPropeller;
import minecraftflightsimulator.modelrenders.RenderSeat;
import minecraftflightsimulator.modelrenders.RenderWheel;
import minecraftflightsimulator.planes.MC172.EntityMC172;
import minecraftflightsimulator.planes.MC172.RenderMC172;
import minecraftflightsimulator.planes.Trimotor.EntityTrimotor;
import minecraftflightsimulator.planes.Trimotor.RenderTrimotor;
import minecraftflightsimulator.sounds.EngineSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy{	
	
	@Override
	public void init(){
		super.init();
		initEntityRenders();
		ControlHelper.init();
		MinecraftForge.EVENT_BUS.register(ClientEventHandler.instance);
		FMLCommonHandler.instance().bus().register(ClientEventHandler.instance);
	}
	
	private void initEntityRenders(){
		RenderingRegistry.registerEntityRenderingHandler(EntityMC172.class, new RenderMC172());
		RenderingRegistry.registerEntityRenderingHandler(EntityTrimotor.class, new RenderTrimotor());
		RenderingRegistry.registerEntityRenderingHandler(EntitySeat.class, new RenderSeat());
		RenderingRegistry.registerEntityRenderingHandler(EntityPlaneChest.class, new RenderPlaneChest());
		RenderingRegistry.registerEntityRenderingHandler(EntityWheelSmall.class, new RenderWheel());
		RenderingRegistry.registerEntityRenderingHandler(EntityWheelLarge.class, new RenderWheel());
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
				rider.rotationYaw+=180;
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
	public EngineSound updateEngineSound(EngineSound sound, EntityEngine engine){
		if(sound == null){
			sound = engine.getEngineSound();
		}else{
			SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
			if(!handler.isSoundPlaying(sound)){
				if(engine.fueled || engine.internalFuel > 0){
					sound = new EngineSound(sound.getPositionedSoundLocation(), engine, sound.getVolume(), sound.pitchFactor);
					handler.playSound(sound);
				}
			}
		}
		return sound;
	}
}
