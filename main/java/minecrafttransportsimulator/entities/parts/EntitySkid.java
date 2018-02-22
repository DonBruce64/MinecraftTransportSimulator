package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityGroundDevice;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntitySkid extends EntityGroundDevice{
	public EntitySkid(World world){
		super(world);
		this.setSize(0.3F, 0.3F);
	}
	
	public EntitySkid(World world, EntityMultipartParent plane, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, (EntityPlane) plane, parentUUID, offsetX, offsetY, offsetZ, 0.0F, 0.5F);
	}
	
	@Override
	protected float getWidth(){
		return 0.3F;
	}

	@Override
	protected float getHeight(){
		return 0.3F;
	}
	
	@Override
	public boolean shouldAffectSteering(){
		return true;
	}

	@Override
	public void setNBTFromStack(ItemStack stack){}

	@Override
	public ItemStack getItemStack(){
		return new ItemStack(MTSRegistry.skid);
	}
	
	@Override
	protected boolean attackChild(DamageSource source, float damage){
		return true;
	}
}
