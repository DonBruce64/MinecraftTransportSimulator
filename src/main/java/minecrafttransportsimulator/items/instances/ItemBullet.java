package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

public class ItemBullet extends AItemSubTyped<JSONBullet>{
	
	public ItemBullet(JSONBullet definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		for(BulletType type : definition.bullet.types){
				tooltipLines.add(InterfaceCore.translate("info.item.bullet.type." + type.name().toLowerCase()));
		}
		tooltipLines.add(InterfaceCore.translate("info.item.bullet.diameter") + definition.bullet.diameter);
		tooltipLines.add(InterfaceCore.translate("info.item.bullet.caseLength") + definition.bullet.caseLength);
		tooltipLines.add(InterfaceCore.translate("info.item.bullet.penetration") + definition.bullet.armorPenetration);
		tooltipLines.add(InterfaceCore.translate("info.item.bullet.quantity") + definition.bullet.quantity);
	}
}
