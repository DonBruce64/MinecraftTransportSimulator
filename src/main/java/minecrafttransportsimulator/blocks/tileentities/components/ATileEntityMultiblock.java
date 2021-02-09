package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.jsondefs.AJSONMultblock;
import minecrafttransportsimulator.jsondefs.JSONCollisionArea;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityMultiblockCollisionUpdate;

/**Multblock collision tile entity.  Contains methods and data for keeping track
 * of collision block offsets and points, as well as the active/inactive state of the
 * TE.
 *
 * @author don_bruce
 */
public abstract class ATileEntityMultiblock<JSONDefinition extends AJSONMultblock<?>> extends ATileEntityBase<JSONDefinition>{
	public static final int MAX_COLLISION_DISTANCE = 32;
	
	public final List<Point3i> collisionBlockOffsets;
	public final List<Point3i> collidingBlockOffsets;
	private boolean isActive;
	
	
	public ATileEntityMultiblock(WrapperWorld world, Point3i position, WrapperNBT data){
		super(world, position, data);
		//Get the active state.
		this.isActive = data.getBoolean("isActive");
		
		//If we have points for collision due to use creating collision blocks, load them now.
		this.collisionBlockOffsets = data.getPoint3is("collisionBlockOffsets");
		this.collidingBlockOffsets = data.getPoint3is("collidingBlockOffsets");
	}
	
	/**
	 *  Returns true if this multiblock is active.  Active multiblocks may be considered
	 *  to have all their collision blocks and no blocking blocks.
	 */
	public boolean isActive(){
		return isActive;
	}
	
	/**
	 *  Sets this multblock as active or inactive.  This happens after collision creation for
	 *  setting it as active, or prior to destruction for inactive.  This method should
	 *  handle any logic that needs to happen once the block is active and valid.
	 */
	public void setActive(boolean active){
		isActive = active;
	}
	
	/**
	 *  Method to generate a set of collision points and heights for collision checks.
	 *  These should be fed into {@link #spawnCollisionBlocks(WrapperPlayer)} to do collision checks.
	 */
	protected Map<Point3i, Integer> generateCollisionPoints(){
		collisionBlockOffsets.clear();
		collidingBlockOffsets.clear();
		Map<Point3i, Integer> collisionHeightMap = new HashMap<Point3i, Integer>();
		//Do static block additions for static component.
		for(JSONCollisionArea collisionArea : definition.general.collisionAreas){
			for(double x=collisionArea.firstCorner.x; x<=collisionArea.secondCorner.x; x += 0.5){
				for(double z=collisionArea.firstCorner.z; z<=collisionArea.secondCorner.z; z += 0.5){
					Point3i testPoint = new Point3i(new Point3d(x, 0, z).rotateY(rotation));
					
					if(!testPoint.isZero() && !collisionBlockOffsets.contains(testPoint) && !collidingBlockOffsets.contains(testPoint)){
						//Offset the point to the global cordinate space, get the block, and offset back.
						testPoint.add(position);
						if(world.isAir(testPoint)){
							//Need a collision box here.
							testPoint.subtract(position);
							collisionBlockOffsets.add(testPoint);
							collisionHeightMap.put(testPoint, collisionArea.collisionHeight);
						}else if(!(world.getBlock(testPoint) instanceof BlockCollision)){
							//Some block is blocking us that's not part of a road.  Flag it.
							testPoint.subtract(position);
							collidingBlockOffsets.add(testPoint);
						}
					}
				}
			}
		}
		return collisionHeightMap;
	}
	
	/**
	 *  Method to spawn collision boxes for this multi-block structure.
	 *  Returns true and makes this TE active if all the boxes could be spawned.
	 *  False if there are blocking blocks.  OP and creative-mode players override blocking block checks.
	 */
	public boolean spawnCollisionBlocks(WrapperPlayer player){
		Map<Point3i, Integer> collisionHeightMap = generateCollisionPoints();
		if(collidingBlockOffsets.isEmpty() || (player.isCreative() && player.isOP())){
			for(Point3i offset : collisionBlockOffsets){
				Point3i testPoint = offset.copy().add(position);
				world.setBlock(BlockCollision.blockInstances.get(collisionHeightMap.get(offset)), testPoint, null, Axis.UP);
			}
			collidingBlockOffsets.clear();
			setActive(true);
			InterfacePacket.sendToAllClients(new PacketTileEntityMultiblockCollisionUpdate(this));
			return true;
		}else{
			collisionBlockOffsets.clear();
			player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.blockingblocks"));
			InterfacePacket.sendToAllClients(new PacketTileEntityMultiblockCollisionUpdate(this));
			return false;
		}
	}
	
	@Override
    public void save(WrapperNBT data){
		super.save(data);
		//Save isActive state.
		data.setBoolean("isActive", isActive);
		
		//Save cure collision point data.
		data.setPoint3is("collisionBlockOffsets", collisionBlockOffsets);
		data.setPoint3is("collidingBlockOffsets", collidingBlockOffsets);
    }
}
