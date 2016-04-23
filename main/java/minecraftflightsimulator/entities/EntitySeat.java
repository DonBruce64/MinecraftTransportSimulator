package minecraftflightsimulator.entities;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.helpers.RotationHelper;
import minecraftflightsimulator.packets.general.ChatPacket;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntitySeat extends EntityChild{
	public boolean driver;
	private boolean hadRiderLastTick;
	private static ResourceLocation[] woodTextures = getWoodTextures();
	private static ResourceLocation[] woolTextures = getWoolTextures();
	
	public EntitySeat(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
	}
	
	public EntitySeat(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode, boolean driver){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
		this.driver=driver;
	}
	
	@Override
    public boolean interactFirst(EntityPlayer player){
		if(!worldObj.isRemote){
			if(riddenByEntity==null){
				player.mountEntity(this);
				return true;
			}else{
				MFS.MFSNet.sendTo(new ChatPacket("This seat is taken!"), (EntityPlayerMP) player);
			}
		}
		return false;
    }
	
	@Override
	public boolean canRiderInteract(){
		return true;
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!linked){return;}
		if(this.riddenByEntity != null){
			hadRiderLastTick=true;
			if(worldObj.isRemote){
				MFS.proxy.updateSeatedRider(this, (EntityLivingBase) this.riddenByEntity);
			}
		}else if(hadRiderLastTick){
			hadRiderLastTick=false;
			if(!worldObj.isRemote){
				parent.sendDataToClient();
			}
		}
	}
	
	@Override
	public void updateRiderPosition(){
		if(this.riddenByEntity != null && this.parent != null){
			Vec3 posVec = RotationHelper.getRotatedPoint(offsetX, (float) (offsetY + this.riddenByEntity.getYOffset()), (float) offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
			this.riddenByEntity.setPosition(parent.posX + posVec.xCoord, parent.posY + posVec.yCoord, parent.posZ + posVec.zCoord);
        }
	}
	
	public static ResourceLocation getFrameTexture(int propertyCode){
		return woodTextures[propertyCode >> 4];
	}
	
	public static ResourceLocation getSeatTexture(int propertyCode){
		return woolTextures[propertyCode & 15];
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.driver=tagCompound.getBoolean("driver");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("driver", this.driver);
	}
	
	
	private static ResourceLocation[] getWoodTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_jungle.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_acacia.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_big_oak.png");
		return texArray;
	}
	
	private static ResourceLocation[] getWoolTextures(){
		ResourceLocation[] texArray = new ResourceLocation[16];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_white.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_orange.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_magenta.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_light_blue.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_yellow.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_lime.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_pink.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_gray.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_silver.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_cyan.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_purple.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_blue.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_brown.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_green.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_red.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_black.png");
		return texArray;
	}
}
