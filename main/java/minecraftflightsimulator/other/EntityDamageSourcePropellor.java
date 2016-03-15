package minecraftflightsimulator.other;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSource;

public class EntityDamageSourcePropellor extends EntityDamageSource{

	public EntityDamageSourcePropellor(String name, Entity transmitter){
		super(name, transmitter);
		this.damageType="propellor";
	}
}
