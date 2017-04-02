package minecrafttransportsimulator.minecrafthelpers;

import java.util.List;

import minecrafttransportsimulator.entities.core.EntityMultipartBase;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class EntityHelper{
	
	public static EntityMultipartBase getEntityByUUID(World world, String UUID){
		for(Object obj : world.loadedEntityList){
			if(obj instanceof EntityMultipartBase){
				if(UUID.equals(((EntityMultipartBase) obj).UUID)){
					return (EntityMultipartBase) obj;
				}
			}
		}
		return null;
	}
	
	public static List<Entity> getEntitiesThatCollideWithBox(World world, Class classToFind, AxisAlignedBB box){
		return world.getEntitiesWithinAABB(classToFind, box);
	}
	
	public static void setRider(Entity rider, Entity ridden){
		rider.mountEntity(ridden);
	}

	public static Entity getRider(Entity ridden){
		return ridden.riddenByEntity;
	}
	
	public static void attackEntity(Entity entity, DamageSource damageType, float damageAmount){
		entity.attackEntityFrom(damageType, damageAmount);
	}
}
