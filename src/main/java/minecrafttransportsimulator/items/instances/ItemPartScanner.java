package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.MasterLoader;

public class ItemPartScanner extends AItemBase{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		for(byte i=1; i<=2; ++i){
			tooltipLines.add(MasterLoader.coreInterface.translate("info.item.partscanner.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public boolean canBeStacked(){
		return false;
	}
}
