package minecrafttransportsimulator;

import java.io.File;

import minecrafttransportsimulator.blocks.core.BlockBench;
import minecrafttransportsimulator.blocks.core.TileEntityTrafficSignalController;
import minecrafttransportsimulator.blocks.pole.BlockPoleSign;
import minecrafttransportsimulator.guis.GUIInstruments;
import minecrafttransportsimulator.guis.GUIManual;
import minecrafttransportsimulator.guis.GUIPartBench;
import minecrafttransportsimulator.guis.GUISign;
import minecrafttransportsimulator.guis.GUITrafficSignalController;
import minecrafttransportsimulator.items.core.ItemManual;
import minecrafttransportsimulator.mcinterface.MTSPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
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
	public void openGUI(Object clicked, MTSPlayer clicker){
		if(clicked instanceof EntityVehicleE_Powered){
			FMLCommonHandler.instance().showGuiScreen(new GUIInstruments((EntityVehicleE_Powered) clicked, clicker));
		}else if(clicked instanceof BlockBench){
			FMLCommonHandler.instance().showGuiScreen(new GUIPartBench((BlockBench) clicked, clicker));
		}else if(clicked instanceof ItemStack && ((ItemStack) clicked).getItem() instanceof ItemManual){
			FMLCommonHandler.instance().showGuiScreen(new GUIManual((ItemStack) clicked));
		}else if(clicked instanceof BlockPoleSign){
			FMLCommonHandler.instance().showGuiScreen(new GUISign((BlockPoleSign) clicked, clicker));
		}else if(clicked instanceof TileEntityTrafficSignalController){
			FMLCommonHandler.instance().showGuiScreen(new GUITrafficSignalController((TileEntityTrafficSignalController) clicked));
		}
	}
	
	@Override
	public void playSound(Vec3d soundPosition, String soundName, float volume, float pitch){
		SFXSystem.playSound(soundPosition, soundName, volume, pitch);
	}
	
	@Override
	public void addVehicleEngineSound(EntityVehicleE_Powered vehicle, APartEngine engine){
		SFXSystem.addVehicleEngineSound(vehicle, engine);
	}
}
