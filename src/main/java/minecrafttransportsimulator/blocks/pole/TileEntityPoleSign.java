package minecrafttransportsimulator.blocks.pole;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONSign;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityPoleSign extends TileEntityPoleAttachment{
	public JSONSign definition;
	public List<String> text = new ArrayList<String>();
	
	public TileEntityPoleSign(){
		super();
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        if(tagCompound.hasKey("packID")){
	        this.definition = MTSRegistry.packSignMap.get(tagCompound.getString("packID")).get(tagCompound.getString("systemName"));
	        text.clear();
	        if(definition.general.textLines != null){
		        for(byte i=0; i<definition.general.textLines.length; ++i){
		        	if(tagCompound.hasKey("text" + String.valueOf(i))){
		        		text.add(tagCompound.getString("text" + String.valueOf(i)));
		        	}else{
		        		text.add("");
		        	}
		        }
	        }
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setString("packID",  this.definition.packID);
        tagCompound.setString("systemName",  this.definition.systemName);
        for(byte i=0; i<text.size(); ++i){
        	tagCompound.setString("text" + String.valueOf(i), text.get(i));
        }
        return tagCompound;
    }
}
