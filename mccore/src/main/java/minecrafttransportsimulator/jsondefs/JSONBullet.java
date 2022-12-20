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

        @JSONDescription("If true, then this bullet will be considered a HEAT bullet and will use the HEAT armor value on any collision boxes it finds.  If that value isn't defined, it will just use the normal armor value.")
        public boolean isHeat;

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

        @JSONDescription("How much armor this bullet can penetrate, in mm.  This allows the bullet to pass through any collision boxes with armorThickness set less than this value.  Note that as the bullet slows down, this value will decrease, so a bullet with 100 penetration may not pass through a collision box with 90 armor if it slows down enough prior to contact.")
        public float armorPenetration;

        @JSONDescription("If this is a guided bullet, it will detonate this many meters away from its target. For a non-guided bullet, it will detonate when it has a block this many meters or less in front of it. This allows things like missiles and artillery rounds that blow up in the air right above the target or the ground, ensuring that the target receives some of the blast rather than it all going into the ground. If used on a non-explosive bullet, it will not detonate, but will despawn at this distance.")
        public float proximityFuze;

        @JSONDescription("Causes the bullet to explode or despawn after this many ticks. This is a 'dumber' cousin of the proximityFuze, but may be useful for anti-aircraft rounds that explode in mid-air.")
        public int airBurstDelay;

        @JSONDescription("How much velocity, each tick, should be deducted from the bullet's velocity.")
        public float slowdownSpeed;

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

        @JSONDescription("The rate of turn, in degrees per tick, that this bullet will be able to turn to track entities.  If set, then this bullet will lock-on to entities, and hot engines when fired.")
        public float turnRate;

        @JSONDescription("A optional list of effects that this bullet will impart on the entity that it hits.")
        public List<JSONPotionEffect> effects;

        @JSONDescription("Number of pellets that this shell has")
        public int pellets;

        @JSONDescription("How much spread the pellets will have when fired. 0 is no spread, higher values have higher spread.")
        public float pelletSpreadFactor;
    }

    public enum BulletType {
        @JSONDescription("Explodes when it hits something.  Explosion size is based on the bullet's diameter.")
        EXPLOSIVE,
        @JSONDescription("Sets whatever it hits on fire, if it's flammable.  This includes entities.")
        INCENDIARY,
        @JSONDescription("Like incendiary, but puts out fires rather than starts them.")
        WATER,
        @JSONDescription("A bullet that pierces player armor.  Useful for pesky super-suits.")
        ARMOR_PIERCING
    }
    
}
