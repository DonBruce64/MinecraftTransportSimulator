package minecrafttransportsimulator;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.entity.player.EntityPlayer;

/**Contains registration methods used by {@link MTSRegistry} and methods overridden by ClientProxy. 
 * See the latter for more info on overridden methods.
 * 
 * @author don_bruce
 */
public class CommonProxy{	
	public void openGUI(Object clicked, EntityPlayer clicker){}
}
