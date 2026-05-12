package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("Bullets are special JSONs, as they're not a part, but they work with parts.  They're not an actual item like food, but they normally are only in item form.  Rather, they are an item that turns itself into a model with logic when fired from a gun.")
public class JSONBullet extends AJSONMultiModelProvider {

    @JSONRequired
    @JSONDescription("Bullet-specific properties.")
    public Bullet bullet;

    public static class Bullet {
        @JSONRequired
        @JSONDescription("A list of types describing the bullet.  This defines how it inflicts damage on whatever it hits.")
        public List<BulletType> types;

        @JSONDescription("Defines the method for how a bullet will guide to it's target.")
        public GuidanceType guidanceType;

        @JSONDescription("How far away the gun will be able to lock targets.")
        public int seekerRange;

        @JSONDescription("Angle in degrees around gun's orientation that it wil see targets.")
        public double seekerMaxAngle;

        @JSONDescription("Set this to true to make this bullet not spawn, but consume ammo.")
        public boolean isBlank;

        @JSONDescription("If true, then this bullet will be considered a HEAT bullet and will use the HEAT armor value on any collision boxes it finds.  If that value isn't defined, it will just use the normal armor value.")
        public boolean isHeat;

        @JSONDescription("Normally, bullet checks are handled only on the client that spawned them.  This client then sends the info to the server when it sees a hit.  This works best for most bullets, since it prevents the firing player from 'missing' something they hit due to lag.  However, this prevents bullets from hitting things that aren't loaded.  Setting this to true will make the bullet do checks on the server, which will let them hit anything loaded on the server, but will result in de-syncs between hit position seen and actual hit position if the gun is moving at any significant speed when fired.")
        public boolean isLongRange;

        @JSONDescription("How many bullets are in the bullet item crafted at the bullet bench. Because nobody wants to have to craft 500 bullets one by one...")
        public int quantity;

        @JSONDescription("The diameter of the bullet.  This determines what guns can fire it, as well as the damage it inflicts.  Units are in mm.")
        public float diameter;

        @JSONDescription("How much damage this bullet does.  Is set to 1/5 the diameter if left out.  Note that 'water' type bullets don't damage things, no matter this value.")
        public float damage;

        @JSONDescription("The case length of the bullet.  This determines what guns can fire it, but does not affect damage.  Units are in mm.")
        public float caseLength;

        @JSONDescription("Only affects explosive bullets.  The damage dealt and size of the blast radius are normally determined by the diameter of the bullet, but you can override that by setting this value. A value of 1 is about equivalent to a single block of TNT. Useful if you want a little more oomph in your explosions, or if you want to tone them down.")
        public float blastStrength;

        @JSONDescription("If set, this bullet, when it hits an entity, will push it back this far.")
        public float knockback;

        @JSONDescription("How much armor this bullet can penetrate, in mm.  This allows the bullet to pass through any collision boxes with armorThickness set less than this value.  Note that as the bullet slows down, this value will decrease, so a bullet with 100 penetration may not pass through a collision box with 90 armor if it slows down enough prior to contact.")
        public float armorPenetration;

        @JSONDescription("If this is a guided bullet, it will detonate this many meters away from its target. For a non-guided bullet, it will detonate when it has a block this many meters or less in front of it. This allows things like missiles and artillery rounds that blow up in the air right above the target or the ground, ensuring that the target receives some of the blast rather than it all going into the ground. If used on a non-explosive bullet, it will not detonate, but will despawn at this distance.")
        public float proximityFuze;

        @JSONDescription("Causes the bullet to explode or despawn after this many ticks. This is a 'dumber' cousin of the proximityFuze, but may be useful for anti-aircraft rounds that explode in mid-air.")
        public int airBurstDelay;

        @JSONDescription("How much velocity, each tick, should be deducted from the bullet's velocity.")
        public float slowdownSpeed;

        @JSONDescription("How much velocity, each tick, should be added in the -Y direction.  Used to make bullets travel in arcs.")
        public float gravitationalVelocity;

        @JSONDescription("How long, in ticks, the bullet should keep its initial velocity. This simulates a rocket motor that is present in rockets and missiles. The bullet will not be affected by gravity or slow down until this amount of time has elapsed.")
        public int burnTime;

        @JSONDescription("How long, in ticks, the bullet should take to accelerate from its initial velocity to its maxVelocity (if maxVelocity is used). Note that if this is greater than burnTime, it will not continue to accelerate once burnTime has expired.")
        public int accelerationTime;

        @JSONDescription("How long, in ticks, to delay initial acceleration.  This can be combined with the initial velocity from the gun to make missiles that detach before igniting.")
        public int accelerationDelay;

        @JSONDescription("The maximum velocity of this bullet, in m/s. If this and accelerationTime are used, the bullet will be spawned with the gun's muzzleVelocity + the vehicle's motion, then it will accelerate at a constant rate and reach maxVelocity when the accelerationTime is about to expire.")
        public int maxVelocity;

        @JSONDescription("The time between bullet firing and de-spawning.  Normally 200 ticks (10 seconds) but can be shorter or longer as needed.  e.g., water might not need to exist more than 2 seconds, but a lob shot from artillery might need 30 seconds.")
        public int despawnTime;

        @JSONDescription("The time between when the bullet impacts, and it despawns.  Normally 0 since you want bullets to go away when they impact.  But can be higher if you want them to do fancy animations or sounds.")
        public int impactDespawnTime;

        @JSONDescription("The time it takes for the missile to begin turning if guided.")
        public int guidanceDelay;

        @JSONDescription("The rate of turn, in degrees per tick, that this bullet will be able to turn to track entities.  If set, then this bullet will lock-on to entities, and hot engines when fired.")
        public float turnRate;

        @JSONDescription("A optional list of effects that this bullet will impart on the entity that it hits.")
        public List<JSONPotionEffect> effects;

        @JSONDescription("Number of pellets that this shell has")
        public int pellets;

        @JSONDescription("How much spread the pellets will have when fired. 0 is no spread, higher values have higher spread.")
        public float pelletSpreadFactor;

        @JSONDescription("The model of the casing to use for the casing particle, or null if no model is to be used.")
        public String casingModel;

        @JSONDescription("The texture of the casing to use for the casing particle, or null if no casing is to be rendered.")
        public String casingTexture;

        @JSONDescription("A optional list of code-defined functions to be called if the bullet types include custom.")
        public List<String> customHitFunctions;

        @JSONDescription("The base damage dealt at the explosion source. If set, the custom explosion system is used instead of the vanilla Minecraft explosion. Damage falls off linearly from the center to the edge of the blast radius.")
        public float blastDamage;

        @JSONDescription("The maximum horizontal distance (in blocks) that explosion damage can be received. Used together with blastDamage.")
        public float blastRadiusXZ;

        @JSONDescription("The maximum vertical distance (in blocks) that explosion damage can be received. If 0, defaults to blastRadiusXZ for a spherical blast.")
        public float blastRadiusY;

        @JSONDescription("The radius (in blocks) within which maximum damage is dealt regardless of distance. No falloff is applied inside this radius.")
        public float maxDamageRadius;

        @JSONDescription("Damage dealt to all vehicles (both aircraft and ground). If 0, falls back to blastDamage.")
        public float blastDamageVsVehicles;

        @JSONDescription("Damage dealt specifically to aircraft. If 0, falls back to blastDamageVsVehicles, then blastDamage.")
        public float blastDamageVsAircraft;

        @JSONDescription("Damage dealt specifically to ground vehicles. If 0, falls back to blastDamageVsVehicles, then blastDamage.")
        public float blastDamageVsGround;

        @JSONDescription("Damage dealt to mobs and players. If 0, falls back to blastDamage.")
        public float blastDamageVsLiving;

        @JSONDescription("The maximum horizontal distance (in blocks) that blastStrength can be applied to blocks for destruction.")
        public float blastStrengthRadiusXZ;

        @JSONDescription("The maximum vertical distance (in blocks) that blastStrength can be applied to blocks. If 0, defaults to blastStrengthRadiusXZ.")
        public float blastStrengthY;

        @JSONDescription("The radius (in blocks) within which blastStrength is applied to blocks at full power regardless of distance.")
        public float maxStrengthRadius;

        @JSONDescription("How long, in ticks, after detonation the area inside the blast radius will continue to apply potion effects to entities entering it. Works like lingering potions. If 0, effects are only applied once at the moment of explosion.")
        public int effectDuration;

        @JSONDescription("The maximum horizontal distance (in blocks) that armor penetration is applied in the explosion.")
        public float armorPenRadiusXZ;

        @JSONDescription("The maximum vertical distance (in blocks) that armor penetration is applied in the explosion. If 0, defaults to armorPenRadiusXZ.")
        public float armorPenRadiusY;

        @JSONDescription("The radius (in blocks) within which full armor penetration is applied regardless of distance.")
        public float maxPenRadius;
    }

    public enum BulletType {
        @JSONDescription("Explodes when it hits something.  Explosion size is based on the bullet's diameter.")
        EXPLOSIVE,
        @JSONDescription("Sets whatever it hits on fire, if it's flammable.  This includes entities.")
        INCENDIARY,
        @JSONDescription("Like incendiary, but puts out fires rather than starts them.")
        WATER,
        @JSONDescription("A bullet that pierces player armor.  Useful for pesky super-suits.")
        ARMOR_PIERCING,
        @JSONDescription("A bullet that has a custom function defined in code. Useful for integration with a variety of mods, regardless of version.")
        CUSTOM
    }

    public enum GuidanceType {
        @JSONDescription("Will track whatever target that was locked prior to firing, However, It will guide to the closest target it can 'see'. Therefore, it is possible to fire without a lock and have it find it's own target with the downside of it being possible to fool.")
        PASSIVE,
        @JSONDescription("Tracks the locked target but if the lock is lost, guidance stops.")
        SEMI_ACTIVE,
        @JSONDescription("Default method. Tracks whatever target the gun was locked on to prior to firing.")
        ACTIVE
    }
    
}
