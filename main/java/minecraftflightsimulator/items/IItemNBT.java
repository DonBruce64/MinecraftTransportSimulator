package minecraftflightsimulator.items;

import minecraftflightsimulator.entities.core.EntityChild;
import net.minecraft.item.ItemStack;

public interface IItemNBT{
	public ItemStack createStackFromEntity(EntityChild entity);
}
