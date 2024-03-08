package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.AWrapperWorld.BlockHitResult;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.systems.LanguageSystem;

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
    private static final List<Explosion> activeExplosions = new ArrayList<>();

    private final AWrapperWorld world;
    //base explosion params
    private final Point3D position;
    //how strong of blocks it can break
    private double strength;
    //how much health it deals at the source
    private final double damage;
    //Out to this radius from boom, full damage is dealt. damage decays after.
    private final double damageDecayStartRadius;
    //No damage dealt outside this radius
    private final double damageDecayEndRadius;
    //Like the one for damage, but for block breaking strength
    private final double strengthDecayStartRadius;
    //No damage dealt outside this radius
    private final double strengthDecayEndRadius;
    //Do it cause fire? Follow decay curve of strength, not damage.
    private final boolean isFlammable;
    private final JSONBullet bullet;
    private final List<Point3D> positionsWithBlocksToBreak = new ArrayList<>();

    private final LanguageSystem.LanguageEntry language;

    private final IWrapperEntity entityResponsible;

    /**Constructor for default explosions without any JSON.  Auto-calculates start/end radius from strength.**/
    public Explosion(AWrapperWorld world, Point3D position, double strength, double damage,boolean isFlammable) {
        this.world = world;
        this.position = position;
        this.strength = strength;
        this.damage = damage;
        this.damageDecayStartRadius = damage * Math.log(damage + 1) * 0.1;
        this.damageDecayEndRadius = damage * Math.log(damage + 1) * 0.25;
        this.isFlammable = isFlammable;
        this.bullet = null;
        strengthDecayStartRadius = 0;
        strengthDecayEndRadius = damageDecayEndRadius;
        language = LanguageSystem.DEATH_EXPLOSION_NULL;
        entityResponsible = null;
    }

    /**Constructor for explosions from bullets.  Uses bullet properties for radii.**/
    public Explosion(AWrapperWorld world, Point3D position, JSONBullet bullet, IWrapperEntity entityResponsible) {
        this.world = world;
        this.position = position;
        this.strength = bullet.bullet.blastStrength;
        this.damage = bullet.bullet.blastDamage;
        //FIXME make properties.
        this.isFlammable = bullet.bullet.types.contains(BulletType.INCENDIARY);
        this.bullet = bullet;
        damageDecayStartRadius = bullet.bullet.blastDamageRadiusDecay;
        damageDecayEndRadius = bullet.bullet.blastDamageRadiusMax;
        strengthDecayStartRadius = bullet.bullet.blaskStrengthRadiusDecay;
        strengthDecayEndRadius = bullet.bullet.blastStrengthRadiusMax;
        language = LanguageSystem.DEATH_EXPLOSION_PLAYER;
        this.entityResponsible = entityResponsible;
    }

    //gets damage dealt to vehicles, parts, anything in IV that can be hurt.
    public void attackInternalEntity() {
        //Check if we are an actual explosion.  If we have a strength of 0, we just want to spawn sounds/particles and not do logic.
        for (AEntityE_Interactable<?> entity : world.getEntitiesExtendingType(AEntityE_Interactable.class)) {
            System.out.println(entity);
            if (entity.position.isDistanceToCloserThan(position, damageDecayEndRadius)) {
                double factor;
                if (entity.position.isDistanceToCloserThan(position, damageDecayStartRadius)) {
                    factor = damage;
                } else {
                    factor = damage * Math.pow(0.5, 3 * (entity.position.distanceTo(position) - damageDecayStartRadius) / (damageDecayEndRadius - damageDecayStartRadius));
                }
                Damage damage = new Damage(factor, null, null, entityResponsible, language).setExplosive();
                if (isFlammable) {
                    damage.setFire();
                }
                //TODO: make configs to separate damage to entity types. e.i. DMGvPlanes, DMGvGRND, etc.

                entity.attack(damage);
                System.out.println("ATTACKED " + damage.amount);
            }
        }
    }

    //Attack any entity that isnt an MTS Entity
    public void attackExternalEntity() {
        //Check if we are an actual explosion.  If we have a strength of 0, we just want to spawn sounds/particles and not do logic.
        for (IWrapperEntity entityE : world.getEntitiesWithin(new BoundingBox(position,damageDecayEndRadius,damageDecayEndRadius,damageDecayEndRadius))) {
            System.out.println(entityE);
            if (entityE.getPosition().isDistanceToCloserThan(position, damageDecayEndRadius)) {
                double factor;
                if (entityE.getPosition().isDistanceToCloserThan(position, damageDecayStartRadius)) {
                    factor = damage;
                } else {
                    factor = damage * Math.pow(0.5, 3 * (entityE.getPosition().distanceTo(position) - damageDecayStartRadius) / (damageDecayEndRadius - damageDecayStartRadius));
                }
                Damage externalDamage = new Damage(factor, null, null, entityResponsible, language).setExplosive();
                if (isFlammable) {
                    externalDamage.setFire();
                }
                entityE.attack(externalDamage);
                System.out.println("ATTACKED " + externalDamage.amount);
            }
        }
    }

    //actually break blocks
    public void breakBlocks() {
        //TODO These variables should already be present somewhere, might be named differently.
        Point3D tempPosition = new Point3D();

        //TODO consoildate this.  We don't need these variables, they're only here for clarity for you.
        double blastReductionBreakFactor = 0.3F;
        double blastReductionSolidFactor = 0.5F;

        //Generate a list of points that are valid explosion points.  This only checks if the block isn't air and is close enough to hit.
        for (int x = (int) (position.x - strengthDecayEndRadius); x < position.x + strengthDecayEndRadius; ++x) {
            for (int y = (int) (position.y - strengthDecayEndRadius); y < position.y + strengthDecayEndRadius; ++y) {
                for (int z = (int) (position.z - strengthDecayEndRadius); z < position.z + strengthDecayEndRadius; ++z) {
                    tempPosition.set(x,y,z);
                    if (!world.isAir(tempPosition)) {
                        positionsWithBlocksToBreak.add(tempPosition.copy());
                    }
                }
            }
        }

        //Sort points by distance for later checks.
        if (!positionsWithBlocksToBreak.isEmpty()) {
            positionsWithBlocksToBreak.sort(new Comparator<Point3D>() {
                @Override
                public int compare(Point3D position1, Point3D position2) {
                    double distance1 = position.distanceTo(position1);
                    double distance2 = position.distanceTo(position2);
                    if (distance1 < distance2) {
                        return -1;
                    } else if (distance2 < distance1) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });

            //Now that we have all positions, do checks.  This way we do outward checking VS box-cube checking.
            Iterator<Point3D> iterator = positionsWithBlocksToBreak.iterator();
            while (iterator.hasNext()) {
                Point3D blockPosition = iterator.next();
                double factor = 0;
                if (blockPosition.isDistanceToCloserThan(position, strengthDecayEndRadius)) {
                    if (blockPosition.isDistanceToCloserThan(position, strengthDecayStartRadius)) {
                        factor = strength;
                    } else {
                        factor = strength * Math.pow(0.5, 3 * (blockPosition.distanceTo(position) - strengthDecayStartRadius) / (strengthDecayEndRadius - strengthDecayStartRadius));
                    }
                }
                double blastResistance = world.getBlockBlastResistance(blockPosition);
                if (factor >= blastResistance) {
                    strength -= factor * blastReductionBreakFactor;
                } else {
                    //Can't break this block.
                    strength -= factor * blastReductionSolidFactor;
                    iterator.remove();
                }
            }
        }

        //If we still have blocks, set us to be ticked.
        if (!positionsWithBlocksToBreak.isEmpty()) {
            activeExplosions.add(this);
        }
    }

    //Breaks some blocks, returns true if all blocks were broken.
    private boolean doBreakingTick() {
        //TODO consoildate this.  We don't need these variables, they're only here for clarity for you.
        int blocksToBreakPertick = 5;
        int maxBlocksToDrop = 30;
        float maxDropRate = 0.25F;
        float fireRate = 0.10F;

        //Need to cast to float here, otherwise we do integer division.
        float dropRate = maxBlocksToDrop / (float) positionsWithBlocksToBreak.size();

        //Don't want to drop all 30 blocks if we only broke 30, need to clamp to max rate.
        if (dropRate > maxDropRate) {
            dropRate = maxDropRate;
        }

        for (int i = 0; i < blocksToBreakPertick; ++i) {
            //Remove operation gets the first element in the list and removes it.
            //We just want a single block to break each go-around of this loop, until
            //we hit the max blocks, or until they're empty.
            Point3D blockPosition = positionsWithBlocksToBreak.remove(0);
            world.destroyBlock(blockPosition, Math.random() < dropRate, true);
            if (isFlammable && Math.random() < fireRate) {
                --blockPosition.y;
                if (!world.isAir(blockPosition)) {
                    world.setToFire(new BlockHitResult(blockPosition, Axis.UP));
                }
            }
            if (positionsWithBlocksToBreak.isEmpty()) {
                return true;
            }
        }
        //Didn't break all blocks, return false.
        return false;
    }

    //Ticks explosions each tick.  Called from EntityManager each tick.
    //This spreads out block breaking across multiple ticks to reduce lag.
    public static void tickActiveExplosions() {
        activeExplosions.removeIf(explosion -> explosion.doBreakingTick());
    }
}