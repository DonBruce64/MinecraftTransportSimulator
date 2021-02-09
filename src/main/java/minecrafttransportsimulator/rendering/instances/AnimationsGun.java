package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Gun;
import minecrafttransportsimulator.rendering.components.AAnimationsBase;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;

/**This class contains methods for gun animations when held by players.
 * For animations of the gun on vehicles, see {@link AnimationsPart}
 *
 * @author don_bruce
 */
public final class AnimationsGun extends AAnimationsBase<EntityPlayerGun>{
	
	@Override
	public double getRawVariableValue(EntityPlayerGun entity, String variable, float partialTicks){
		//First check if we are a base variable.
		double value = getBaseVariableValue(entity, variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}
		
		value = getGunVariable(entity.gun, variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}else{
			return 0;
		}
	}
	
	public static double getGunVariable(Gun gun, String variable, float partialTicks){
		if(gun != null){
			//Check for an instance of a gun_muzzle_# variable, since these requires additional parsing
			if (variable.startsWith("gun_muzzle_")){
				//Get the rest of the variable after gun_muzzle_
				String muzzleVariable = variable.substring("gun_muzzle_".length());
				//Parse one or more digits, then take off one because we are zero-indexed
				int muzzleNumber = Integer.parseInt(muzzleVariable.substring(0, muzzleVariable.indexOf('_'))) - 1;
				switch(muzzleVariable.substring(muzzleVariable.indexOf('_') + 1)) {
					case("firing"): return (muzzleNumber == gun.currentMuzzle ? 1 : 0) * gun.cooldownTimeRemaining/(double)gun.definition.gun.fireDelay;
				}
			}
			switch(variable){
				case("gun_inhand"): return gun.provider instanceof EntityPlayerGun ? 1 : 0;	
				case("gun_active"): return gun.active ? 1 : 0;
				case("gun_firing"): return gun.firing ? 1 : 0;
				case("gun_pitch"): return gun.prevOrientation.x + (gun.currentOrientation.x - gun.prevOrientation.x)*partialTicks;
				case("gun_yaw"): return gun.prevOrientation.y + (gun.currentOrientation.y - gun.prevOrientation.y)*partialTicks;
				case("gun_cooldown"): return gun.cooldownTimeRemaining > 0 ? 1 : 0;
				case("gun_windup_time"): return gun.windupTimeCurrent;
				case("gun_windup_rotation"): return gun.windupRotation;
				case("gun_windup_complete"): return gun.windupTimeCurrent == gun.definition.gun.windupTime ? 1 : 0;
				case("gun_reload"): return gun.reloadTimeRemaining > 0 ? 1 : 0;
				case("gun_ammo_count"): return gun.bulletsLeft;
				case("gun_ammo_percent"): return gun.bulletsLeft/gun.definition.gun.capacity;
			}
		}
		
		//Not a base variable, or a gun variable.  Return NaN to indicate this.
		return Double.NaN;
	}
}
