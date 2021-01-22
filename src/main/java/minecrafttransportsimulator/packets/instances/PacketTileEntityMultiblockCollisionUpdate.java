package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityMultiblock;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketTileEntity;

/**Packet sent to multiblocks when their collision data changes.  This can either be the blocks that make up the
 * multiblock's collision, blocks blocking the collision, or the active state in general.  This is sent from servers
 * to all clients when collision checks are requested for multiblocks.
 * 
 * @author don_bruce
 */
public class PacketTileEntityMultiblockCollisionUpdate extends APacketTileEntity<ATileEntityMultiblock<?>>{
	private final List<Point3i> collisionBlockOffsets;
	private final List<Point3i> collidingBlockOffsets;
	
	public PacketTileEntityMultiblockCollisionUpdate(ATileEntityMultiblock<?> multiblock){
		super(multiblock);
		collisionBlockOffsets = multiblock.collisionBlockOffsets;
		collidingBlockOffsets = multiblock.collidingBlockOffsets;
	}
	
	public PacketTileEntityMultiblockCollisionUpdate(ByteBuf buf){
		super(buf);
		this.collisionBlockOffsets = new ArrayList<Point3i>();
		int collisionBlockOffsetCount = buf.readInt();
		for(int i=0; i<collisionBlockOffsetCount; ++i){
			collisionBlockOffsets.add(readPoint3iFromBuffer(buf));
		}
		
		this.collidingBlockOffsets = new ArrayList<Point3i>();
		int collidingBlockOffsetCount = buf.readInt();
		for(int i=0; i<collidingBlockOffsetCount; ++i){
			collidingBlockOffsets.add(readPoint3iFromBuffer(buf));
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(collisionBlockOffsets.size());
		for(Point3i point : collisionBlockOffsets){
			writePoint3iToBuffer(point, buf);
		}
		
		buf.writeInt(collidingBlockOffsets.size());
		for(Point3i point : collidingBlockOffsets){
			writePoint3iToBuffer(point, buf);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, ATileEntityMultiblock<?> multiblock){
		multiblock.collisionBlockOffsets.clear();
		multiblock.collisionBlockOffsets.addAll(collisionBlockOffsets);
		multiblock.collidingBlockOffsets.clear();
		multiblock.collidingBlockOffsets.addAll(collidingBlockOffsets);
		if(!multiblock.isActive() && multiblock.collidingBlockOffsets.isEmpty()){
			multiblock.setActive(true);
		}
		return false;
	}
}
