package minecrafttransportsimulator.entities.core;

import minecrafttransportsimulator.baseclasses.MTSEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class EntityMultipartBase extends MTSEntity{
	public boolean linked;
	public String UUID = "";



	public EntityMultipartBase(World world){
		super(world);
	}
	
	public boolean hasUUID(){
		if(this.UUID.equals("")){
			if(this.worldObj.isRemote){
				if(this.ticksExisted==1 || this.ticksExisted%10==0){
					this.requestDataFromServer();
				}
				return false;
			}else{
				this.UUID=String.valueOf(this.getUniqueID());
			}
		}
		return true;
	}
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.UUID=tagCompound.getString("UUID");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setString("UUID", this.UUID);
		return tagCompound;
	}
}
