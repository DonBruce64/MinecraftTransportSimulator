package minecrafttransportsimulator.blocks.pole;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONSign;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityPoleSign extends TileEntityPoleAttachment{
	public String definition = "";
	public List<String> text = new ArrayList<String>();
	
	public TileEntityPoleSign(){
		super();
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.definition = tagCompound.getString("definition");
        JSONSign pack = PackParserSystem.getSign(definition);
        if(pack != null){
	        text.clear();
	        if(PackParserSystem.getSign(definition).general.textLines != null){
		        for(byte i=0; i<PackParserSystem.getSign(definition).general.textLines.length; ++i){
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
        tagCompound.setString("definition",  this.definition);
        for(byte i=0; i<text.size(); ++i){
        	tagCompound.setString("text" + String.valueOf(i), text.get(i));
        }
        return tagCompound;
    }
}
