package minecrafttransportsimulator.baseclasses;

import mcinterface.WrapperEntity;

/**Basic damage class.  Used to make instances of damage to apply to entities.  Allows for quick addition
 * of new damage types that prevents the need to change constructors.  The boundingBox hit is required for
 * construction, as some entities may do different things based on what bounding box was hit.  If this is
 * an "area of effect" attack, and not a target attack, the BoundingBox should be the area of effect, not
 * a specific box.  This is mainly for attacks that hurt all entities within the area, but don't target a
 * specific box or part on the entity.
 * <br><br>
 * The passed-in entity here represents the entity that is responsible for the damage.  The player responsible
 * may not be the actual entity that attacked.  For example, a player that fires a gun on a car will spawn bullet,
 * and the bullet may hit another player, but it's the player who shot the gun that is responsible, not the bullet.
 * Conversely, if a player starts a jet engine and walks away, and then another player gets sucked into the jet engine,
 * the player who started the engine isn't responsible, as they weren't controlling the vehicle at the time.
 *
 * @author don_bruce
 */
public class Damage{
	public final String name;
	public final double amount;
	public final BoundingBox box;
	public final WrapperEntity attacker;
	
	public boolean isFire;
	public boolean isWater;
	public boolean isExplosion;
	public boolean ignoreArmor;
	public boolean ignoreCooldown;
	
	public Damage(String name, double amount, BoundingBox box, WrapperEntity attacker){
		this.name = name;
		this.amount = amount;
		this.box = box;
		this.attacker = attacker;
	}
	
	/**
	 * Sets this damage as fire damage.
	 * Returns object for construction simplicity.
	 */
	public Damage setFire(){
		this.isFire = true;
		return this;
	}
	
	/**
	 * Sets this damage as water damage.
	 * Returns object for construction simplicity.
	 */
	public Damage setWater(){
		this.isWater = true;
		return this;
	}
	
	/**
	 * Sets this damage as explosive damage.
	 * Returns object for construction simplicity.
	 */
	public Damage setExplosive(){
		this.isExplosion = true;
		return this;
	}
	
	/**
	 * Sets this damage to ignore armor.
	 * Returns object for construction simplicity.
	 */
	public Damage ignoreArmor(){
		this.ignoreArmor = true;
		return this;
	}
	
	/**
	 * Sets this damage to ignore cooldown.
	 * Returns object for construction simplicity.
	 */
	public Damage ignoreCooldown(){
		this.ignoreCooldown = true;
		return this;
	}
}
