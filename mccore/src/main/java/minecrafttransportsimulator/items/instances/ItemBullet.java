package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.systems.LanguageSystem;

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
                    tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_TYPE_ARMOR_PIERCING.getCurrentValue());
                    break;
                case EXPLOSIVE:
                    tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_TYPE_EXPLOSIVE.getCurrentValue());
                    break;
                case INCENDIARY:
                    tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_TYPE_INCENDIARY.getCurrentValue());
                    break;
                case WATER:
                    tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_TYPE_WATER.getCurrentValue());
                    break;
            }

        }
        if (definition.bullet.pellets > 0)
            tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_PELLETS.getCurrentValue() + definition.bullet.pellets);
        tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_DIAMETER.getCurrentValue() + definition.bullet.diameter);
        tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_CASELENGTH.getCurrentValue() + definition.bullet.caseLength);
        tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_PENETRATION.getCurrentValue() + definition.bullet.armorPenetration);
        tooltipLines.add(LanguageSystem.ITEMINFO_BULLET_QUANTITY.getCurrentValue() + definition.bullet.quantity);
    }
}