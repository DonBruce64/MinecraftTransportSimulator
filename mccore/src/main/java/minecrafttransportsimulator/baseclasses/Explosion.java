package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;

/**
 * Basic explosion class.  Used to make instances of explosions to apply in the world.
 * Contains parameters for the position, strength, and two radiuses.  The first is how
 * far out the explosion should go before it starts reducing in strength.  The second
 * is how far out it should have 0 strength.  Interpolation being linear between to two spherical areas.
 * <br><br>
 * This class also includes a JSON reference to allow a bullet reference, as bullets require custom supplemental
 * explosive properties, such as modifiers for blocks, potions for AOE effects, etc.
 *
 * @author don_bruce
 */
public class Explosion {
    public final Point3D position;
    public final double strength;
    public final double reductionStartRadius;
    public final double reductionEndRadius;
    public final boolean isFlamable;
    public final JSONBullet bullet;

    /**Constructor for default explosions without any JSON.  Auto-calculates start/end radius from strength.**/
    public Explosion(Point3D position, double strength, boolean isFlamable) {
        this.position = position;
        this.strength = strength;
        this.reductionStartRadius = 0;
        this.reductionEndRadius = strength;
        this.isFlamable = isFlamable;
        this.bullet = null;
    }

    /**Constructor for explosions from bullets.  Uses bullet properties for radiuses.**/
    public Explosion(Point3D position, double strength, JSONBullet bullet) {
        this.position = position;
        this.strength = strength;
        //FIXME make properties.
        this.reductionStartRadius = 0;
        this.reductionEndRadius = strength;
        this.isFlamable = bullet.bullet.types.contains(BulletType.INCENDIARY);
        this.bullet = bullet;
    }
}
