package minecrafttransportsimulator.blocks.decor;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.dataclasses.PackSignObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityDecor6AxisSign extends TileEntityDecor6AxisOriented{
	public String definition = "";
	public List<String> text = new ArrayList<String>();
	
	public TileEntityDecor6AxisSign(){
		super();
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.definition = tagCompound.getString("definition");
        PackSignObject pack = PackParserSystem.getSign(definition);
        if(pack != null){
	        text.clear();
	        for(byte i=0; i<PackParserSystem.getSign(definition).general.textLines.length; ++i){
	        	if(tagCompound.hasKey("text" + String.valueOf(i))){
	        		text.add(tagCompound.getString("text" + String.valueOf(i)));
	        	}else{
	        		text.add("");
	        	}
	        }
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setString("definition",  this.definition);
        for(byte i=0; i<text.size(); ++i){
        	tagCompound.setString("text" + String.valueOf(i), text.get(i));
        }
        return tagCompound;
    }
}
