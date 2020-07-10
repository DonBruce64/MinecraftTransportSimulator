package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperEntityPlayer;
import mcinterface.InterfaceNetwork;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;

/**Packet class that includes a default implementation for transmitting a tile entity
 * to allow tile entity-specific interactions on the other side of the network.
 *
 * @author don_bruce
 */
public abstract class APacketTileEntity<TileEntityType extends ATileEntityBase<?>> extends APacketBase{
	private final Point3i position;
	
	public APacketTileEntity(TileEntityType tile){
		super(null);
		this.position = tile.position;
	}
	
	public APacketTileEntity(ByteBuf buf){
		super(buf);
		this.position = new Point3i(buf.readInt(), buf.readInt(), buf.readInt());
	};

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(position.x);
		buf.writeInt(position.y);
		buf.writeInt(position.z);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void handle(WrapperWorld world, WrapperEntityPlayer player){
		TileEntityType tile = (TileEntityType) world.getTileEntity(position); 
		if(tile != null && tile.world != null){
			if(handle(world, player, tile) && !world.isClient()){
				world.markTileEntityChanged(position);
				InterfaceNetwork.sendToAllClients(this);
			}
		}
	}
	
	/**
	 *  Handler method with an extra parameter for the tile entity that this packet
	 *  is associated with. If the tile entity is null, or if it hasn't loaded it's world,
	 *  then this method won't be called.  Saves having to do null checks for every packet type.
	 *  If this is handled on the server, and a packet shouldn't be sent to all clients (like
	 *  if the action failed due to an issue) return false.  Otherwise, return true to 
	 *  send this packet on to all clients.  Return method has no function on clients.
	 */
	protected abstract boolean handle(WrapperWorld world, WrapperEntityPlayer player, TileEntityType tile);
}
