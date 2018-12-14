package minecrafttransportsimulator.multipart.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.MultipartAxisAlignedBB;
import minecrafttransportsimulator.baseclasses.MultipartAxisAlignedBBCollective;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackCollisionBox;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.APartGroundDevice;
import minecrafttransportsimulator.multipart.parts.PartGroundDevicePontoon;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**Now that we have an existing multipart its time to add the ability to collide with it.
 * This is where we add collision functions and collision AABB methods to allow
 * players to collide with this part.  Note that this does NOT handle interaction as that
 * was done in level B.  Also note that we have still not defined the motions and forces
 * as those are based on the collision properties defined here and will come in level D.
 * We DO take into account the global speed variable for movement, as that's needed for
 * correct collision detection and is not state-dependent.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartC_Colliding extends EntityMultipartB_Existing{
	/**Collective wrapper that allows for the calling of multiple collision boxes in this multipart.  May be null on the first scan.*/
	private MultipartAxisAlignedBBCollective collisionFrame;
	/**List of current collision boxes in this multipart.  Contains both multipart collision boxes and ground device collision boxes.*/
	private final List<MultipartAxisAlignedBB> currentCollisionBoxes = new ArrayList<MultipartAxisAlignedBB>();
	/**List of interaction boxes.  These are AABBs that can be clicked but do NOT affect multipart collision.*/
	private final List<MultipartAxisAlignedBB> currentInteractionBoxes = new ArrayList<MultipartAxisAlignedBB>();
	/**Map that keys collision boxes of ground devices to the devices themselves.  Used for ground device collision operations.*/
	protected final Map<MultipartAxisAlignedBB, APartGroundDevice> groundDeviceCollisionBoxMap = new HashMap<MultipartAxisAlignedBB, APartGroundDevice>();
	
	public final double speedFactor = ConfigSystem.getDoubleConfig("SpeedFactor");
	
	private float hardnessHitThisTick = 0;
			
	public EntityMultipartC_Colliding(World world){
		super(world);
	}
	
	public EntityMultipartC_Colliding(World world, float posX, float posY, float posZ, float playerRotation, String multipartName){
		super(world, posX, posY, posZ, playerRotation, multipartName);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(pack != null){
			//Make sure the collision bounds for MC are big enough to collide with this entity.
			if(World.MAX_ENTITY_RADIUS < 32){
				World.MAX_ENTITY_RADIUS = 32;
			}
			
			//Populate the box lists.
			currentCollisionBoxes.clear();
			currentInteractionBoxes.clear();
			groundDeviceCollisionBoxMap.clear();
			currentCollisionBoxes.addAll(this.getUpdatedCollisionBoxes());
			hardnessHitThisTick = 0;
			for(APart part : this.getMultipartParts()){
				if(part instanceof APartGroundDevice){
					currentCollisionBoxes.add(part.getAABBWithOffset(Vec3d.ZERO));
					groundDeviceCollisionBoxMap.put(currentCollisionBoxes.get(currentCollisionBoxes.size() -1 ), (APartGroundDevice) part);
				}else{
					currentInteractionBoxes.add(part.getAABBWithOffset(Vec3d.ZERO));
				}
			}
		}
	}
	
	@Override
	public AxisAlignedBB getEntityBoundingBox(){
		//Override this to make collision checks work with the multiple collision points.
		return this.getCollisionBoundingBox();
	}
	
	@Override
    @Nullable
    public MultipartAxisAlignedBBCollective getCollisionBoundingBox(){
		//Return custom AABB for multi-collision.
		return this.collisionFrame != null ? this.collisionFrame : new MultipartAxisAlignedBBCollective(this, 1, 1);
    }
	
	/**
	 * Called by systems needing information about collision with this entity.
	 * This is a way to keep other bits from messing with the collision list
	 * and a way to get collisions without going through the collective class wrapper.
	 */
	public List<MultipartAxisAlignedBB> getCurrentCollisionBoxes(){
		return currentCollisionBoxes;
	}
	
	/**
	 * Called by the multipart AABB wrapper to allow entities to collide with
	 * interactable parts.  This allows for part collision with entities,
	 * but prevents the parts from being used in movement systems.
	 */
	public List<MultipartAxisAlignedBB> getCurrentInteractionBoxes(){
		return currentInteractionBoxes;
	}
    
	/**
	 * Called to get a set of updated collision lists for this entity.
	 * Do NOT call more than once a tick as this operation is complex and
	 * CPU and RAM intensive!
	 */
	private List<MultipartAxisAlignedBB> getUpdatedCollisionBoxes(){
		if(this.pack != null){
			double furthestWidth = 0;
			double furthestHeight = 0;
			List<MultipartAxisAlignedBB> boxList = new ArrayList<MultipartAxisAlignedBB>();
			for(PackCollisionBox box : pack.collision){
				Vec3d partOffset = new Vec3d(box.pos[0], box.pos[1], box.pos[2]);
				Vec3d offset = RotationSystem.getRotatedPoint(partOffset, rotationPitch, rotationYaw, rotationRoll);
				MultipartAxisAlignedBB newBox = new MultipartAxisAlignedBB(this.getPositionVector().add(offset), partOffset, box.width, box.height, box.isInterior);
				boxList.add(newBox);
				furthestWidth = (float) Math.max(furthestWidth, Math.abs(newBox.rel.xCoord) + box.width/2F);
				furthestHeight = (float) Math.max(furthestHeight, Math.abs(newBox.rel.yCoord) + box.height/2F);
				furthestWidth = (float) Math.max(furthestWidth, Math.abs(newBox.rel.zCoord) + box.width/2F);
			}
			this.collisionFrame = new MultipartAxisAlignedBBCollective(this, (float) furthestWidth*2F+0.5F, (float) furthestHeight*2F+0.5F);
			return boxList;
		}else{
			return new ArrayList<MultipartAxisAlignedBB>(0);
		}
	}
	
    /**
     * Checks if the passed-in entity could have clicked this multipart.
     * Result is based on rotation of the entity passed in and the current collision boxes.
     */
	public boolean wasMultipartClicked(Entity entity){
		Vec3d lookVec = entity.getLook(1.0F);
		Vec3d hitVec = entity.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
		for(float f=1.0F; f<4.0F; f += 0.1F){
			//First check the collision boxes.
			for(MultipartAxisAlignedBB box : this.currentCollisionBoxes){
				if(box.isVecInside(hitVec)){
					return true;
				}
			}
			//If we didn't hit a collision box we may have hit an interaction box instead.
			for(MultipartAxisAlignedBB box : this.currentInteractionBoxes){
				if(box.isVecInside(hitVec)){
					return true;
				}
			}
			
			hitVec = hitVec.addVector(lookVec.xCoord*0.1F, lookVec.yCoord*0.1F, lookVec.zCoord*0.1F);
		}
		return false;
	}
	
	/**
	 * Checks collisions and returns the collision depth for a box.
	 * Returns -1 and breaks the ground device if it was a ground device that collided.
	 * Returns -2 destroys the multipart if it hit a core collision box at too high a speed.
	 * Returns -3 if the collision is a ground device and could be moved upwards to not collide (only for X and Z axis).
	 */
	protected float getCollisionForAxis(MultipartAxisAlignedBB box, boolean xAxis, boolean yAxis, boolean zAxis, APartGroundDevice optionalGroundDevice){
		Vec3d motion = new Vec3d(this.motionX*speedFactor, this.motionY*speedFactor, this.motionZ*speedFactor);
		box = box.offset(xAxis ? motion.xCoord : 0, yAxis ? motion.yCoord : 0, zAxis ? motion.zCoord : 0);
		
		//Add a slight vertical offset to collisions in the X or Z axis to prevent them from catching the ground.
		//Sometimes ground devices and the like end up with a lower level of 3.9999 due to floating-point errors
		//and as such and don't collide correctly with blocks above 4.0.  Can happen at other Y values too, but that
		//one shows up extensively in superflat world testing.
		if(xAxis || zAxis){
			box = box.offset(0, 0.05F, 0);
		}
		List<BlockPos> collidedBlockPos = new ArrayList<BlockPos>();
		List<AxisAlignedBB> collidingAABBList = this.getAABBCollisions(box, optionalGroundDevice, collidedBlockPos);
		
		float collisionDepth = 0;
		for(AxisAlignedBB box2 : collidingAABBList){
			if(xAxis){
				collisionDepth = (float) Math.max(collisionDepth, motion.xCoord > 0 ? box.maxX - box2.minX : box2.maxX - box.minX);
			}
			if(yAxis){
				collisionDepth = (float) Math.max(collisionDepth, motion.yCoord > 0 ? box.maxY - box2.minY : box2.maxY - box.minY);
			}
			if(zAxis){
				collisionDepth = (float) Math.max(collisionDepth, motion.zCoord > 0 ? box.maxZ - box2.minZ : box2.maxZ - box.minZ);
			}
			if(collisionDepth > 0.3){
				//This could be a collision, but it could also be that it's a ground device and moved
				//into a block and another axis needs to collide here.  Check the motion and bail if we
				//aren't a ground device and shouldn't be colliding here.
				if((xAxis && (Math.abs(motion.xCoord) < collisionDepth)) || (yAxis && (Math.abs(motion.yCoord) < collisionDepth)) || (zAxis && (Math.abs(motion.zCoord) < collisionDepth))){
					if(optionalGroundDevice == null){
						return 0;
					}
				}
			}
		}
		
		if(collisionDepth > 0){
			if(optionalGroundDevice != null && !yAxis){
				//Ground device has collided.
				//Check to see if this collision can be avoided if the device is moved upwards.
				//Expand this box slightly to ensure we see the collision even with floating-point errors.
				collidingAABBList = getAABBCollisions(box.offset(xAxis ? motion.xCoord : 0, optionalGroundDevice.getHeight()*1.5F, zAxis ? motion.zCoord : 0).expandXyz(0.05F), optionalGroundDevice, null);
				if(collidingAABBList.isEmpty()){
					//Ground device can be moved upward out of the way.
					//Return -3 and deal with this later.
					return -3;
				}else if(collisionDepth > 0.3){
					//Ground device couldn't be moved out of the way and hit too fast;
					//Break it off the multipart and return.
					if(!worldObj.isRemote){
						this.removePart(optionalGroundDevice, true);
					}
					return -1;
				}
			}else if(optionalGroundDevice == null){
				//Not a ground device, therefore we are a core collision box and
				//need to check to see if we can break some blocks or if we need to explode.
				//Don't bother with this logic if it's impossible for us to break anything.
				double velocity = Math.hypot(motion.xCoord, motion.zCoord);
				if(velocity > 0 && !yAxis){
					byte blockPosIndex = 0;
					while(blockPosIndex < collidedBlockPos.size()){
						BlockPos pos = collidedBlockPos.get(blockPosIndex);
						float hardness = worldObj.getBlockState(pos).getBlockHardness(worldObj, pos);
						if(hardness <= velocity*currentMass/250F && hardness >= 0){
							hardnessHitThisTick += hardness;
							if(ConfigSystem.getBooleanConfig("BlockBreakage")){
								collidedBlockPos.remove(blockPosIndex);
								motionX *= Math.max(1.0F - hardness*0.5F/((1000F + currentMass)/1000F), 0.0F);
								motionY *= Math.max(1.0F - hardness*0.5F/((1000F + currentMass)/1000F), 0.0F);
								motionZ *= Math.max(1.0F - hardness*0.5F/((1000F + currentMass)/1000F), 0.0F);
								if(!worldObj.isRemote){
									worldObj.destroyBlock(pos, true);
								}
							}else{
								++blockPosIndex;
							}
						}else{
							++blockPosIndex;
						}
					}
	
					if(hardnessHitThisTick > currentMass/(0.75+velocity)/250F){
						if(!worldObj.isRemote){
							this.destroyAtPosition(box.pos.xCoord, box.pos.yCoord, box.pos.zCoord, true);
						}
						return -2;
					}else if(collidedBlockPos.isEmpty()){
						return 0;
					}
				}
				if(collisionDepth > 0.3){
					if(!worldObj.isRemote){
						this.destroyAtPosition(box.pos.xCoord, box.pos.yCoord, box.pos.zCoord, true);
					}
					return -2;	
				}
			}
		}
		return collisionDepth;
	}

	
	/**
	 * Checks if an AABB is colliding with blocks, and returns the AABB of those blocks.
	 */
	protected List<AxisAlignedBB> getAABBCollisions(AxisAlignedBB box, APartGroundDevice optionalGroundDevice, List<BlockPos> collidedBlockPos){
		int minX = (int) Math.floor(box.minX);
    	int maxX = (int) Math.floor(box.maxX + 1.0D);
    	int minY = (int) Math.floor(box.minY);
    	int maxY = (int) Math.floor(box.maxY + 1.0D);
    	int minZ = (int) Math.floor(box.minZ);
    	int maxZ = (int) Math.floor(box.maxZ + 1.0D);
    	List<AxisAlignedBB> collidingAABBList = new ArrayList<AxisAlignedBB>();
    	
    	for(int i = minX; i < maxX; ++i){
    		for(int j = minY; j < maxY; ++j){
    			for(int k = minZ; k < maxZ; ++k){
    				BlockPos pos = new BlockPos(i, j, k);
    				IBlockState state = worldObj.getBlockState(pos);
    				if(state.getBlock().canCollideCheck(state, false)){
    					state.addCollisionBoxToList(worldObj, pos, box, collidingAABBList, null);
        				if(collidedBlockPos != null){
        					collidedBlockPos.add(pos);
        				}
    				}
					if(optionalGroundDevice instanceof PartGroundDevicePontoon && state.getMaterial().isLiquid()){
						collidingAABBList.add(state.getBoundingBox(worldObj, pos).offset(pos));
					}
    			}
    		}
    	}
		return collidingAABBList;
	}
}
