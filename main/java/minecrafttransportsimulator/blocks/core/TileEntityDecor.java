package minecrafttransportsimulator.blocks.core;

import net.minecraft.nbt.NBTTagCompound;

public class TileEntityDecor extends TileEntityRotatable{
	public String decorName;
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.decorName = tagCompound.getString("decorName");
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setString("decorName", this.decorName);
        return tagCompound;
    }
}
