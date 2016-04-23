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
import minecraftflightsimulator.itemrenders.RenderItemEngine;
import minecraftflightsimulator.itemrenders.RenderItemPlane;
import minecraftflightsimulator.itemrenders.RenderItemPropeller;
import minecraftflightsimulator.itemrenders.RenderItemSeat;
import minecraftflightsimulator.itemrenders.RenderItemWheel;
import minecraftflightsimulator.modelrenders.RenderEngine;
import minecraftflightsimulator.modelrenders.RenderNull;
import minecraftflightsimulator.modelrenders.RenderPlaneChest;
import minecraftflightsimulator.modelrenders.RenderPropeller;
import minecraftflightsimulator.modelrenders.RenderSeat;
import minecraftflightsimulator.modelrenders.RenderWheel;
import minecraftflightsimulator.other.ClientController;
import minecraftflightsimulator.planes.MC172.EntityMC172;
import minecraftflightsimulator.planes.MC172.ModelMC172;
import minecraftflightsimulator.planes.MC172.RenderMC172;
import minecraftflightsimulator.planes.Trimotor.EntityTrimotor;
import minecraftflightsimulator.planes.Trimotor.ModelTrimotor;
import minecraftflightsimulator.planes.Trimotor.RenderTrimotor;
import minecraftflightsimulator.sounds.EngineSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy{
	private static boolean lockedView = true;
	private static int zoomLevel = 4;
	private static final String[] rollNames =new String[] {"camRoll", "R", "field_78495_O"};
	private static final String[] zoomNames = new String[] {"thirdPersonDistance", "E", "field_78490_B"};
	
	public static int hudMode = 2;
	public static KeyBinding configKey;
	
	@Override
	public void init(){
		super.init();
		initEntityRenders();
		initItemRenders();
		ClientController.init();
		configKey = new KeyBinding("key.config", Keyboard.KEY_P, "key.categories.mfs");
		ClientRegistry.registerKeyBinding(configKey);
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
	
	private void initItemRenders(){
		MinecraftForgeClient.registerItemRenderer(planeMC172, new RenderItemPlane(new ModelMC172()));
		MinecraftForgeClient.registerItemRenderer(planeTrimotor, new RenderItemPlane(new ModelTrimotor()));
		MinecraftForgeClient.registerItemRenderer(seat, new RenderItemSeat());
		MinecraftForgeClient.registerItemRenderer(wheel, new RenderItemWheel());
		MinecraftForgeClient.registerItemRenderer(propeller, new RenderItemPropeller());
		MinecraftForgeClient.registerItemRenderer(engineSmall, new RenderItemEngine());
		MinecraftForgeClient.registerItemRenderer(engineLarge, new RenderItemEngine());
	}
	
	@Override
	public void updateSeatedPlayer(EntitySeat part){
		EntityLivingBase rider = ((EntityLivingBase) part.riddenByEntity);
		rider.renderYawOffset += part.parent.rotationYaw - part.parent.prevRotationYaw;
		if(lockedView){
			rider.rotationYaw += part.parent.rotationYaw - part.parent.prevRotationYaw;
			
			if(part.parent.rotationPitch > 90 || part.parent.rotationPitch < -90){
				rider.rotationPitch -= part.parent.rotationPitch - part.parent.prevRotationPitch;
			}else{
				rider.rotationPitch += part.parent.rotationPitch - part.parent.prevRotationPitch;
			}
			
			if((part.parent.rotationPitch > 90 || part.parent.rotationPitch < -90) ^ part.parent.prevRotationPitch > 90 || part.parent.prevRotationPitch < -90){
				rider.rotationYaw+=180;
			}
		}
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView==0 && part.riddenByEntity.equals(Minecraft.getMinecraft().thePlayer)){
			this.changeCameraRoll(part.parent.rotationRoll);
		}else{
			this.changeCameraRoll(0);
		}
		
		
	}
	
	@Override
	public void checkKeyboard(EntitySeat seat){
		if(seat.riddenByEntity.equals(Minecraft.getMinecraft().thePlayer)){
			if(!Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen()){
				ClientController.controlCamera();
				if(seat.driver){
					if(seat.parent instanceof EntityPlane){
						ClientController.controlPlane((EntityPlane) seat.parent);
					}
				}
			}
		}
	}
	
	@Override
	public void changeCameraZoom(int zoom){
		if(zoomLevel < 15 && zoom == 1){
			++zoomLevel;
		}else if(zoomLevel > 4 && zoom == -1){
			--zoomLevel;
		}else if(zoom == 0){
			zoomLevel = 4;
		}else{
			return;
		}
			
		try{
			ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, zoomLevel, zoomNames);
		}catch (Exception e){
			System.err.println("ERROR IN AIRCRAFT ZOOM REFLECTION!");
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void changeCameraRoll(float roll){
		try{
			ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, roll, rollNames);
		}catch (Exception e){
			System.err.println("ERROR IN AIRCRAFT ROLL REFLECTION!");
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void changeCameraLock(){
		lockedView = !lockedView;
		Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147673_a(new ResourceLocation("gui.button.press")));
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
