package minecrafttransportsimulator.packets.general;

import minecrafttransportsimulator.mcinterface.MTSNetwork.MTSPacket;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to servers when the player closes a
 * manual.  Used to store the page number the player was last
 * looking at so when they re-open the manual the same page
 * will be displayed.
 * 
 * @author don_bruce
 */
public class PacketServerManualPageUpdate extends MTSPacket{
	private byte pageNumber;

	public PacketServerManualPageUpdate() {}
	
	public PacketServerManualPageUpdate(byte pageNumber){
		this.pageNumber = pageNumber;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		pageNumber = tag.getByte("pageNumber");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		tag.setByte("pageNumber", pageNumber);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		ItemStack stack = player.geHeldStack();
		if(!stack.hasTagCompound()){
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setByte("pageNumber", pageNumber);
	}
}
