package minecrafttransportsimulator.entities.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSEntity;
import minecrafttransportsimulator.minecrafthelpers.AABBHelper;
import minecrafttransportsimulator.minecrafthelpers.BlockHelper;
import minecrafttransportsimulator.minecrafthelpers.ItemStackHelper;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
/**General moving entity class.  This provides a basic set of variables and functions for moving entities.
 * Simple things like texture and display names are included, as well as standards for removal of this
 * entity based on names and damage.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartMoving extends EntityMultipartParent{
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public boolean openTop;
	//TODO set this on all planes.
	public byte maxTextLength;
	public byte textureOptions;
	public double velocity;
	public double health;
	public String ownerName=MTS.MODID;
	public String displayName="";
	
	private Map<AxisAlignedBB, Integer[]> collisionMap = new HashMap<AxisAlignedBB, Integer[]>();
	protected List<AxisAlignedBB> collidingBoxes = new ArrayList<AxisAlignedBB>();
		
	public EntityMultipartMoving(World world){
		super(world);
	}
	
	public EntityMultipartMoving(World world, float posX, float posY, float posZ, float playerRotation, byte textureOptions){
		super(world, posX, posY, posZ, playerRotation);
		this.textureOptions = textureOptions;
	}
	
	@Override
	protected void entityInit(){
		super.entityInit();
		initProperties();
	}
	
	/**
	 * Used to init the properties of this multi-part entity.
	 * Should be called by the last subclass.
	 */
	protected abstract void initProperties();
	
	/**
	 * Gets the strength of an explosion when this entity is destroyed.
	 * Is not used if explosions are disabled in the config.
	 */
	protected abstract float getExplosionStrength();
	
	@Override
	public boolean performRightClickAction(MTSEntity clicked, EntityPlayer player){
		if(!worldObj.isRemote){
			if(PlayerHelper.getHeldStack(player) != null){
				if(ItemStackHelper.getItemFromStack(PlayerHelper.getHeldStack(player)).equals(Items.name_tag)){
					this.displayName = PlayerHelper.getHeldStack(player).getDisplayName().length() > this.maxTextLength ? PlayerHelper.getHeldStack(player).getDisplayName().substring(0, this.maxTextLength - 1) : PlayerHelper.getHeldStack(player).getDisplayName();
					this.sendDataToClient();
					return true;
				}
			}
		}
		return super.performRightClickAction(clicked, player);
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				EntityPlayer attackingPlayer = (EntityPlayer) source.getEntity();
				if(attackingPlayer.isSneaking()){
					if(attackingPlayer.capabilities.isCreativeMode || attackingPlayer.getDisplayName().endsWith(this.ownerName)){
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
	
	protected List<AxisAlignedBB> getChildCollisions(EntityMultipartChild child, AxisAlignedBB box){
		//Need to contract the box because sometimes the slight error in math causes issues.
		collisionMap = AABBHelper.getCollidingBlockBoxes(worldObj, box.contract(0.01F, 0.01F,  0.01F), child.collidesWithLiquids());
		collidingBoxes.clear();
		if(!collisionMap.isEmpty()){
			for(Entry<AxisAlignedBB, Integer[]> entry : collisionMap.entrySet()){
				float hardness = BlockHelper.getBlockHardness(worldObj, entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]);
				if(hardness  <= 0.2F && hardness >= 0){
					BlockHelper.setBlockToAir(worldObj, entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]);
            		motionX *= 0.95;
            		motionY *= 0.95;
            		motionZ *= 0.95;
				}else{
					collidingBoxes.add(entry.getKey());
				}
			}
		}
		return collidingBoxes;
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
				weight += 1.2F*stack.stackSize/stack.getMaxStackSize()*(ConfigSystem.getStringConfig("HeavyItems").contains(ItemStackHelper.getItemFromStack(stack).getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
		
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.openTop=tagCompound.getBoolean("openTop");
		this.textureOptions=tagCompound.getByte("textureOptions");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayName=tagCompound.getString("displayName");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("brakeOn", this.brakeOn);
		tagCompound.setBoolean("parkingBrakeOn", this.parkingBrakeOn);
		tagCompound.setBoolean("openTop", this.openTop);
		tagCompound.setByte("textureOptions", this.textureOptions);
		tagCompound.setString("ownerName", this.ownerName);
		tagCompound.setString("displayName", this.displayName);
	}
}
