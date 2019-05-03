package minecrafttransportsimulator.dataclasses;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public abstract class DamageSources extends DamageSource{
	//Source of the damage.  This will be the vehicle controller or the player who shot the gun.
	private final Entity playerResponsible;
	
	public DamageSources(String name, Entity playerResponsible){
		super(name);
		this.playerResponsible = playerResponsible;
	}
	
	@Override
	public ITextComponent getDeathMessage(EntityLivingBase player){
		EntityLivingBase recentEntity = player.getLastAttacker();
		if(recentEntity != null){//Player engaged with another player...
			if(playerResponsible != null){//and then was killed by another player.
				return new TextComponentTranslation("death.attack." + this.damageType + ".player.player", 
						new Object[] {player.getDisplayName(), playerResponsible.getDisplayName(), recentEntity.getDisplayName()});
			}else{//and then was killed by something.
				return new TextComponentTranslation("death.attack." + this.damageType + ".null.player", 
						new Object[] {player.getDisplayName(), recentEntity.getDisplayName()});
			}
		}else{//Player was minding their own business...
			if(playerResponsible != null){//and was killed by another player.
				return new TextComponentTranslation("death.attack." + this.damageType + ".player.null", 
						new Object[] {player.getDisplayName(), playerResponsible.getDisplayName()});
			}else{//and then was killed by something.
				return new TextComponentTranslation("death.attack." + this.damageType + ".null.null", 
						new Object[] {player.getDisplayName()});
			}
		}
	                
	}
	
	public static class DamageSourcePropellor extends DamageSources{
		public DamageSourcePropellor(Entity playerResponsible){
			super("propellor", playerResponsible);
		}
	};
	
	public static class DamageSourceJet extends DamageSources{
		public DamageSourceJet(Entity playerResponsible, boolean intake){
			super("jet" + (intake ? "_intake" : "_exhaust"), playerResponsible);
		}
	};
	
	public static class DamageSourceWheel extends DamageSources{
		public DamageSourceWheel(Entity playerResponsible){
			super("wheel", playerResponsible);
		}
	};

	public static class DamageSourceCrash extends DamageSources{
		public DamageSourceCrash(Entity playerResponsible, String entityCrashed){
			super(entityCrashed + "crash", playerResponsible);
		}
	};
	
	public static class DamageSourceBullet extends DamageSources{
		public DamageSourceBullet(Entity playerResponsible, String bulletType){
			super("bullet." + bulletType, playerResponsible);
			if(bulletType.equals("incendiary")){
				this.setFireDamage();
			}else if(bulletType.equals("armor_piercing")){
				this.setDamageBypassesArmor();
			}
			//We don't need explosive damage here as that spawns explosions that will damage the player instead.
		}
	};
}
