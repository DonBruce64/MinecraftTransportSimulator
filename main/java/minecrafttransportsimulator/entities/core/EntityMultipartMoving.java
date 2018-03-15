package minecrafttransportsimulator.entities.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSAxisAlignedBB;
import minecrafttransportsimulator.baseclasses.MTSAxisAlignedBB.MTSAxisAlignedBBCollective;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSDamageSources.DamageSourceCrash;
import minecrafttransportsimulator.dataclasses.MTSPackObject;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackCollisionBox;
import minecrafttransportsimulator.entities.main.EntityGroundDevice;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.packets.general.MultipartDeltaPacket;
import minecrafttransportsimulator.packets.general.MultipartParentDamagePacket;
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

/**General moving entity class.  This provides a set of variables and functions for moving entities.
 * Simple things like texture and display names are also included, as well as standards for removal of this
 * entity based on names and damage, and syncing methods and packet generation.
 * This is the most basic class used for custom multipart entities and should be extended
 * by any multipart looking to do movement.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartMoving extends EntityMultipartParent{
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public boolean locked;
	public byte brokenWindows;
	public float motionRoll;
	public float motionPitch;
	public float motionYaw;
	public double velocity;
	public double currentMass;
	public double damage;
	public String name="";
	public String ownerName="";
	public String displayText="";
	
	public MTSPackObject pack;
	public List<EntityGroundDevice> groundedGroundDevices = new ArrayList<EntityGroundDevice>();
	
	private double clientDeltaX;
	private double clientDeltaY;
	private double clientDeltaZ;
	private float clientDeltaYaw;
	private float clientDeltaPitch;
	private float clientDeltaRoll;
	private double serverDeltaX;
	private double serverDeltaY;
	private double serverDeltaZ;
	private float serverDeltaYaw;
	private float serverDeltaPitch;
	private float serverDeltaRoll;
	
	private float width;
	private float height;
	private MTSAxisAlignedBBCollective collisionFrame;
	private final Map<MTSAxisAlignedBB, EntityMultipartChild> collisionMap = new HashMap<MTSAxisAlignedBB, EntityMultipartChild>();
	private final Map<MTSAxisAlignedBB, EntityMultipartChild> partMap = new HashMap<MTSAxisAlignedBB, EntityMultipartChild>();
	private final double speedFactor = ConfigSystem.getDoubleConfig("SpeedFactor");
			
	public EntityMultipartMoving(World world){
		super(world);
	}
	
	public EntityMultipartMoving(World world, float posX, float posY, float posZ, float playerRotation, String name){
		super(world, posX, posY, posZ, playerRotation);
		this.name = name;
		this.pack = PackParserSystem.getPack(name); 
		//This only gets done at the beginning when the entity is first spawned.
		this.displayText = pack.general.defaultDisplayText;
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		//TOOD remove this in V11 once all the buggy vehicles are gone.
		if(pack != null && !linked){
			if(this.numberChildren > this.pack.parts.size()){
				this.numberChildren -= this.getChildren().length;
			}else if(this.numberChildren < 0){
				this.setDead();
			}
		}
		if(linked && pack != null){
			//Make sure the collision bounds for MC are big enough to collide with this entity.
			if(World.MAX_ENTITY_RADIUS < 32){
				World.MAX_ENTITY_RADIUS = 32;
			}
			//Populate the collision box map.
			collisionMap.clear();
			partMap.clear();
			for(EntityMultipartChild child : getChildren()){
				MTSAxisAlignedBB newBox = new MTSAxisAlignedBB(child.posX, child.posY, child.posZ, child.offsetX, child.offsetY, child.offsetZ, child.getWidth(), child.getHeight()); 
				partMap.put(newBox, child);
				if(child instanceof EntityGroundDevice){
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
	
	@Override
    public boolean processInitialInteract(EntityPlayer player, @Nullable ItemStack stack, EnumHand hand){
		//In all cases, interaction will be handled on the client and forwarded to the server.
		//However, there is one case where we can't forward an event, and that is if a player
		//right-clicks this with an empty hand.
		if(worldObj.isRemote && player.getHeldItemMainhand() == null){
			EntityMultipartChild hitChild = getHitChild(player);
			if(hitChild != null){
				if(hitChild.interactPart(player)){
					MTS.MTSNet.sendToServer(new MultipartPartInteractionPacket(hitChild.getEntityId(), player.getEntityId()));
				}
			}
		}
        return false;
    }
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(source.getSourceOfDamage() != null && !source.getSourceOfDamage().equals(source.getEntity())){
				//This is a projectile of some sort.
				//Get the position of the projectile and damage either the part it hit or the multipart itself.
				Entity projectile = source.getSourceOfDamage();
				for(MTSAxisAlignedBB box : partMap.keySet()){
					//Expand this box by the speed of the projectile just in case the projectile is custom and
					//attacks this entity before it actually gets inside the collision box.
					if(box.expand(Math.abs(projectile.motionX), Math.abs(projectile.motionY), Math.abs(projectile.motionZ)).isVecInside(projectile.getPositionVector())){
						//This is a box that the projectile could attack.
						//If it is a part, let the part handle the attack.
						collisionMap.get(box).attackPart(source, damage);
						return true;
					}
				}
			}else{
				//This is not a projectile, and therefore must be some sort of entity.
				//Check to see where this entity is looking and find the collision box.
				//If the box is a part, forward the attack to that part.
				Entity attacker = source.getEntity();
				if(attacker != null){
					EntityMultipartChild hitChild = getHitChild(attacker);
					if(hitChild != null){
						hitChild.attackPart(source, damage);
						return true;
					}
				}
			}
			
			//Since we didn't forward any attacks or do special events, we must have attacked this multipart directly.
			Entity damageSource = source.getEntity() != null && !source.getEntity().equals(source.getSourceOfDamage()) ? source.getSourceOfDamage() : source.getEntity();
			if(damageSource != null){
				this.damage += damage;
				if(this.brokenWindows < pack.general.numberWindows){
					++brokenWindows;
					this.playSound(SoundEvents.BLOCK_GLASS_BREAK, 2.0F, 1.0F);
					MTS.MTSNet.sendToAll(new MultipartParentDamagePacket(this.getEntityId(), damage, true));
				}else{
					MTS.MTSNet.sendToAll(new MultipartParentDamagePacket(this.getEntityId(), damage, false));
				}
				
			}
		}
		return true;
	}
	
	public EntityMultipartChild getHitChild(Entity entity){
		Vec3d lookVec = entity.getLook(1.0F);
		Vec3d hitVec = entity.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
		for(float f=1.0F; f<4.0F; f += 0.1F){
			for(MTSAxisAlignedBB box : partMap.keySet()){
				if(box.isVecInside(hitVec)){
					return partMap.get(box);
				}
			}
			hitVec = hitVec.addVector(lookVec.xCoord*0.1F, lookVec.yCoord*0.1F, lookVec.zCoord*0.1F);
		}
		return null;
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
	public void setDead(){
		if(!worldObj.isRemote){
			for(EntityMultipartChild child : this.getChildren()){
				ItemStack stack = child.getItemStack();
				if(stack != null){
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, stack));
				}
			}
		}
		super.setDead();
	}
	
    //Need to render in pass 1 to render transparent things like light beams.
    @Override
    public boolean shouldRenderInPass(int pass){
    	return true;
    }
    
	@Override
	public boolean canBeCollidedWith(){
		//This gets overridden to do collisions with players.
		return true;
	}
	
	@Override
	public AxisAlignedBB getEntityBoundingBox(){
		//Override this to make collision checks work.
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
		List<MTSAxisAlignedBB> retList = new ArrayList(collisionMap.keySet());
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
				MTSVector offset = RotationSystem.getRotatedPoint(box.pos[0], box.pos[1], box.pos[2], rotationPitch, rotationYaw, rotationRoll);
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
	 * Call this when moving multiparts to ensure they move correctly.
	 * Failure to do this will result in things going badly!
	 */
	private void moveMultipart(){
		//First check planned movement.
		boolean needCheck = false;
		boolean groundDeviceNeedsLifting = false;
		double originalMotionY = motionY;
		
		//First try to add the current motion and see if we need to check anything.
		for(MTSAxisAlignedBB box : collisionMap.keySet()){
			MTSVector offset = RotationSystem.getRotatedPoint(box.relX, box.relY, box.relZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
			MTSAxisAlignedBB offsetBox = box.getBoxWithOrigin(posX + offset.xCoord + motionX*speedFactor, posY + offset.yCoord + motionY*speedFactor, posZ + offset.zCoord + motionZ*speedFactor);
			if(!getAABBCollisions(offsetBox, collisionMap.get(box)).isEmpty()){
				needCheck = true;
			}
		}
	
		//If any child was collided with something we need to adjust movement here.
		//Otherwise we can just let the multipart move like normal.
		if(needCheck){
			//The first thing we need to do is see the depth of the collision in the XZ plane.
			//If minor, we can stop movement or move up (if a ground device is collided).

			//First check the X-axis.
			for(MTSAxisAlignedBB box : collisionMap.keySet()){
				float collisionDepth = getCollisionForAxis(box, true, false, false, collisionMap.get(box));
				if(collisionDepth < 0){
					if(collisionDepth != -2){
						if(collisionDepth == -3){
							groundDeviceNeedsLifting = true;
						}
						continue;
					}else{
						return;
					}
				}else{
					if(this.motionX > 0){
						this.motionX = Math.max(motionX - collisionDepth/speedFactor, 0);
					}else if(this.motionX < 0){
						this.motionX = Math.min(motionX + collisionDepth/speedFactor, 0);
					}
				}
			}
			
			//Do the same for the Z-axis
			for(MTSAxisAlignedBB box : collisionMap.keySet()){
				float collisionDepth = getCollisionForAxis(box, false, false, true, collisionMap.get(box));
				if(collisionDepth < 0){
					if(collisionDepth != -2){
						if(collisionDepth == -3){
							groundDeviceNeedsLifting = true;
						}
						continue;
					}else{
						return;
					}
				}else{
					if(this.motionZ > 0){
						this.motionZ = Math.max(motionZ - collisionDepth/speedFactor, 0);
					}else if(this.motionZ < 0){
						this.motionZ = Math.min(motionZ + collisionDepth/speedFactor, 0);
					}
				}
			}
			
			//Now that the XZ motion has been limited based on collision we can move in the Y.
			for(MTSAxisAlignedBB box : collisionMap.keySet()){
				float collisionDepth = getCollisionForAxis(box, false, true, false, collisionMap.get(box));
				if(collisionDepth < 0){
					if(collisionDepth != -2){
						continue;
					}else{
						return;
					}
				}else if(collisionDepth != 0){
					if(this.motionY > 0){
						this.motionY = Math.max(motionY - collisionDepth/speedFactor, 0);
					}else if(this.motionY < 0){
						this.motionY = Math.min(motionY + collisionDepth/speedFactor, 0);
					}
				}
			}			
			
			//Check the yaw.
			for(MTSAxisAlignedBB box : collisionMap.keySet()){
				while(motionYaw != 0){
					MTSVector offset = RotationSystem.getRotatedPoint(box.relX, box.relY, box.relZ, rotationPitch, rotationYaw + motionYaw, rotationRoll);
					//Raise this box ever so slightly because Floating Point errors are a PITA.
					MTSAxisAlignedBB offsetBox = box.getBoxWithOrigin(posX + offset.xCoord + motionX*speedFactor, posY + offset.yCoord + motionY*speedFactor + 0.1, posZ + offset.zCoord + motionZ*speedFactor);
					if(getAABBCollisions(offsetBox, collisionMap.get(box)).isEmpty()){
						break;
					}
					if(this.motionYaw > 0){
						this.motionYaw = Math.max(motionYaw - 0.1F, 0);
					}else{
						this.motionYaw = Math.min(motionYaw + 0.1F, 0);
					}
				}
			}
			
			//Now do pitch.
			//Make sure to take into account yaw as it's already been checked.
			//Note that pitch is special in that it can add a slight Y to multiparts if the multipart is
			//trying to pitch up and rear ground devices are blocking it.  This needed to allow vehicles to
			//rotate on their ground devices.
			for(MTSAxisAlignedBB box : collisionMap.keySet()){
				while(motionPitch != 0){
					MTSVector offset = RotationSystem.getRotatedPoint(box.relX, box.relY, box.relZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll);
					MTSAxisAlignedBB offsetBox = box.getBoxWithOrigin(posX + offset.xCoord + motionX*speedFactor, posY + offset.yCoord + motionY*speedFactor, posZ + offset.zCoord + motionZ*speedFactor);
					if(getAABBCollisions(offsetBox, collisionMap.get(box)).isEmpty()){
						break;
					}else if(motionPitch < 0){
						if(box.posZ <= 0 && collisionMap.get(box) instanceof EntityGroundDevice){
							float yBoost = 0;
							for(AxisAlignedBB box2 : getAABBCollisions(offsetBox, collisionMap.get(box))){
								if(box.maxY > offsetBox.minY + yBoost){
									yBoost += (box.maxY - offsetBox.minY);
								}
							}
							//Clamp the boost relative to the speed of the vehicle.
							//Otherwise things get bouncy.
							yBoost = (float) Math.min(Math.min(this.velocity, -motionPitch), yBoost/speedFactor);
							motionY += yBoost;
							originalMotionY += yBoost;
							break;
						}
					}
					if(this.motionPitch > 0){
						this.motionPitch = Math.max(motionPitch - 0.1F, 0);
					}else{
						this.motionPitch = Math.min(motionPitch + 0.1F, 0);
					}
				}
			}
			
			//And lastly the roll.
			for(MTSAxisAlignedBB box : collisionMap.keySet()){
				while(motionRoll != 0){
					MTSVector offset = RotationSystem.getRotatedPoint(box.relX, box.relY, box.relZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
					MTSAxisAlignedBB offsetBox = box.getBoxWithOrigin(posX + offset.xCoord + motionX*speedFactor, posY + offset.yCoord + motionY*speedFactor, posZ + offset.zCoord + motionZ*speedFactor);
					if(getAABBCollisions(offsetBox, collisionMap.get(box)).isEmpty()){
						break;
					}
					if(this.motionRoll > 0){
						this.motionRoll = Math.max(motionRoll - 0.1F, 0);
					}else{
						this.motionRoll = Math.min(motionRoll + 0.1F, 0);
					}
				}
			}
			
			//Now everything has been checked and clamped.  If we had a colliding ground device
			//during the XZ movement that could move up to get out of the way increase the Y
			//of this multipart to do so.
			if(groundDeviceNeedsLifting && motionY <= 0.25F){
				motionY = 0.25F;
			}
			
			if(originalMotionY != motionY && originalMotionY < 0){
				//Even if we didn't collide any ground devices, we still may need to adjust pitch/roll.
				//In this case it's determined by what is on the ground and the 
				//original motionY.  If we are supposed to go down by gravity, and can't because the
				//multipart is crooked, we need to fix this.
				
				//To do this we check which boxes are colliding in which motion groups and add pitch/roll.
				//Do this until we are able to do the entire motionY or until opposite groups are stable.
				//Boxes with an offset of 0 in X or Z are ignored when calculating roll and pitch respectively.
				boolean needPitchUp = false;
				boolean needPitchDown = false;
				boolean needRollRight = false;
				boolean needRollLeft = false;
				boolean needPitchUpAnytime = false;
				boolean needPitchDownAnytime = false;
				boolean needRollRightAnytime = false;
				boolean needRollLeftAnytime = false;
				boolean continueLoop = true;
				int loopCount = 0;
				float modifiedPitch = this.motionPitch;
				float modifiedRoll = this.motionRoll;
				
				do{
					needPitchUp = false;
					needPitchDown = false;
					needRollRight = false;
					needRollLeft = false;
					for(MTSAxisAlignedBB box : collisionMap.keySet()){
						MTSVector offset = RotationSystem.getRotatedPoint(box.relX, box.relY, box.relZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
						MTSAxisAlignedBB offsetBox = box.getBoxWithOrigin(posX + offset.xCoord + motionX*speedFactor, posY + offset.yCoord + motionY*speedFactor, posZ + offset.zCoord + motionZ*speedFactor);
						if(!getAABBCollisions(offsetBox, collisionMap.get(box)).isEmpty()){
							if(box.relZ > 0){
								needPitchUp = true;
								needPitchUpAnytime = true;
							}
							if(box.relZ <= 0 && motionPitch >= 0){
								needPitchDown = true;
								needPitchDownAnytime = true;
							}
							if(box.relX > 0){
								needRollRight = true;
								needRollRightAnytime = true;
							}
							if(box.relX < 0){
								needRollLeft = true;
								needRollLeftAnytime = true;
							}
						}
					}
					
					//At this point a combinations of pitch and roll may be needed.
					//If so, apply either or both.  If not, add back some Y.
					//Do NOT add pitch or roll if we were subtracting it before (or vice-versa).
					//That's stupid and leads to infinite loops.
					if(needPitchUp && !needPitchDownAnytime){
						this.motionPitch -= 0.1F;
					}
					if(needPitchDown && !needPitchUpAnytime){
						this.motionPitch += 0.1F;
					}
					if(needRollLeft && !needRollRightAnytime){
						this.motionRoll -= 0.1F;
					}
					if(needRollRight && !needRollLeftAnytime){
						this.motionRoll += 0.1F;
					}
					
					//If nothing is colliding see about adding some Y back.
					if(!needPitchUp && !needPitchDown && !needRollRight && !needRollLeft){
						motionY = Math.max(originalMotionY, motionY - 0.1F);
					}
					
					//Exit this loop when:
					//Nothing is colliding (and motionY is equal to originalMotionY if not grounded) or
					//the pitch axis is colliding on both ends and the roll axis is not colliding (on the ground with no wheels) or
					//the roll axis is colliding on both ends and the pitch is not colliding (hanging on wheels) or
					//both roll and pitch have gone as far as they can go (system is in balance).
					if(!needPitchUp && !needPitchDown && !needRollRight && !needRollLeft && (groundDeviceNeedsLifting ? true : motionY == originalMotionY)){
						continueLoop = false;
					}else if(needPitchUp && needPitchDown && !needRollRight && !needRollLeft){
						continueLoop = false;
					}else if(needRollRight && needRollLeft && !needPitchUp && !needPitchDown){
						continueLoop = false;
					}else if(needPitchUpAnytime && needPitchDownAnytime && needRollRightAnytime && needRollLeftAnytime){
						continueLoop = false;
						if(loopCount == 1){
							loopCount = 0;
						}
					}
					if(loopCount == 0 && !continueLoop){
						motionPitch = modifiedPitch;
						motionRoll = modifiedRoll;
					}
					++loopCount;
				}while(continueLoop && loopCount < 20);
			}
		}
		
		//Now that that the movement has been checked, move the multipart.
		prevRotationRoll = rotationRoll;
		if(!worldObj.isRemote){
			rotationYaw += motionYaw;
			rotationPitch += motionPitch;
			rotationRoll += motionRoll;
			setPosition(posX + motionX*speedFactor, posY + motionY*speedFactor, posZ + motionZ*speedFactor);
			addToServerDeltas(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor, motionYaw, motionPitch, motionRoll);
			MTS.MTSNet.sendToAll(new MultipartDeltaPacket(getEntityId(), motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor, motionYaw, motionPitch, motionRoll));
		}else{
			//Make sure the server is sending delta packets and NBT is initialized before we try to do delta correction.
			if(!(serverDeltaX == 0 && serverDeltaY == 0 && serverDeltaZ == 0)){
				double deltaX = motionX*speedFactor + (serverDeltaX - clientDeltaX)/4F;
				double deltaY = motionY*speedFactor + (serverDeltaY - clientDeltaY)/4F;
				double deltaZ = motionZ*speedFactor + (serverDeltaZ - clientDeltaZ)/4F;
				float deltaYaw = motionYaw + (serverDeltaYaw - clientDeltaYaw)/4F;
				float deltaPitch = motionPitch + (serverDeltaPitch - clientDeltaPitch)/4F;
				float deltaRoll = motionRoll + (serverDeltaRoll - clientDeltaRoll)/4F;
				
				setPosition(posX + deltaX, posY + deltaY, posZ + deltaZ);
				rotationYaw += deltaYaw;
				rotationPitch += deltaPitch;
				rotationRoll += deltaRoll;
				addToClientDeltas(deltaX, deltaY, deltaZ, deltaYaw, deltaPitch, deltaRoll);
			}else{
				rotationYaw += motionYaw;
				rotationPitch += motionPitch;
				rotationRoll += motionRoll;
				setPosition(posX + motionX*speedFactor, posY + motionY*speedFactor, posZ + motionZ*speedFactor);
			}
		}
		
		for(EntityMultipartChild child : getChildren()){
			if(child.isDead){
				removeChild(child.UUID, false);
			}else{
				MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw, rotationRoll);
				child.setPosition(posX + offset.xCoord, posY + offset.yCoord, posZ + offset.zCoord);
			}
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
		if(optionalChild instanceof EntityGroundDevice && !yAxis && collisionDepth > 0){
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
	
	/**
	 * Essentially the way to kill this multipart.  Explosions may not occur 
	 * depending on config settings or a lack of fuel or explodable cargo.
	 */
	protected void destroyAtPosition(double x, double y, double z){
		Entity controller = null;
		for(EntityMultipartChild child : getChildren()){
			if(child instanceof EntitySeat){
				EntitySeat seat = (EntitySeat) child;
				Entity rider = seat.getPassenger();
				if(seat.isController && controller != null){
					controller = rider;
				}
				
				if(rider != null){
					if(rider.equals(controller)){
						rider.attackEntityFrom(new DamageSourceCrash(null, this.pack.general.type), (float) (ConfigSystem.getDoubleConfig("CrashDamageFactor")*velocity*20));
					}else{
						rider.attackEntityFrom(new DamageSourceCrash(controller, this.pack.general.type), (float) (ConfigSystem.getDoubleConfig("CrashDamageFactor")*velocity*20));
					}
				}
			}
		}
		this.setDead();
	}
	
	private void populateGroundedGroundDeviceList(List<EntityGroundDevice> deviceList){
		deviceList.clear();
		for(EntityMultipartChild child : getChildren()){
			if(child instanceof EntityGroundDevice){
				if(!child.isDead){
					if(child.isOnGround()){
						deviceList.add((EntityGroundDevice) child);
					}
				}
			}
		}
	}
	
	/**
	 * Returns factor for braking.
	 * Depends on number of grounded cores and braking ground devices.
	 */
	protected float getBrakingForceFactor(){
		float brakingFactor = 0;
		for(MTSAxisAlignedBB box : collisionMap.keySet()){
			float addedFactor = 0;
			if(collisionMap.get(box) instanceof EntityGroundDevice){
				if(brakeOn || parkingBrakeOn){
					if(collisionMap.get(box).isOnGround()){
						addedFactor = ((EntityGroundDevice) collisionMap.get(box)).motiveFriction;
					}
				}
			}else{
				if(!worldObj.getCollisionBoxes(box.offset(this.posX, this.posY - 0.05F, this.posZ)).isEmpty()){
					addedFactor = 1;
				}
			}
			
			if(addedFactor != 0){
				//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything extra should increase it.
				BlockPos pos = new BlockPos(box.posX, box.posY - 1, box.posZ);
				float frictionLoss = 0.6F - worldObj.getBlockState(pos).getBlock().slipperiness;
				brakingFactor += Math.max(addedFactor - frictionLoss, 0);
			}
		}
		return brakingFactor;
	}
	
	/**
	 * Returns factor for skidding based on lateral friction and velocity.
	 * If the value is non-zero, it indicates that yaw should be restricted
	 * due to ground devices being in contact with the ground.
	 * Note that this should be called prior to turning code as it will interpret
	 * the yaw change as a skid and will attempt to prevent it!
	 */
	protected float getSkiddingFactor(){
		float skiddingFactor = 0;
		for(EntityGroundDevice grounder : groundedGroundDevices){
			//0.6 is default slipperiness for blocks.  Anything less should reduce friction, anything extra should increase it.
			BlockPos pos = grounder.getPosition().down();
			float frictionLoss = 0.6F - grounder.worldObj.getBlockState(pos).getBlock().slipperiness;
			//Do we have enough friction to prevent skidding?
			if(grounder.lateralFriction - frictionLoss > 0){
				skiddingFactor += grounder.lateralFriction - frictionLoss;
			}
		}
		return skiddingFactor;
	}
	
	/**
	 * Returns factor for turning based on lateral friction and velocity.
	 * Sign of returned value indicates which direction entity should yaw.
	 * A 0 value indicates no yaw change.
	 */
	protected float getTurningFactor(){		
		float turningForce = 0;
		float steeringAngle = this.getSteerAngle();
		if(steeringAngle != 0){
			float turningFactor = 0;
			for(EntityGroundDevice grounder : groundedGroundDevices){
				//0.6 is default slipperiness for blocks.  Anything less should reduce friction, anything extra should increase it.
				BlockPos pos = grounder.getPosition().down();
				float frictionLoss = 0.6F - grounder.worldObj.getBlockState(pos).getBlock().slipperiness;
				//Do we have enough friction to change yaw?
				if(grounder.shouldAffectSteering() && grounder.lateralFriction - frictionLoss > 0){
					turningFactor += grounder.lateralFriction - frictionLoss;
				}
			}
			if(turningFactor > 0){
				//Now that we know we can turn, we can attempt to change the track.
				steeringAngle = Math.abs(steeringAngle);
				if(turningFactor < 1){
					steeringAngle *= turningFactor;
				}
				//Another thing that can affect the steering angle is speed.
				//More speed makes for less wheel turn to prevent crazy circles.
				steeringAngle *= Math.pow(0.25F, velocity);
				//Adjust turn force to steer angle based on turning factor.
				turningForce = -(float) (steeringAngle*velocity/2F);
				//Now add the sign to this force.
				turningForce *= Math.signum(this.getSteerAngle());
			}
		}
		return turningForce;
	}
	
	protected float getCurrentMass(){
		int currentMass = pack.general.emptyMass;
		for(EntityMultipartChild child : getChildren()){
			Entity rider = child.getRidingEntity();
			if(rider != null){
				if(rider instanceof EntityPlayer){
					currentMass += 100 + calculateInventoryWeight(((EntityPlayer) rider).inventory);
				}else{
					currentMass += 100;
				}
			}else if(child instanceof IInventory){
				currentMass += calculateInventoryWeight((IInventory) child);
			}else{
				currentMass += 50;
			}
		}
		return currentMass;
	}
	
	/**
	 * Calculates the weight of the inventory passed in.  Used for physics calculations.
	 * @param inventory
	 */
	private static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				weight += 1.2F*stack.stackSize/stack.getMaxStackSize()*(ConfigSystem.getStringConfig("HeavyItems").contains(stack.getItem().getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
	
	private void addToClientDeltas(double dX, double dY, double dZ, float dYaw, float dPitch, float dRoll){
		this.clientDeltaX += dX;
		this.clientDeltaY += dY;
		this.clientDeltaZ += dZ;
		this.clientDeltaYaw += dYaw;
		this.clientDeltaPitch += dPitch;
		this.clientDeltaRoll += dRoll;
	}
	
	public void addToServerDeltas(double dX, double dY, double dZ, float dYaw, float dPitch, float dRoll){
		this.serverDeltaX += dX;
		this.serverDeltaY += dY;
		this.serverDeltaZ += dZ;
		this.serverDeltaYaw += dYaw;
		this.serverDeltaPitch += dPitch;
		this.serverDeltaRoll += dRoll;
	}
	
	/**
	 * Method block for basic properties like weight and vectors.
	 */
	protected abstract void getBasicProperties();
	
	/**
	 * Method block for force and motion calculations.
	 */
	protected abstract void getForcesAndMotions();
	
	/**
	 * Method block for ground operations.
	 * Must come AFTER force calculations as it depends on motions.
	 */
	protected abstract void performGroundOperations();
	
	/**
	 * Method block for dampening control surfaces.
	 * Used to move control surfaces back to neutral position.
	 */
	protected abstract void dampenControlSurfaces();
	
	/**
	 * Returns whatever the steering angle is.  Used for rendering and possibly other things.
	 */
	public abstract float getSteerAngle();
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.locked=tagCompound.getBoolean("locked");
		this.brokenWindows=tagCompound.getByte("brokenWindows");
		this.name=tagCompound.getString("name");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayText=tagCompound.getString("displayText");
		this.pack=PackParserSystem.getPack(name);
		
		this.serverDeltaX=tagCompound.getDouble("serverDeltaX");
		this.serverDeltaY=tagCompound.getDouble("serverDeltaY");
		this.serverDeltaZ=tagCompound.getDouble("serverDeltaZ");
		this.serverDeltaYaw=tagCompound.getFloat("serverDeltaYaw");
		this.serverDeltaPitch=tagCompound.getFloat("serverDeltaPitch");
		this.serverDeltaRoll=tagCompound.getFloat("serverDeltaRoll");
		
		if(worldObj.isRemote){
			this.clientDeltaX = this.serverDeltaX;
			this.clientDeltaY = this.serverDeltaY;
			this.clientDeltaZ = this.serverDeltaZ;
			this.clientDeltaYaw = this.serverDeltaYaw;
			this.clientDeltaPitch = this.serverDeltaPitch;
			this.clientDeltaRoll = this.serverDeltaRoll;
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("brakeOn", this.brakeOn);
		tagCompound.setBoolean("parkingBrakeOn", this.parkingBrakeOn);
		tagCompound.setBoolean("locked", this.locked);
		tagCompound.setByte("brokenWindows", this.brokenWindows);
		tagCompound.setString("name", this.name);
		tagCompound.setString("ownerName", this.ownerName);
		tagCompound.setString("displayText", this.displayText);
		
		tagCompound.setDouble("serverDeltaX", this.serverDeltaX);
		tagCompound.setDouble("serverDeltaY", this.serverDeltaY);
		tagCompound.setDouble("serverDeltaZ", this.serverDeltaZ);
		tagCompound.setFloat("serverDeltaYaw", this.serverDeltaYaw);
		tagCompound.setFloat("serverDeltaPitch", this.serverDeltaPitch);
		tagCompound.setFloat("serverDeltaRoll", this.serverDeltaRoll);
		return tagCompound;
	}
}
