package minecrafttransportsimulator.entities.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.helpers.EntityHelper;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.pack.PackCollisionBox;
import minecrafttransportsimulator.systems.pack.PackObject;
import minecrafttransportsimulator.systems.pack.PackParserSystem;
import minecrafttransportsimulator.systems.pack.PackPart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
/**General moving entity class.  This provides a basic set of variables and functions for moving entities.
 * Simple things like texture and display names are included, as well as standards for removal of this
 * entity based on names and damage.  This is the most basic class used for custom multipart entities.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartMoving extends EntityMultipartParent{


	public boolean openTop;
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public byte displayTextMaxLength;
	public double velocity;
	public double health;
	public String name="";
	public String ownerName=MTS.MODID;
	public String displayText="";
	
	/**
	 * Array containing part data for spawnable parts.
	 * This is populated after instantiation and may not be ready right after spawning.
	 * Data comes from {@link PackParserSystem} when {@link #writeToNBT(NBTTagCompound)} is called.
	 * Means there is a slight lag client-side for the population of this field, so do NOT
	 * assume it is populated on the client!
	 */
	protected List<PartData> partData;
			
	public EntityMultipartMoving(World world){
		super(world);
	}
	
	public EntityMultipartMoving(World world, float posX, float posY, float posZ, float playerRotation, String name){
		super(world, posX, posY, posZ, playerRotation);
		this.name = name;
		PackObject pack = PackParserSystem.getPack(name);
		//This only gets done at the beginning when the entity is first spawned.
		this.displayText = pack.general.defaultDisplayText;
		//Make sure all data for the PackParser in the NBT methods is inited now that we have a name.
		NBTTagCompound tempTag = new NBTTagCompound();
		this.writeEntityToNBT(tempTag);
		this.readFromNBT(tempTag);
	}


	public List<Float[]> getCollisionBoxes(){
		PackObject pack = PackParserSystem.getPack(name);
		List<Float[]> boxList = new ArrayList<Float[]>();
		for(PackCollisionBox box : pack.collision){
			boxList.add(new Float[]{box.pos[0], box.pos[1], box.pos[2], box.width, box.height});
		}
		return boxList;
	}
		
	/**
	 * Gets the strength of an explosion when this entity is destroyed.
	 * Is not used if explosions are disabled in the config.
	 */
	protected abstract float getExplosionStrength();
	
	@Override
	public boolean performRightClickAction(MTSEntity clicked, EntityPlayer player){
		if(!worldObj.isRemote){
			if(player.getRidingEntity() instanceof EntitySeat){
				if(this.equals(((EntitySeat) player.getRidingEntity()).parent)){
					//No in-use changes for sneaky sneaks!
					return false;
				}
			}else if(player.inventory.getCurrentItem() != null){
				if(player.inventory.getCurrentItem().getItem().equals(Items.NAME_TAG)){
					this.displayText = player.inventory.getCurrentItem().getDisplayName().length() > this.displayTextMaxLength ? player.inventory.getCurrentItem().getDisplayName().substring(0, this.displayTextMaxLength - 1) : player.inventory.getCurrentItem().getDisplayName();
					this.sendDataToClient();
					return true;
				}else if(EntityHelper.isPlayerHoldingWrench(player)){
					return false;
				}else{
					ItemStack heldStack = player.inventory.getCurrentItem();
					Item heldItem = player.inventory.getCurrentItem().getItem();
					EntityMultipartChild childClicked = (EntityMultipartChild) clicked;	
					PartData dataToSpawn = null;
					float closestPosition = 9999;
					
					//Look though the part data to find the class that goes with the held item.
					for(PartData data : partData){
						for(String partName : data.validNames){
							if(heldItem.getUnlocalizedName().equals(partName)){
								//The held item can spawn a part.
								//Now find the closest spot to put it.
								float distance = (float) Math.hypot(childClicked.offsetX - data.offsetX, childClicked.offsetZ - data.offsetZ);
								if(distance < closestPosition){
									//Make sure a part doesn't exist already.
									boolean childPresent = false;
									for(EntityMultipartChild child : this.getChildren()){
										if(child.offsetX == data.offsetX && child.offsetY == data.offsetY && child.offsetZ == data.offsetZ){
											childPresent = true;
											break;
										}
									}
									if(!childPresent){
										closestPosition = distance;
										dataToSpawn = data;
									}
								}
							}
						}
					}
					
					if(dataToSpawn != null){
						//We have a part, now time to spawn it.
						try{
							Constructor<? extends EntityMultipartChild> construct = MTSRegistry.partClasses.get(heldItem.getUnlocalizedName()).getConstructor(World.class, EntityMultipartParent.class, String.class, float.class, float.class, float.class, int.class);
							EntityMultipartChild newChild = construct.newInstance(worldObj, this, this.UUID, dataToSpawn.offsetX, dataToSpawn.offsetY, dataToSpawn.offsetZ, heldStack.getItemDamage());
							newChild.setNBTFromStack(heldStack);
							newChild.setTurnsWithSteer(dataToSpawn.turnsWithSteer);
							newChild.setController(dataToSpawn.isController);
							this.addChild(newChild.UUID, newChild, true);
							if(!player.capabilities.isCreativeMode){
								EntityHelper.removeItemFromHand(player);
							}
							return true;
						}catch(Exception e){
							System.err.println("ERROR SPAWING PART!");
							e.printStackTrace();
						}
					}
				}
			}
		}else if(EntityHelper.isPlayerHoldingWrench(player)){
			MTS.proxy.openGUI(this, player);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				EntityPlayer attackingPlayer = (EntityPlayer) source.getEntity();
				if(attackingPlayer.isSneaking()){
					if(attackingPlayer.capabilities.isCreativeMode || attackingPlayer.getDisplayName().getFormattedText().endsWith(this.ownerName)){
						this.setDead();
						return true;
					}
				}
			}
			if(!this.equals(source.getEntity())){
				if(!this.isDead){
					health -= damage;
					if(health <= 0){
						this.explodeAtPosition(this.posX, this.posY, this.posZ);
					}
				}
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
	
	/**
	 * Checks to see if a child can move in a specific direction.
	 * Returns true if no blocking blocks were found.
	 * Removes soft blocks if they are in the way. 
	 */
	protected boolean checkChildMovement(EntityMultipartChild child, AxisAlignedBB box){
		//Need to contract the box because sometimes the slight error in math causes issues.
		ListIterator<BlockPos> iterator = EntityHelper.getCollidingBlocks(worldObj, box.contract(0.01F), child.collidesWithLiquids()).listIterator();
		while(iterator.hasNext()){
			BlockPos pos = iterator.next();
			float hardness = worldObj.getBlockState(pos).getBlockHardness(worldObj, pos);
			if(hardness  <= 0.2F && hardness >= 0){
				worldObj.setBlockToAir(pos);
        		motionX *= 0.95;
        		motionY *= 0.95;
        		motionZ *= 0.95;
        		iterator.remove();
			}
		}
		return !(iterator.hasNext() || iterator.hasPrevious());
	}
	
	public void explodeAtPosition(double x, double y, double z){
		this.setDead();
		if(ConfigSystem.getBooleanConfig("Explosions")){
			worldObj.newExplosion(this, x, y, z, this.getExplosionStrength(), true, true);
		}
	}
	
	/**
	 * Calculates the weight of the inventory passed in.  Used for physics calculations.
	 * @param inventory
	 */
	public static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				weight += 1.2F*stack.stackSize/stack.getMaxStackSize()*(ConfigSystem.getStringConfig("HeavyItems").contains(stack.getItem().getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
		
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.name=tagCompound.getString("name");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayText=tagCompound.getString("displayText");
		PackObject pack = PackParserSystem.getPack(name);

		this.openTop = pack.general.openTop;
		this.displayTextMaxLength = (byte) pack.general.displayTextMaxLength;
		
		partData = new ArrayList<PartData>();

		for(PackPart part : pack.parts){
			partData.add(new PartData(part.pos[0], part.pos[1], part.pos[2], part.turnsWithSteer, part.isController, part.names));
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("brakeOn", this.brakeOn);
		tagCompound.setBoolean("parkingBrakeOn", this.parkingBrakeOn);
		tagCompound.setString("name", this.name);
		tagCompound.setString("ownerName", this.ownerName);
		tagCompound.setString("displayText", this.displayText);
		return tagCompound;
	}
	
	/**This class contains data for parts that can be attached to or are attached to this parent.
	 * These are added by the data parsed by the {@link PackParserSystem}.
	 * Data is used during item clicks to determine the part to spawn.
	 * 
	 *@author don_bruce
	 */
	protected class PartData{
		public final float offsetX;
		public final float offsetY;
		public final float offsetZ;
		public final boolean turnsWithSteer;
		public final boolean isController;
		public final String[] validNames;
		
		public PartData(float offsetX, float offsetY, float offsetZ, boolean turnsWithSteer, boolean isController, String... validNames){
			this.turnsWithSteer = turnsWithSteer;
			this.isController = isController;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.validNames = validNames;
		}
	}
}
