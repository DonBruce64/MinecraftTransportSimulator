package minecraftflightsimulator.dataclasses;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;

public abstract class MTSDamageSources extends DamageSource{
	//Source of the damage.  This will be the vehicle controller or the player who shot the gun.
	private final Entity playerResponsible;
	
	public MTSDamageSources(String name, Entity playerResponsible){
		super(name);
		this.playerResponsible = playerResponsible;
	}
	
	@Override
	public IChatComponent func_151519_b(EntityLivingBase player){
		EntityLivingBase recentEntity = player.func_94060_bK();
		if(recentEntity != null){//Player engaged with another player...
			if(playerResponsible != null){//and then was killed by another player.
				return new ChatComponentTranslation("death.attack." + this.damageType + ".player.player", 
						new Object[] {player.func_145748_c_(), playerResponsible.func_145748_c_(), recentEntity.func_145748_c_()});
			}else{//and then was killed by something.
				return new ChatComponentTranslation("death.attack." + this.damageType + ".null.player", 
						new Object[] {player.func_145748_c_(), recentEntity.func_145748_c_()});
			}
		}else{//Player was minding their own business...
			if(playerResponsible != null){//and was killed by another player.
				return new ChatComponentTranslation("death.attack." + this.damageType + ".player.null", 
						new Object[] {player.func_145748_c_(), playerResponsible.func_145748_c_()});
			}else{//and then was killed by something.
				return new ChatComponentTranslation("death.attack." + this.damageType + ".null.null", 
						new Object[] {player.func_145748_c_()});
			}
		}
	                
	}
	
	public static class DamageSourcePropellor extends MTSDamageSources{
		public DamageSourcePropellor(Entity playerResponsible){
			super("propellor", playerResponsible);
		}
	};

	public static class DamageSourceCrash extends MTSDamageSources{
		public DamageSourceCrash(Entity playerResponsible, String entityCrashed){
			super(entityCrashed + "crash", playerResponsible);
		}
	};
}
