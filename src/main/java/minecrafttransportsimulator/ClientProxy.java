package minecrafttransportsimulator;

import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.items.packs.ItemBooklet;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.entity.player.EntityPlayer;
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
	public void openGUI(Object clicked, EntityPlayer clicker){
		if(clicked instanceof ItemBooklet){
			WrapperGUI.openGUI(new GUIBooklet((ItemBooklet) clicked));
		}
	}
}
