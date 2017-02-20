package minecraftflightsimulator;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.guis.GUIInstrumentsFlyer;
import minecraftflightsimulator.guis.GUIPropellerBench;
import minecraftflightsimulator.sounds.BenchSound;
import minecraftflightsimulator.sounds.EngineSound;
import minecraftflightsimulator.systems.ClientEventSystem;
import minecraftflightsimulator.systems.ConfigSystem;
import minecraftflightsimulator.systems.ControlSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

/**Class responsible for performing client-only updates and operations.
 * Any version-updatable, client-based method should be put in here.
 * 
 * @author don_bruce
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy{	
	
	@Override
	public void preInit(FMLPreInitializationEvent event){
		super.preInit(event);
		ConfigSystem.initClient();
		MFSClientRegistry.preInit();
	}
	
	@Override
	public void init(FMLInitializationEvent event){
		super.init(event);
		MFSClientRegistry.init();
		ControlSystem.init();
		MinecraftForge.EVENT_BUS.register(ClientEventSystem.instance);
		FMLCommonHandler.instance().bus().register(ClientEventSystem.instance);
	}
	
	@Override
	public void openGUI(Object clicked, EntityPlayer clicker){
		if(clicked instanceof EntityPlane){
			FMLCommonHandler.instance().showGuiScreen(new GUIInstrumentsFlyer((EntityVehicle) clicked, clicker));
		}else if(clicked instanceof TileEntityPropellerBench){
			FMLCommonHandler.instance().showGuiScreen(new GUIPropellerBench((TileEntityPropellerBench) clicked, clicker));
		}
	}
	
	/**
	 * Plays a sound in the same way as World.playSound.
	 * Placed here for ease of version updates.
	 */
	@Override
	public void playSound(Entity noisyEntity, String soundName, float volume, float pitch){
		if(noisyEntity.worldObj.isRemote){
			double soundDistance = Minecraft.getMinecraft().thePlayer.getDistance(noisyEntity.posX, noisyEntity.posY, noisyEntity.posZ);
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
					if(engine.state.running || engine.internalFuel > 0){
						sound = engine.getEngineSound();
						handler.playSound(sound);
					}
				}
			}
			//TODO add drips here.
			if(engine.temp > engine.engineOverheatTemp1){
				if(Minecraft.getMinecraft().effectRenderer != null){
					Minecraft.getMinecraft().theWorld.spawnParticle("smoke", engine.posX, engine.posY + 0.5, engine.posZ, 0, 0.15, 0);
					if(engine.temp > engine.engineOverheatTemp2){
						Minecraft.getMinecraft().theWorld.spawnParticle("largesmoke", engine.posX, engine.posY + 0.5, engine.posZ, 0, 0.15, 0);
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
					if(bench.isRunning()){
						sound = new BenchSound(bench);
						handler.playSound(sound);
					}
				}
			}
		}
		return sound;
	}
}
