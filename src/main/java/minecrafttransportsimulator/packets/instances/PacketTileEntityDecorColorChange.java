package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet sent to decors to update their their subName (color).  This gets sent from
 * a client when they change the color in the paint gun GUI.
 * 
 * @author don_bruce
 */
public class PacketTileEntityDecorColorChange extends APacketTileEntity<TileEntityDecor>{
	private final ItemDecor newDecorItem;
	
	public PacketTileEntityDecorColorChange(TileEntityDecor decor, ItemDecor newDecorItem){
		super(decor);
		this.newDecorItem = newDecorItem;
	}
	
	public PacketTileEntityDecorColorChange(ByteBuf buf){
		super(buf);
		this.newDecorItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(newDecorItem.definition.packID, buf);
		writeStringToBuffer(newDecorItem.definition.systemName, buf);
		writeStringToBuffer(newDecorItem.subName, buf);
	}
	
	@Override
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, TileEntityDecor decor){
		IWrapperInventory inventory = player.getInventory();
		if(player.isCreative() || inventory.hasMaterials(newDecorItem, false, true)){
			//Remove livery materials (if required) and set new subName.
			if(!player.isCreative()){
				inventory.removeMaterials(newDecorItem, false, true);
			}
			decor.currentSubName = newDecorItem.subName;
			return true;
		}
		return false;
	}
}
