package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONSign;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.blocks.RenderPoleSign;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;

/**Sign tile entity.  Remembers which sign we are, as well as what text lines we have on that sign.
*
* @author don_bruce
*/
public class TileEntityPoleSign extends ATileEntityBase{
	public JSONSign definition;
	public List<String> text = new ArrayList<String>();
	
	@Override
    public void load(WrapperNBT data){
        definition = MTSRegistry.packSignMap.get(data.getString("packID")).get(data.getString("systemName"));
        if(definition.general.textLines != null){
        	text = data.getStrings("text", definition.general.textLines.length);
        }
    }
    
	@Override
    public void save(WrapperNBT data){
		data.setString("packID", definition.packID);
        data.setString("systemName", definition.systemName);
        data.setStrings("text", text);
    }
	
	@Override
	public ARenderTileEntityBase<? extends ATileEntityBase, ? extends WrapperTileEntity.IProvider> getRenderer(){
		return new RenderPoleSign();
	}
}
