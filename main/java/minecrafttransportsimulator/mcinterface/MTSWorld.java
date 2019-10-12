package minecrafttransportsimulator.mcinterface;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**Class used for getting world information, such as a specific entity,
 * block, or player.  Also gets state information about players, such
 * as sneaking and creative status, as well as entity inventories.
 * Basically, if it's in the world, and we need to know about it,
 * check here.
 * 
 * @author don_bruce
 */
public class MTSWorld {
	
	
	//---------------START OF ENTITY METHODS---------------//
	/**Gets the entity ID of an Entity.*/
	public static int getID(Entity entity){
		return entity.getEntityId();
	}

}
