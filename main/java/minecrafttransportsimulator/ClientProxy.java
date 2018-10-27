package minecrafttransportsimulator;

import java.io.File;

import minecrafttransportsimulator.blocks.core.BlockPartBench;
import minecrafttransportsimulator.blocks.decor.BlockDecor6AxisSign;
import minecrafttransportsimulator.guis.GUIInstruments;
import minecrafttransportsimulator.guis.GUIManual;
import minecrafttransportsimulator.guis.GUIPartBench;
import minecrafttransportsimulator.guis.GUISign;
import minecrafttransportsimulator.items.core.ItemManual;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Class responsible for performing client-only updates and operations.
 * This class acts as a forwarding system rather than code executor.
 * Code operations should be in their own classes, if possible.
 * 
 * @author don_bruce
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy{
	
	@Override
	public void initConfig(File configFile){
		ConfigSystem.initClient(configFile);
	}

	@Override
	public void initControls(){
		ControlSystem.init();
	}
	@Override
	public void openGUI(Object clicked, EntityPlayer clicker){
		if(clicked instanceof EntityMultipartE_Vehicle){
			FMLCommonHandler.instance().showGuiScreen(new GUIInstruments((EntityMultipartE_Vehicle) clicked, clicker));
		}else if(clicked instanceof BlockPartBench){
			FMLCommonHandler.instance().showGuiScreen(new GUIPartBench((BlockPartBench) clicked, clicker));
		}else if(clicked instanceof ItemStack && ((ItemStack) clicked).getItem() instanceof ItemManual){
			FMLCommonHandler.instance().showGuiScreen(new GUIManual((ItemStack) clicked));
		}else if(clicked instanceof BlockDecor6AxisSign){
			FMLCommonHandler.instance().showGuiScreen(new GUISign((BlockDecor6AxisSign) clicked, clicker));
		}
	}
	
	@Override
	public void playSound(Vec3d soundPosition, String soundName, float volume, float pitch){
		SFXSystem.playSound(soundPosition, soundName, volume, pitch);
	}
	
	@Override
	public void addVehicleEngineSound(EntityMultipartE_Vehicle vehicle, APartEngine engine){
		SFXSystem.addVehicleEngineSound(vehicle, engine);
	}
}
