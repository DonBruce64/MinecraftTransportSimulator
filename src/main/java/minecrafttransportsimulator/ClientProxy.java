package minecrafttransportsimulator;

import minecrafttransportsimulator.blocks.core.BlockBench;
import minecrafttransportsimulator.blocks.core.TileEntityTrafficSignalController;
import minecrafttransportsimulator.blocks.pole.BlockPoleSign;
import minecrafttransportsimulator.guis.GUIPartBench;
import minecrafttransportsimulator.guis.GUISign;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.guis.instances.GUITrafficSignalController;
import minecrafttransportsimulator.items.packs.ItemBooklet;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.VehicleSoundSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.entity.player.EntityPlayer;
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
	public void initControls(){
		ControlSystem.init();
	}
	@Override
	public void openGUI(Object clicked, EntityPlayer clicker){
		if(clicked instanceof EntityVehicleE_Powered){
			WrapperGUI.openGUI(new GUIInstruments((EntityVehicleE_Powered) clicked, clicker));
		}else if(clicked instanceof BlockBench){
			FMLCommonHandler.instance().showGuiScreen(new GUIPartBench((BlockBench) clicked, clicker));
		}else if(clicked instanceof ItemBooklet){
			WrapperGUI.openGUI(new GUIBooklet((ItemBooklet) clicked));
		}else if(clicked instanceof BlockPoleSign){
			FMLCommonHandler.instance().showGuiScreen(new GUISign((BlockPoleSign) clicked, clicker));
		}else if(clicked instanceof TileEntityTrafficSignalController){
			WrapperGUI.openGUI(new GUITrafficSignalController((TileEntityTrafficSignalController) clicked));
		}
	}
	
	@Override
	public void playSound(Vec3d soundPosition, String soundName, float volume, float pitch, EntityVehicleE_Powered optionalVehicle){
		VehicleSoundSystem.playSound(soundPosition, soundName, volume, pitch, optionalVehicle);
	}
	
	@Override
	public void addVehicleEngineSound(EntityVehicleE_Powered vehicle, APartEngine engine){
		VehicleSoundSystem.addVehicleEngineSound(vehicle, engine);
	}
}
