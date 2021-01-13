package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.LinkedHashMap;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.rendering.components.ITextProvider;

/**Sign pole component.  Renders a sign texture and text.
*
* @author don_bruce
*/
public class TileEntityPole_Sign extends ATileEntityPole_Component implements ITextProvider{
	
	public final LinkedHashMap<JSONText, String> text = new LinkedHashMap<JSONText, String>();
	
	public TileEntityPole_Sign(TileEntityPole core, ItemPoleComponent item){
		super(core, item);
		//Populate the textLines with blank strings at construction.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(byte i=0; i<definition.rendering.textObjects.size(); ++i){
				text.put(definition.rendering.textObjects.get(i), "");
			}
		}
	}
	
	@Override
	public float lightLevel(){
		return 0;
	}

	@Override
	public LinkedHashMap<JSONText, String> getText(){
		return text;
	}

	@Override
	public String getSecondaryTextColor(){
		for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(item.subName)){
				return subDefinition.secondColor;
			}
		}
		throw new IllegalArgumentException("Tried to get the definition for a pole of subName:" + item.subName + ".  But that isn't a valid subName for the pole:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
	}

	@Override
	public boolean renderTextLit(){
		return true;
	}
}
