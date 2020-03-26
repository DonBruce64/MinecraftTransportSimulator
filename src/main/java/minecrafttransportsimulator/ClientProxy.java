package minecrafttransportsimulator;

import minecrafttransportsimulator.blocks.core.BlockBench;
import minecrafttransportsimulator.blocks.core.TileEntityTrafficSignalController;
import minecrafttransportsimulator.blocks.pole.BlockPoleSign;
import minecrafttransportsimulator.guis.GUIPartBench;
import minecrafttransportsimulator.guis.GUISign;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.guis.instances.GUITrafficSignalController;
import minecrafttransportsimulator.items.packs.ItemBooklet;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.entity.player.EntityPlayer;
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
		if(clicked instanceof BlockBench){
			FMLCommonHandler.instance().showGuiScreen(new GUIPartBench((BlockBench) clicked, clicker));
		}else if(clicked instanceof ItemBooklet){
			WrapperGUI.openGUI(new GUIBooklet((ItemBooklet) clicked));
		}else if(clicked instanceof BlockPoleSign){
			FMLCommonHandler.instance().showGuiScreen(new GUISign((BlockPoleSign) clicked, clicker));
		}else if(clicked instanceof TileEntityTrafficSignalController){
			WrapperGUI.openGUI(new GUITrafficSignalController((TileEntityTrafficSignalController) clicked));
		}
	}
}
