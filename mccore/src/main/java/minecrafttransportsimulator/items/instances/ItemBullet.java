package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;

public class ItemBullet extends AItemSubTyped<JSONBullet> {

    public ItemBullet(JSONBullet definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        for (BulletType type : definition.bullet.types) {
            switch (type) {
                case ARMOR_PIERCING:
                    tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_TYPE_ARMOR_PIERCING.value);
                    break;
                case EXPLOSIVE:
                    tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_TYPE_EXPLOSIVE.value);
                    break;
                case INCENDIARY:
                    tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_TYPE_INCENDIARY.value);
                    break;
                case WATER:
                    tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_TYPE_WATER.value);
                    break;
            }

        }
        if (definition.bullet.pellets > 0)
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_PELLETS.value + definition.bullet.pellets);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_DIAMETER.value + definition.bullet.diameter);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_CASELENGTH.value + definition.bullet.caseLength);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_PENETRATION.value + definition.bullet.armorPenetration);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_BULLET_QUANTITY.value + definition.bullet.quantity);
    }
}