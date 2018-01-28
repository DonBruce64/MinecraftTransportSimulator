package minecrafttransportsimulator.entities.core;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSAchievements;
import minecrafttransportsimulator.dataclasses.MTSDamageSources.DamageSourceCrash;
import minecrafttransportsimulator.dataclasses.MTSPackObject;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackCollisionBox;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackPart;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackWindow;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.entities.main.EntityGroundDevice;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.items.ItemKey;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.packets.general.MultipartDeltaPacket;
import minecrafttransportsimulator.packets.general.MultipartParentDamagePacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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
	public List<Byte> brokenWindows = new ArrayList<Byte>();
	public List<EntityGroundDevice> groundedGroundDevices = new ArrayList<EntityGroundDevice>();
	
	private boolean gotDeltaPacket;
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
		if(linked){
			currentMass = getCurrentMass();
			populateGroundedGroundDeviceList(groundedGroundDevices);
		}
	}
	
	@Override
	public boolean processInitialInteractFromChild(EntityPlayer player, EntityMultipartChild childClicked, @Nullable ItemStack stack){
		if(player.getRidingEntity() instanceof EntitySeat){
			if(this.equals(((EntitySeat) player.getRidingEntity()).parent)){
				//No in-use changes for sneaky sneaks!
				//Unless we're using a key to lock ourselves in.
				if(stack != null){
					if(!stack.getItem().equals(MTSRegistry.key)){
						return false;
					}
				}
			}
		}
		if(!worldObj.isRemote){
			if(stack != null){
				if(stack.getItem().equals(Items.NAME_TAG)){
					int maxText = pack.general.displayTextMaxLength;
					this.displayText = stack.getDisplayName().length() > maxText ? stack.getDisplayName().substring(0, maxText - 1) : stack.getDisplayName();
					this.sendDataToClient();
					return true;
				}else if(stack.getItem().equals(MTSRegistry.wrench)){
					return true;
				}else if(stack.getItem().equals(MTSRegistry.key)){
					if(player.isSneaking()){
						if(this.ownerName.isEmpty()){
							this.ownerName = player.getUUID(player.getGameProfile()).toString();
							MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.own"), (EntityPlayerMP) player);
						}else{
							boolean isPlayerOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
							if(player.getUUID(player.getGameProfile()).toString().equals(this.ownerName) || isPlayerOP){
								this.ownerName = "";
								MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.unown"), (EntityPlayerMP) player);	
							}else{
								MTS.MTSNet.sendTo(new ChatPacket(worldObj.getPlayerEntityByUUID(player.getPersistentID().fromString(ownerName)).getDisplayNameString() + " " + "interact.key.failure.alreadyowned"), (EntityPlayerMP) player);
								return true;
							}
						}
					}else{
						if(ItemKey.getVehicleUUID(stack).isEmpty()){
							if(!this.ownerName.isEmpty()){
								if(!player.getUUID(player.getGameProfile()).toString().equals(this.ownerName)){
									MTS.MTSNet.sendTo(new ChatPacket("interact.key.failure.notowner"), (EntityPlayerMP) player);
									return true;
								}
							}
							ItemKey.setVehicle(stack, this);
							this.locked = true;
							MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.lock"), (EntityPlayerMP) player);
							player.addStat(MTSAchievements.key);
						}else if(!ItemKey.getVehicleUUID(player.getHeldItemMainhand()).equals(this.UUID)){
							MTS.MTSNet.sendTo(new ChatPacket("interact.key.failure.wrongkey"), (EntityPlayerMP) player);
							return true;
						}else{
							if(locked){
								this.locked = false;
								MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.unlock"), (EntityPlayerMP) player);
							}else{
								this.locked = true;
								MTS.MTSNet.sendTo(new ChatPacket("interact.key.info.lock"), (EntityPlayerMP) player);
							}
						}
					}
					this.sendDataToClient();
					return true;
				}else{
					boolean isItemPart = false;
					Item heldItem = stack.getItem();
					PackPart partToSpawn = null;
					float closestPosition = 9999;
					//Look though the part data to find the class that goes with the held item.
					for(PackPart part : pack.parts){
						for(String partName : part.names){
							if(heldItem.getRegistryName().getResourcePath().equals(partName)){
								isItemPart = true;
								//The held item can spawn a part.
								//Now find the closest spot to put it.
								float distance = (float) Math.hypot(childClicked.offsetX - part.pos[0], childClicked.offsetZ - part.pos[2]);
								if(distance < closestPosition){
									//Make sure a part doesn't exist already.
									boolean childPresent = false;
									for(EntityMultipartChild child : this.getChildren()){
										if(child.offsetX == part.pos[0] && child.offsetY == part.pos[1] && child.offsetZ == part.pos[2]){
											childPresent = true;
											break;
										}
									}
									if(!childPresent){
										closestPosition = distance;
										partToSpawn = part;
									}
								}
							}
						}
					}
					
					if(partToSpawn != null){
						//We have a part, now time to spawn it.
						try{
							Constructor<? extends EntityMultipartChild> construct = MTSRegistry.partClasses.get(heldItem.getRegistryName().getResourcePath()).getConstructor(World.class, EntityMultipartParent.class, String.class, float.class, float.class, float.class, int.class);
							EntityMultipartChild newChild = construct.newInstance(worldObj, this, this.UUID, partToSpawn.pos[0], partToSpawn.pos[1], partToSpawn.pos[2], stack.getItemDamage());
							newChild.setNBTFromStack(stack);
							newChild.setTurnsWithSteer(partToSpawn.turnsWithSteer);
							newChild.setController(partToSpawn.isController);
							this.addChild(newChild.UUID, newChild, true);
							if(!player.capabilities.isCreativeMode){
								if(stack.stackSize > 0){
									--stack.stackSize;
								}else{
									player.inventory.removeStackFromSlot(player.inventory.currentItem);
								}
							}
						}catch(Exception e){
							MTS.MTSLog.error("ERROR SPAWING PART!");
							e.printStackTrace();
						}
					}
					if(isItemPart){
						return true;
					}
				}
			}
		}else if(player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem().equals(MTSRegistry.wrench)){
			MTS.proxy.openGUI(this, player);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				EntityPlayer attackingPlayer = (EntityPlayer) source.getEntity();
				if(attackingPlayer.isSneaking() && attackingPlayer.getHeldItemMainhand() != null && attackingPlayer.getHeldItemMainhand().getItem().equals(MTSRegistry.wrench)){
					boolean isPlayerOP = attackingPlayer.getServer().getPlayerList().getOppedPlayers().getEntry(attackingPlayer.getGameProfile()) != null || attackingPlayer.getServer().isSinglePlayer();
					if(this.ownerName.isEmpty() || attackingPlayer.getUUID(attackingPlayer.getGameProfile()).toString().equals(this.ownerName) || isPlayerOP){
						this.setDead();
					}else{
						MTS.MTSNet.sendTo(new ChatPacket("interact.failure.vehicleowned"), (EntityPlayerMP) attackingPlayer);
					}
					return true;
				}
			}
			if(source.getEntity() != null && !this.equals(source.getEntity())){
				this.damage += damage;
				Map<Byte, Float> windowDistances = new HashMap<Byte, Float>();
				MTSVector relativeAttackerPosition = new MTSVector(source.getEntity().posX, source.getEntity().posY, source.getEntity().posZ).add(-this.posX, -this.posY, -this.posZ);
				
				for(byte i=0; i<pack.rendering.windows.size(); ++i){
					PackWindow window = pack.rendering.windows.get(i);
					float averageX;
					float averageY;
					float averageZ;
					if(window.pos4 != null){
						averageX = (window.pos1[0] + window.pos2[0] + window.pos3[0] + window.pos4[0])/4F;
						averageY = (window.pos1[1] + window.pos2[1] + window.pos3[1] + window.pos4[1])/4F;
						averageZ = (window.pos1[2] + window.pos2[2] + window.pos3[2] + window.pos4[2])/4F;
					}else{
						averageX = (window.pos1[0] + window.pos2[0] + window.pos3[0])/3F;
						averageY = (window.pos1[1] + window.pos2[1] + window.pos3[1])/3F;
						averageZ = (window.pos1[2] + window.pos2[2] + window.pos3[2])/3F;
					}
					MTSVector windowPosition = RotationSystem.getRotatedPoint(averageX, averageY, averageZ, rotationPitch, rotationYaw, rotationRoll);
					if(!this.brokenWindows.contains(i)){
						windowDistances.put(i, (float) relativeAttackerPosition.distanceTo(windowPosition));
					}
				}
				
				byte closestWindowIndex = -1;
				float shortestDistance = Float.MAX_VALUE;
				for(Entry<Byte, Float> windowEntry : windowDistances.entrySet()){
					if(windowEntry.getValue() < shortestDistance){
						closestWindowIndex = windowEntry.getKey();
						shortestDistance = windowEntry.getValue();
					}
				}
				if(shortestDistance <= 3){
					this.brokenWindows.add(closestWindowIndex);
					this.playSound(SoundEvents.BLOCK_GLASS_BREAK, 2.0F, 1.0F);
				}
				MTS.MTSNet.sendToAll(new MultipartParentDamagePacket(this.getEntityId(), damage, closestWindowIndex));
			}
		}
		return true;
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
    
	public List<Float[]> getCollisionBoxes(){
		List<Float[]> boxList = new ArrayList<Float[]>();
		for(PackCollisionBox box : pack.collision){
			boxList.add(new Float[]{box.pos[0], box.pos[1], box.pos[2], box.width, box.height});
		}
		return boxList;
	}
	
	/**
	 * Checks to see if this multipart can move.  If so, nothing is changed.
	 * If not, motions are adjusted to prevent collisions.  If velocity motions
	 * cause a collision that's larger than 0.3 blocks the part is removed,
	 * or if a core was collided the multipart is destroyed (by an explosion if set).
	 */
	protected void checkPlannedMovement(){
		boolean needCheck = false;
		boolean groundDeviceNeedsLifting = false;
		double originalMotionY = motionY;
		EntityMultipartChild[] childArray = getChildren();

		for(EntityMultipartChild child : childArray){
			MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw, rotationRoll);
			AxisAlignedBB offsetChildBox = child.getEntityBoundingBox().offset(posX + offset.xCoord - child.posX + motionX*speedFactor, posY + offset.yCoord - child.posY + motionY*speedFactor, posZ + offset.zCoord - child.posZ + motionZ*speedFactor);
			if(!getChildCollisions(child, offsetChildBox).isEmpty()){
				needCheck = true;
			}
		}
	
		//If any child was collided with something we need to adjust movement here.
		//Otherwise we can just let the multipart move like normal.
		if(needCheck){
			//The first thing we need to do is see the depth of the collision in the XZ plane.
			//If minor, we can stop movement or move child up (if a ground device).

			//First check the X-axis.
			for(EntityMultipartChild child : childArray){
				float collisionDepth = getCollisionForAxis(child, true, false, false);
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
			for(EntityMultipartChild child : childArray){
				float collisionDepth = getCollisionForAxis(child, false, false, true);
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
			for(EntityMultipartChild child : childArray){
				float collisionDepth = getCollisionForAxis(child, false, true, false);
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
			for(EntityMultipartChild child : childArray){
				while(motionYaw != 0){
					MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw + motionYaw, rotationRoll);
					//Raise this box ever so slightly because Floating Point errors are a PITA.
					AxisAlignedBB offsetChildBox = child.getEntityBoundingBox().offset(posX + offset.xCoord - child.posX + motionX*speedFactor, posY + offset.yCoord - child.posY + motionY*speedFactor + 0.1, posZ + offset.zCoord - child.posZ + motionZ*speedFactor);
					if(getChildCollisions(child, offsetChildBox).isEmpty()){
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
			//trying to pitch up and rear ground devices are blocking it.  This needed to allow planes to
			//rotate on their landing gear.
			for(EntityMultipartChild child : childArray){
				while(motionPitch != 0){
					MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll);
					AxisAlignedBB offsetChildBox = child.getEntityBoundingBox().offset(posX + offset.xCoord - child.posX + motionX*speedFactor, posY + offset.yCoord - child.posY + motionY*speedFactor, posZ + offset.zCoord - child.posZ + motionZ*speedFactor);
					if(getChildCollisions(child, offsetChildBox).isEmpty()){
						break;
					}else if(motionPitch < 0){
						if(child.offsetZ <= 0 && child instanceof EntityGroundDevice){
							float yBoost = 0;
							for(AxisAlignedBB box : getChildCollisions(child, offsetChildBox)){
								if(box.maxY > offsetChildBox.minY + yBoost){
									yBoost += (box.maxY - offsetChildBox.minY);
								}
							}
							//Clamp the boost relative to the speed of the plane.
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
			for(EntityMultipartChild child : childArray){
				while(motionRoll != 0){
					MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
					AxisAlignedBB offsetChildBox = child.getEntityBoundingBox().offset(posX + offset.xCoord - child.posX + motionX*speedFactor, posY + offset.yCoord - child.posY + motionY*speedFactor, posZ + offset.zCoord - child.posZ + motionZ*speedFactor);
					if(getChildCollisions(child, offsetChildBox).isEmpty()){
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
				//In this case it's determined by what children are on the ground and the 
				//original motionY.  If we are supposed to go down by gravity, and can't because the
				//multipart is crooked, we need to fix this.
				
				//To do this we check which children are colliding in which motion groups and add pitch/roll.
				//Do this until we are able to do the entire motionY or until opposite groups are stable.
				//Parts with an offset of 0 in X or Z are ignored when calculating roll and pitch respectively.
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
					for(EntityMultipartChild child : childArray){
						MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
						AxisAlignedBB offsetChildBox = child.getEntityBoundingBox().offset(posX + offset.xCoord - child.posX + motionX*speedFactor, posY + offset.yCoord - child.posY + motionY*speedFactor, posZ + offset.zCoord - child.posZ + motionZ*speedFactor);
						if(!getChildCollisions(child, offsetChildBox).isEmpty()){
							if(child.offsetZ > 0){
								needPitchUp = true;
								needPitchUpAnytime = true;
							}
							if(child.offsetZ <= 0 && motionPitch >= 0){
								needPitchDown = true;
								needPitchDownAnytime = true;
							}
							if(child.offsetX > 0){
								needRollRight = true;
								needRollRightAnytime = true;
							}
							if(child.offsetX < 0){
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
	}
	
	/**
	 * Call this when moving multiparts to ensure they sync correctly.
	 * Failure to do this will result in things going badly!
	 */
	protected void moveMultipart(){
		prevRotationRoll = rotationRoll;
		if(!worldObj.isRemote){
			rotationYaw += motionYaw;
			rotationPitch += motionPitch;
			rotationRoll += motionRoll;
			setPosition(posX + motionX*speedFactor, posY + motionY*speedFactor, posZ + motionZ*speedFactor);
			addToServerDeltas(motionX, motionY, motionZ, motionYaw, motionPitch, motionRoll);
			MTS.MTSNet.sendToAll(new MultipartDeltaPacket(getEntityId(), motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor, motionYaw, motionPitch, motionRoll));
		}else{
			if(!(serverDeltaX == 0 && serverDeltaY == 0 && serverDeltaZ == 0)){
				if(gotDeltaPacket){
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
					gotDeltaPacket = false;
				}else{
					setPosition(posX + motionX*speedFactor, posY + motionY*speedFactor, posZ + motionZ*speedFactor);
					rotationYaw += motionYaw;
					rotationPitch += motionPitch;
					rotationRoll += motionRoll;
					addToClientDeltas(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor, motionYaw, motionPitch, motionRoll);
				}
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
		if(worldObj.isRemote){
			gotDeltaPacket = true;
		}
	}
	
	/**
	 * Checks collisions and returns the collision depth for an entity.
	 * Returns -1 and breaks the child if it had a hard collision.
	 * Returns -2 destroys the parent if the child is a core.
	 * Returns -3 if the child is a ground device that could be moved upwards to not collide (only for X and Z axis).
	 */
	private float getCollisionForAxis(EntityMultipartChild child, boolean xAxis, boolean yAxis, boolean zAxis){
		AxisAlignedBB offsetChildBox = child.getEntityBoundingBox().offset(xAxis ? this.motionX*speedFactor : 0, yAxis ? this.motionY*speedFactor : 0, zAxis ? this.motionZ*speedFactor : 0);
		
		//Add a slight vertical offset to collisions in the X or Z axis to prevent them from catching the ground.
		//Sometimes ground devices and the like end up with a lower level of 3.9999 due to floating-point errors
		//and as such and don't collide correctly with blocks above 4.0.  Can happen at other Y values too, but that
		//one shows up extensively in superflat world testing.
		if(xAxis || zAxis){
			offsetChildBox = offsetChildBox.offset(0, 0.05F, 0);
		}
		List<AxisAlignedBB> collidingAABBList = getChildCollisions(child, offsetChildBox);
		
		float collisionDepth = 0;
		for(AxisAlignedBB box : collidingAABBList){
			if(xAxis){
				collisionDepth = (float) Math.max(collisionDepth, motionX > 0 ? offsetChildBox.maxX - box.minX : box.maxX - offsetChildBox.minX);
			}
			if(yAxis){
				collisionDepth = (float) Math.max(collisionDepth, motionY > 0 ? offsetChildBox.maxY - box.minY : box.maxY - offsetChildBox.minY);
			}
			if(zAxis){
				collisionDepth = (float) Math.max(collisionDepth, motionZ > 0 ? offsetChildBox.maxZ - box.minZ : box.maxZ - offsetChildBox.minZ);
			}
			if(collisionDepth > 0.3){
				//This could be a collision, but it could also be that the child moved into a block and another
				//axis needs to collide here.  Check the motion and bail if so.
				if((xAxis && (Math.abs(motionX) < collisionDepth)) || (yAxis && (Math.abs(motionY) < collisionDepth)) || (zAxis && (Math.abs(motionZ) < collisionDepth))){
					return 0;
				}
			}
		}
		if(child instanceof EntityGroundDevice && !yAxis && collisionDepth > 0){
			//Ground device has collided.
			//Check to see if this collision can be avoided if the device is moved upwards.
			//Expand this box slightly to ensure we see the collision even with floating-point errors.
			offsetChildBox = child.getEntityBoundingBox().offset(xAxis ? this.motionX*speedFactor : 0, child.height*1.5F, zAxis ? this.motionZ*speedFactor : 0).expandXyz(0.05F);
			collidingAABBList = getChildCollisions(child, offsetChildBox);
			if(collidingAABBList.isEmpty()){
				//Ground device can be moved upward out of the way.
				//Return -3 and deal with this later.
				return -3;
			}else if(collisionDepth > 0.3){
				if(!worldObj.isRemote){
					this.removeChild(child.UUID, true);
				}
				return -1;
			}else{
				return collisionDepth;
			}
		}else if(collisionDepth > 0.3){
			if(child instanceof EntityCore){
				if(!worldObj.isRemote){
					this.destroyAtPosition(child.posX, child.posY, child.posZ);
				}
				return -2;
			}else{
				if(!worldObj.isRemote){
					this.removeChild(child.UUID, true);
				}
				return -1;
			}
		}else{
			return collisionDepth;
		}
	}

	
	/**
	 * Checks if a child is colliding with blocks, and returns the AABB of those blocks.
	 * If a soft block is encountered and this entity is going fast enough,
	 * it sets the soft block to air and slows down the entity.
	 * Used to plow though leaves and snow and the like. 
	 */
	private List<AxisAlignedBB> getChildCollisions(EntityMultipartChild child, AxisAlignedBB box){
		//Only check collisions on ground devices and cores.
		if(!(child instanceof EntityGroundDevice || child instanceof EntityCore)){
			return new ArrayList<AxisAlignedBB>();
		}
		
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
    					if(child.collidesWithLiquids() && worldObj.getBlockState(pos).getMaterial().isLiquid()){
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
	private void destroyAtPosition(double x, double y, double z){
		Entity controller = null;
		for(EntityMultipartChild child : getChildren()){
			if(child instanceof EntitySeat){
				if(((EntitySeat) child).isController){
					if(child.getRidingEntity() != null){
						controller = child.getRidingEntity();
						break;
					}
				}
			}
		}
		for(EntityMultipartChild child : getChildren()){
			if(child.getRidingEntity() != null){
				if(child.getRidingEntity().equals(controller)){
					child.getRidingEntity().attackEntityFrom(new DamageSourceCrash(null, this.pack.general.type), (float) (ConfigSystem.getDoubleConfig("CrashDamageFactor")*velocity*20));
				}else{
					child.getRidingEntity().attackEntityFrom(new DamageSourceCrash(controller, this.pack.general.type), (float) (ConfigSystem.getDoubleConfig("CrashDamageFactor")*velocity*20));
				}
			}
		}
		this.setDead();
		if(ConfigSystem.getBooleanConfig("Explosions")){
			if(this.getExplosionStrength() > 0){
				worldObj.newExplosion(this, x, y, z, this.getExplosionStrength(), true, true);
			}
		}
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
		for(EntityMultipartChild child : getChildren()){
			float addedFactor = 0;
			
			if(child instanceof EntityCore){
				if(!child.isDead){
					if(child.isOnGround()){
						addedFactor = 1;
					}
				}
			}else if(child instanceof EntityGroundDevice){
				if(brakeOn || parkingBrakeOn){
					if(child.isOnGround()){
						addedFactor = ((EntityGroundDevice) child).motiveFriction;
					}
				}
			}
			if(addedFactor != 0){
				//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything extra should increase it.
				float frictionLoss = 0.6F - child.worldObj.getBlockState(child.getPosition().down()).getBlock().slipperiness;
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
			float frictionLoss = 0.6F - grounder.worldObj.getBlockState(grounder.getPosition().down()).getBlock().slipperiness;
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
				float frictionLoss = 0.6F - grounder.worldObj.getBlockState(grounder.getPosition().down()).getBlock().slipperiness;
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
			}else if(!(child instanceof EntityCore)){
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
	
	/**
	 * Returns whatever the steering angle is.  Used for rendering and possibly other things.
	 */
	public abstract float getSteerAngle();
	
	/**
	 * Gets the strength of an explosion when this entity is destroyed.
	 * Is not used if explosions are disabled in the config.
	 */
	protected abstract float getExplosionStrength();
		
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.locked=tagCompound.getBoolean("locked");
		this.name=tagCompound.getString("name");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayText=tagCompound.getString("displayText");
		for(byte brokenWindow : tagCompound.getByteArray("brokenWindows")){
			this.brokenWindows.add(brokenWindow);
		}
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
		tagCompound.setString("name", this.name);
		tagCompound.setString("ownerName", this.ownerName);
		tagCompound.setString("displayText", this.displayText);
		
		byte[] brokenWindows = new byte[this.brokenWindows.size()];
		for(byte i=0; i<brokenWindows.length; ++i){
			brokenWindows[i] = this.brokenWindows.get(i);
		}
		tagCompound.setByteArray("brokenWindows", brokenWindows);
		
		tagCompound.setDouble("serverDeltaX", this.serverDeltaX);
		tagCompound.setDouble("serverDeltaY", this.serverDeltaY);
		tagCompound.setDouble("serverDeltaZ", this.serverDeltaZ);
		tagCompound.setFloat("serverDeltaYaw", this.serverDeltaYaw);
		tagCompound.setFloat("serverDeltaPitch", this.serverDeltaPitch);
		tagCompound.setFloat("serverDeltaRoll", this.serverDeltaRoll);
		return tagCompound;
	}
}
