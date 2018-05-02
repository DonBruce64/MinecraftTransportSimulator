package minecrafttransportsimulator.entities.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSAxisAlignedBB;
import minecrafttransportsimulator.baseclasses.MTSAxisAlignedBB.MTSAxisAlignedBBCollective;
import minecrafttransportsimulator.dataclasses.MTSDamageSources.DamageSourceCrash;
import minecrafttransportsimulator.dataclasses.PackMultipartObject;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackCollisionBox;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.multipart.parts.PartGroundDevice;
import minecrafttransportsimulator.packets.general.MultipartDeltaPacket;
import minecrafttransportsimulator.packets.general.MultipartWindowBreakPacket;
import minecrafttransportsimulator.packets.general.MultipartPartInteractionPacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**Now that we have an existing multipart its time to add the ability to collide with it.
 * This is where we add collision functions and collision AABB methods to allow
 * players to collide with this part.  Note that this does NOT handle interaction as that
 * was done in level B.  Also note that we have still not defined the motions and forces
 * as those are based on the collision properties defined here and must come later.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartC_Colliding extends EntityMultipartB_Existing{
	public List<PartGroundDevice> groundedGroundDevices = new ArrayList<PartGroundDevice>();
	
	private float width;
	private float height;
	private MTSAxisAlignedBBCollective collisionFrame;
	
	/**List of current collision boxes in this multipart.  Contains both multipart collision boxes and part collision boxes.*/
	private final List<AxisAlignedBB> currentCollisionBoxes = new ArrayList<AxisAlignedBB>();
	
	private final Map<MTSAxisAlignedBB, EntityMultipartChild> collisionMap = new HashMap<MTSAxisAlignedBB, EntityMultipartChild>();
	private final Map<MTSAxisAlignedBB, EntityMultipartChild> partMap = new HashMap<MTSAxisAlignedBB, EntityMultipartChild>();
			
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
			
			//Populate the collision lists.
			collisionMap.clear();
			partMap.clear();
			for(EntityMultipartChild child : getChildren()){
				MTSAxisAlignedBB newBox = new MTSAxisAlignedBB(child.posX, child.posY, child.posZ, child.offsetX, child.offsetY, child.offsetZ, child.getWidth(), child.getHeight()); 
				partMap.put(newBox, child);
				if(child instanceof PartGroundDevice){
					collisionMap.put(newBox, child);
				}
			}
			for(MTSAxisAlignedBB box : getUpdatedCollisionBoxes()){
				collisionMap.put(box, null);
			}
			
			currentMass = getCurrentMass();
			populateGroundedGroundDeviceList(groundedGroundDevices);
			getBasicProperties();
			getForcesAndMotions();
			performGroundOperations();
			moveMultipart();
			if(!worldObj.isRemote){
				dampenControlSurfaces();
			}
			if(this instanceof SFXEntity){
				MTS.proxy.updateSFXEntity((SFXEntity) this, worldObj);
			}
		}else if(!linked && pack != null && this.ticksExisted > 500){
			//If we don't link for over 500 ticks, assume we're bugged and just run collision boxes to let player destroy us.
			collisionMap.clear();
			for(MTSAxisAlignedBB box : getUpdatedCollisionBoxes()){
				collisionMap.put(box, null);
			}
		}
	}
	
    /**
     * Checks if the passed-in entity could have clicked this multipart given the current rotation of the entity.
     */
	public boolean wasMultipartClicked(Entity entity){
		Vec3d lookVec = entity.getLook(1.0F);
		Vec3d hitVec = entity.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
		for(float f=1.0F; f<4.0F; f += 0.1F){
			for(MTSAxisAlignedBB box : getCurrentCollisionBoxes()){
				if(box.isVecInside(hitVec)){
					return true;
				}
			}
			hitVec = hitVec.addVector(lookVec.xCoord*0.1F, lookVec.yCoord*0.1F, lookVec.zCoord*0.1F);
		}
		return false;
	}
	
	@Override
	public AxisAlignedBB getEntityBoundingBox(){
		//Override this to make collision checks work with the multiple collision points.
		return this.getCollisionBoundingBox();
	}
	
	@Override
    @Nullable
    public MTSAxisAlignedBBCollective getCollisionBoundingBox(){
		//Return custom AABB for multi-collision.
		return this.collisionFrame != null ? this.collisionFrame : new MTSAxisAlignedBBCollective(this, 1, 1);
    }
	
	/**
	 * Called by systems needing information about collision with this entity.
	 * Note that this is different than what this entity uses for collision
	 * with blocks; block collision only looks at collision bits, while
	 * attack and interaction collision looks at that and parts.
	 */
	public List<MTSAxisAlignedBB> getCurrentCollisionBoxes(){
		List<MTSAxisAlignedBB> retList = new ArrayList<MTSAxisAlignedBB>(collisionMap.keySet());
		//Remove duplicates before adding parts.
		retList.removeAll(partMap.keySet());
		retList.addAll(partMap.keySet());
		return retList;
	}
    
	/**
	 * Called to populate the collision lists for this entity.
	 * Do NOT call more than once a tick as this operation is complex and
	 * CPU and RAM intensive!
	 */
	private List<MTSAxisAlignedBB> getUpdatedCollisionBoxes(){
		if(this.pack != null){
			double furthestWidth = 0;
			double furthestHeight = 0;
			List<MTSAxisAlignedBB> boxList = new ArrayList<MTSAxisAlignedBB>();
			for(PackCollisionBox box : pack.collision){
				Vec3d offset = RotationSystem.getRotatedPoint(box.pos[0], box.pos[1], box.pos[2], rotationPitch, rotationYaw, rotationRoll);
				MTSAxisAlignedBB newBox = new MTSAxisAlignedBB((float) (this.posX + offset.xCoord), (float) (this.posY + offset.yCoord), (float) (this.posZ + offset.zCoord), box.pos[0], box.pos[1], box.pos[2], box.width, box.height);
				boxList.add(newBox);
				furthestWidth = (float) Math.max(furthestWidth, Math.abs(newBox.relX) + box.width/2F);
				furthestHeight = (float) Math.max(furthestHeight, Math.abs(newBox.relY) + box.height);
				furthestWidth = (float) Math.max(furthestWidth, Math.abs(newBox.relZ) + box.width/2F);
			}
			this.collisionFrame = new MTSAxisAlignedBBCollective(this, (float) furthestWidth*2F+0.5F, (float) furthestHeight+0.5F);
			return boxList;
		}else{
			return new ArrayList<MTSAxisAlignedBB>(0);
		}
	}
	
	/**
	 * Checks collisions and returns the collision depth for an entity.
	 * Returns -1 and breaks the optional child if it had a hard collision.
	 * Returns -2 destroys the parent if the child is a core collision box.
	 * Returns -3 if the optional child is a ground device that could be moved upwards to not collide (only for X and Z axis).
	 */
	private float getCollisionForAxis(MTSAxisAlignedBB box, boolean xAxis, boolean yAxis, boolean zAxis, EntityMultipartChild optionalChild){
		box = box.offset(xAxis ? this.motionX*speedFactor : 0, yAxis ? this.motionY*speedFactor : 0, zAxis ? this.motionZ*speedFactor : 0);
		
		//Add a slight vertical offset to collisions in the X or Z axis to prevent them from catching the ground.
		//Sometimes ground devices and the like end up with a lower level of 3.9999 due to floating-point errors
		//and as such and don't collide correctly with blocks above 4.0.  Can happen at other Y values too, but that
		//one shows up extensively in superflat world testing.
		if(xAxis || zAxis){
			box = box.offset(0, 0.05F, 0);
		}
		List<AxisAlignedBB> collidingAABBList = getAABBCollisions(box, optionalChild);
		
		float collisionDepth = 0;
		for(AxisAlignedBB box2 : collidingAABBList){
			if(xAxis){
				collisionDepth = (float) Math.max(collisionDepth, motionX > 0 ? box.maxX - box2.minX : box2.maxX - box.minX);
			}
			if(yAxis){
				collisionDepth = (float) Math.max(collisionDepth, motionY > 0 ? box.maxY - box2.minY : box2.maxY - box.minY);
			}
			if(zAxis){
				collisionDepth = (float) Math.max(collisionDepth, motionZ > 0 ? box.maxZ - box2.minZ : box2.maxZ - box.minZ);
			}
			if(collisionDepth > 0.3){
				//This could be a collision, but it could also be that the child moved into a block and another
				//axis needs to collide here.  Check the motion and bail if so.
				if((xAxis && (Math.abs(motionX) < collisionDepth)) || (yAxis && (Math.abs(motionY) < collisionDepth)) || (zAxis && (Math.abs(motionZ) < collisionDepth))){
					return 0;
				}
			}
		}
		if(optionalChild instanceof PartGroundDevice && !yAxis && collisionDepth > 0){
			//Ground device has collided.
			//Check to see if this collision can be avoided if the device is moved upwards.
			//Expand this box slightly to ensure we see the collision even with floating-point errors.
			collidingAABBList = getAABBCollisions(box.offset(xAxis ? this.motionX*speedFactor : 0, optionalChild.getHeight()*1.5F, zAxis ? this.motionZ*speedFactor : 0).expandXyz(0.05F), optionalChild);
			if(collidingAABBList.isEmpty()){
				//Ground device can be moved upward out of the way.
				//Return -3 and deal with this later.
				return -3;
			}else if(collisionDepth > 0.3){
				if(!worldObj.isRemote){
					this.removeChild(optionalChild.UUID, true);
				}
				return -1;
			}else{
				return collisionDepth;
			}
		}else if(collisionDepth > 0.3){
			if(optionalChild == null){
				if(!worldObj.isRemote){
					this.destroyAtPosition(box.posX, box.posY, box.posZ);
				}
				return -2;
			}else{
				if(!worldObj.isRemote){
					this.removeChild(optionalChild.UUID, true);
				}
				return -1;
			}
		}else{
			return collisionDepth;
		}
	}

	
	/**
	 * Checks if an AABB is colliding with blocks, and returns the AABB of those blocks.
	 * 
	 * If a soft block is encountered and this entity is going fast enough,
	 * it sets the soft block to air and slows down the entity.
	 * Used to plow though leaves and snow and the like. 
	 */
	private List<AxisAlignedBB> getAABBCollisions(AxisAlignedBB box, EntityMultipartChild optionalChild){
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
    				byte currentBoxes = (byte) collidingAABBList.size();
    				worldObj.getBlockState(pos).addCollisionBoxToList(worldObj, pos, box, collidingAABBList, null);
    				if(collidingAABBList.size() != currentBoxes){
    					float hardness = worldObj.getBlockState(pos).getBlockHardness(worldObj, pos);
    					if(hardness  <= 0.2F && hardness >= 0){
    						worldObj.setBlockToAir(pos);
    						motionX *= 0.95;
    						motionY *= 0.95;
    						motionZ *= 0.95;
    						collidingAABBList.remove(currentBoxes);
    					}
    				}else{
    					if(optionalChild != null && optionalChild.collidesWithLiquids() && worldObj.getBlockState(pos).getMaterial().isLiquid()){
    						collidingAABBList.add(worldObj.getBlockState(pos).getBoundingBox(worldObj, pos).offset(pos));
    					}
    				}
    			}
    		}
    	}
		return collidingAABBList;
	}
	
	private void populateGroundedGroundDeviceList(List<PartGroundDevice> deviceList){
		deviceList.clear();
		for(EntityMultipartChild child : getChildren()){
			if(child instanceof PartGroundDevice){
				if(!child.isDead){
					if(child.isOnGround()){
						deviceList.add((PartGroundDevice) child);
					}
				}
			}
		}
	}
}
