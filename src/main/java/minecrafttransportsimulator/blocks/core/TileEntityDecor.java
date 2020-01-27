package minecrafttransportsimulator.blocks.core;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityDecor extends TileEntityBase{
	public JSONDecor definition;
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.definition = (JSONDecor) MTSRegistry.packItemMap.get(tagCompound.getString("packID")).get(tagCompound.getString("systemName")).definition;
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setString("packID", this.definition.packID);
        tagCompound.setString("systemName", this.definition.systemName);
        return tagCompound;
    }
}
