package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityRollingStock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityBogie extends EntityMultipartChild{

	public EntityBogie(World world){
		super(world);
	}
	
	public EntityBogie(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityRollingStock) parent, parentUUID, offsetX, offsetY, offsetZ, 2.75F, 0.75F, propertyCode);
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public void onUpdate(){
		
	}
	
	@Override
	public ItemStack getItemStack(){
		// TODO Auto-generated method stub
		return null;
	}

}
