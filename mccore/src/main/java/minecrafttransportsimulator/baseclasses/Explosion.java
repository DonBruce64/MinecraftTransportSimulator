package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Custom explosion class for bullets.  Provides elliptical blast radii, per-target-type damage,
 * block destruction with ray-casting (like vanilla MC), AoE armor penetration, knockback,
 * and lingering potion effects.
 * Replaces vanilla Minecraft explosions when blastDamage is set on a bullet definition.
 *
 * @author dldev32
 */
public class Explosion {
    private static final List<Explosion> lingeringExplosions = new ArrayList<>();
    private static final double RAY_STEP = 0.3;
    private static final int RAY_GRID = 16;
    private static final double MIN_PATH_LENGTH = 1.0E-7D;

    private final AWrapperWorld world;
    private final Point3D position;
    private final PartGun gun;
    private final JSONBullet.Bullet bulletDef;
    private final IWrapperEntity entityResponsible;
    private final LanguageEntry deathLanguage;
    private final boolean isIncendiary;

    //Entity damage parameters.
    private final float blastDamage;
    private final float blastRadiusXZ;
    private final float blastRadiusY;
    private final float maxDamageRadius;
    private final float blastDamageVsVehicles;
    private final float blastDamageVsAircraft;
    private final float blastDamageVsGround;
    private final float blastDamageVsLiving;

    //Block damage parameters.
    private final float blastStrength;
    private final float blastStrengthRadiusXZ;
    private final float blastStrengthY;
    private final float maxStrengthRadius;

    //Knockback parameter.
    private final float knockback;

    //Potion effect parameters.
    private final List<JSONPotionEffect> effects;
    private int remainingEffectTicks;

    //Armor penetration parameters.
    private final float armorPenetration;
    private final float armorPenRadiusXZ;
    private final float armorPenRadiusY;
    private final float maxPenRadius;

    public Explosion(AWrapperWorld world, Point3D position, PartGun gun) {
        this.world = world;
        this.position = position.copy();
        this.gun = gun;
        this.bulletDef = gun.lastLoadedBullet.definition.bullet;
        this.entityResponsible = gun.lastController;
        this.deathLanguage = entityResponsible != null ? LanguageSystem.DEATH_BULLET_PLAYER : LanguageSystem.DEATH_BULLET_NULL;
        this.isIncendiary = bulletDef.types.contains(BulletType.INCENDIARY);

        //Entity damage - resolve defaults.
        //If only maxDamageRadius is set without radiusXZ, use maxDamageRadius as the outer radius.
        this.maxDamageRadius = bulletDef.maxDamageRadius;
        this.blastDamage = bulletDef.blastDamage;
        float resolvedBlastRadiusXZ = bulletDef.blastRadiusXZ != 0 ? bulletDef.blastRadiusXZ : maxDamageRadius;
        this.blastRadiusXZ = resolvedBlastRadiusXZ;
        this.blastRadiusY = bulletDef.blastRadiusY != 0 ? bulletDef.blastRadiusY : resolvedBlastRadiusXZ;
        this.blastDamageVsVehicles = bulletDef.blastDamageVsVehicles;
        this.blastDamageVsAircraft = bulletDef.blastDamageVsAircraft;
        this.blastDamageVsGround = bulletDef.blastDamageVsGround;
        this.blastDamageVsLiving = bulletDef.blastDamageVsLiving;

        //Block damage - resolve defaults.
        //maxStrengthRadius and blastStrengthRadiusXZ/Y are interchangeable.
        //Either one can stand alone; if both are set, each retains its own role.
        this.blastStrength = bulletDef.blastStrength;
        float resolvedStrengthRadiusXZ = bulletDef.blastStrengthRadiusXZ != 0 ? bulletDef.blastStrengthRadiusXZ : bulletDef.maxStrengthRadius;
        this.blastStrengthRadiusXZ = resolvedStrengthRadiusXZ;
        this.blastStrengthY = bulletDef.blastStrengthY != 0 ? bulletDef.blastStrengthY : resolvedStrengthRadiusXZ;
        this.maxStrengthRadius = bulletDef.maxStrengthRadius != 0 ? bulletDef.maxStrengthRadius : resolvedStrengthRadiusXZ;

        //Knockback.
        this.knockback = bulletDef.knockback;

        //Potion effects.
        this.effects = bulletDef.effects;
        this.remainingEffectTicks = bulletDef.effectDuration;

        //Armor penetration - resolve defaults.
        //If only maxPenRadius is set without radiusXZ, use maxPenRadius as the outer radius.
        this.armorPenetration = bulletDef.armorPenetration;
        this.maxPenRadius = bulletDef.maxPenRadius;
        float resolvedPenRadiusXZ = bulletDef.armorPenRadiusXZ != 0 ? bulletDef.armorPenRadiusXZ : (maxPenRadius != 0 ? maxPenRadius : resolvedBlastRadiusXZ);
        this.armorPenRadiusXZ = resolvedPenRadiusXZ;
        this.armorPenRadiusY = bulletDef.armorPenRadiusY != 0 ? bulletDef.armorPenRadiusY : resolvedPenRadiusXZ;
    }

    /**
     * Executes the explosion: damages entities, destroys blocks, and schedules lingering effects.
     */
    public void doExplosion() {
        if (blastRadiusXZ > 0) {
            doEntityDamage();
        }
        if (blastStrength > 0 && ConfigSystem.settings.damage.bulletBlockBreaking.value) {
            doBlockDamage();
        }
        if (effects != null && !effects.isEmpty() && remainingEffectTicks > 0) {
            lingeringExplosions.add(this);
        }
    }

    /**
     * Damages vehicles and living entities within the blast radius.
     * Applies knockback pushing entities away from the explosion center.
     */
    private void doEntityDamage() {
        BoundingBox blastBounds = new BoundingBox(position, blastRadiusXZ, blastRadiusY, blastRadiusXZ);

        //Damage vehicles.
        List<AEntityF_Multipart<?>> multiparts = new ArrayList<>();
        world.populateWithEntitiesInBounds(multiparts, blastBounds);
        for (AEntityF_Multipart<?> multipart : multiparts) {
            if (multipart instanceof EntityVehicleF_Physics) {
                EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) multipart;
                BlastHit blastHit = getBestVehicleBlastHit(vehicle, blastBounds);
                if (blastHit == null) {
                    continue;
                }

                double dx = blastHit.hitPosition.x - position.x;
                double dy = blastHit.hitPosition.y - position.y;
                double dz = blastHit.hitPosition.z - position.z;

                //Select base damage based on vehicle type.
                float baseDamage;
                if (vehicle.definition.motorized.isAircraft) {
                    baseDamage = blastDamageVsAircraft != 0 ? blastDamageVsAircraft : (blastDamageVsVehicles != 0 ? blastDamageVsVehicles : blastDamage);
                } else {
                    baseDamage = blastDamageVsGround != 0 ? blastDamageVsGround : (blastDamageVsVehicles != 0 ? blastDamageVsVehicles : blastDamage);
                }

                double damageAmount = baseDamage * blastHit.falloff;
                Damage damage = new Damage(damageAmount, blastHit.box, gun, entityResponsible, deathLanguage).setExplosive().ignoreCooldown().bypassIgnoredExplosiveDamage();

                //Apply knockback away from explosion center.
                if (knockback != 0) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > 0) {
                        damage.knockback = new Point3D(dx / dist * knockback * blastHit.falloff, dy / dist * knockback * blastHit.falloff, dz / dist * knockback * blastHit.falloff);
                    }
                }

                //Check armor penetration.
                if (armorPenetration > 0 && armorPenRadiusXZ > 0) {
                    double penFalloff = getEllipticalFalloff(dx, dy, dz, armorPenRadiusXZ, armorPenRadiusY, maxPenRadius);
                    if (penFalloff > 0) {
                        damage.ignoreArmor = true;
                    }
                }

                //Apply effects.
                if (effects != null && !effects.isEmpty()) {
                    damage.effects = effects;
                }

                if (isIncendiary) {
                    damage.isFire = true;
                }

                vehicle.attack(damage);
            }
        }

        //Damage living entities (mobs/players).
        List<IWrapperEntity> entities = world.getEntitiesWithin(blastBounds);
        for (IWrapperEntity entity : entities) {
            Point3D entityPos = entity.getPosition();
            Point3D entityBlastPoint = entityPos.copy().add(0, entity.getEyeHeight(), 0);
            if (!isBlastPathClear(entityBlastPoint)) {
                continue;
            }

            double dx = entityPos.x - position.x;
            double dy = entityPos.y - position.y;
            double dz = entityPos.z - position.z;

            double damageFalloff = getEllipticalFalloff(dx, dy, dz, blastRadiusXZ, blastRadiusY, maxDamageRadius);
            if (damageFalloff <= 0) {
                continue;
            }

            float baseDamage = blastDamageVsLiving != 0 ? blastDamageVsLiving : blastDamage;
            double damageAmount = baseDamage * damageFalloff;
            Damage damage = new Damage(damageAmount, blastBounds, gun, entityResponsible, deathLanguage).setExplosive().ignoreCooldown();

            //Apply MC-style knockback: use entity eye height for the Y delta, creating natural upward arcs.
            //In vanilla MC, the vector goes from explosion center to the entity's eye position,
            //so ground-level explosions naturally launch entities up and away.
            if (knockback != 0) {
                double eyeDy = (entityPos.y + entity.getEyeHeight()) - position.y;
                double dist = Math.sqrt(dx * dx + eyeDy * eyeDy + dz * dz);
                if (dist > 0) {
                    double strength = damageFalloff * knockback;
                    damage.knockback = new Point3D(dx / dist * strength, eyeDy / dist * strength, dz / dist * strength);
                }
            }

            //Check armor penetration.
            if (armorPenetration > 0 && armorPenRadiusXZ > 0) {
                double penFalloff = getEllipticalFalloff(dx, dy, dz, armorPenRadiusXZ, armorPenRadiusY, maxPenRadius);
                if (penFalloff > 0) {
                    damage.ignoreArmor = true;
                }
            }

            //Apply effects.
            if (effects != null && !effects.isEmpty()) {
                damage.effects = effects;
            }

            if (isIncendiary) {
                damage.isFire = true;
            }

            entity.attack(damage);
        }
    }

    /**
     * Destroys blocks using ray-casting from the explosion center, matching vanilla MC's algorithm.
     * Rays are cast in all directions on a 16x16x16 grid.  Each ray starts with randomized strength
     * (blastStrength * (0.7 + random * 0.6)) and loses energy as it passes through blocks based on
     * their hardness: (hardness + 0.3) * 0.3 per step.  This creates natural-looking craters with
     * ragged edges.  Directly exposed blocks within maxStrengthRadius are always destroyed
     * regardless of ray energy (but rays still lose energy normally for blocks beyond that zone).
     * blastStrengthRadiusXZ/Y optionally cap maximum ray travel distance.
     * If blastStrengthRadiusXZ is 0, rays run until energy is depleted (like vanilla MC).
     * Block drops use 1/blastStrength probability like vanilla MC.
     */
    private void doBlockDamage() {
        Set<Long> blocksToDestroy = new HashSet<>();
        Set<Long> blocksToIgnite = new HashSet<>();
        Point3D rayPos = new Point3D();
        Point3D blockPos = new Point3D();

        //First pass: mark exposed blocks within maxStrengthRadius for guaranteed destruction.
        if (maxStrengthRadius > 0) {
            int maxR = (int) Math.ceil(maxStrengthRadius);
            for (int x = -maxR; x <= maxR; x++) {
                for (int y = -maxR; y <= maxR; y++) {
                    for (int z = -maxR; z <= maxR; z++) {
                        if (Math.sqrt(x * x + y * y + z * z) <= maxStrengthRadius) {
                            int bx = (int) Math.floor(position.x) + x;
                            int by = (int) Math.floor(position.y) + y;
                            int bz = (int) Math.floor(position.z) + z;
                            blockPos.set(bx, by, bz);
                            float hardness = world.getBlockHardness(blockPos);
                            if (hardness > 0 && hardness <= blastStrength && hardness < Float.MAX_VALUE && isBlastPathClearToBlock(blockPos, bx, by, bz)) {
                                blocksToDestroy.add(packBlockPos(bx, by, bz));
                            }
                        }
                    }
                }
            }
        }

        //Second pass: ray-cast for natural MC-style crater edges.
        for (int xi = 0; xi < RAY_GRID; xi++) {
            for (int yi = 0; yi < RAY_GRID; yi++) {
                for (int zi = 0; zi < RAY_GRID; zi++) {
                    //Only cast rays from the surface of the grid cube (like vanilla MC).
                    if (xi != 0 && xi != RAY_GRID - 1 && yi != 0 && yi != RAY_GRID - 1 && zi != 0 && zi != RAY_GRID - 1) {
                        continue;
                    }

                    //Calculate ray direction, normalized.
                    double dirX = (double) xi / (RAY_GRID - 1) * 2.0 - 1.0;
                    double dirY = (double) yi / (RAY_GRID - 1) * 2.0 - 1.0;
                    double dirZ = (double) zi / (RAY_GRID - 1) * 2.0 - 1.0;
                    double dirLen = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                    if (dirLen == 0) {
                        continue;
                    }
                    dirX /= dirLen;
                    dirY /= dirLen;
                    dirZ /= dirLen;

                    //Scale direction by step size.
                    double stepX = dirX * RAY_STEP;
                    double stepY = dirY * RAY_STEP;
                    double stepZ = dirZ * RAY_STEP;

                    //Initialize ray strength with randomization like vanilla MC.
                    double rayStrength = blastStrength * (0.7 + Math.random() * 0.6);

                    //Step the ray through blocks until energy is depleted.
                    rayPos.set(position.x, position.y, position.z);
                    while (rayStrength > 0) {
                        int bx = (int) Math.floor(rayPos.x);
                        int by = (int) Math.floor(rayPos.y);
                        int bz = (int) Math.floor(rayPos.z);

                        //Check if ray has exceeded the optional elliptical radius cap.
                        if (blastStrengthRadiusXZ > 0) {
                            double dx = rayPos.x - position.x;
                            double dy = rayPos.y - position.y;
                            double dz = rayPos.z - position.z;
                            double effectiveRadiusY = blastStrengthY > 0 ? blastStrengthY : blastStrengthRadiusXZ;
                            double normDist = Math.sqrt((dx * dx + dz * dz) / (blastStrengthRadiusXZ * blastStrengthRadiusXZ) + (dy * dy) / (effectiveRadiusY * effectiveRadiusY));
                            if (normDist > 1.0) {
                                break;
                            }
                        }

                        blockPos.set(bx, by, bz);
                        if (!world.isAir(blockPos)) {
                            float hardness = world.getBlockHardness(blockPos);

                            //Reduce ray strength by block resistance like vanilla MC.
                            //Rays always lose energy from blocks, even in maxStrengthRadius.
                            rayStrength -= (hardness + 0.3) * RAY_STEP;

                            if (rayStrength > 0 && hardness < Float.MAX_VALUE) {
                                blocksToDestroy.add(packBlockPos(bx, by, bz));
                            }
                        }

                        //Mark air blocks for fire if incendiary.
                        if (isIncendiary && world.isAir(blockPos)) {
                            blocksToIgnite.add(packBlockPos(bx, by, bz));
                        }

                        rayPos.x += stepX;
                        rayPos.y += stepY;
                        rayPos.z += stepZ;
                    }
                }
            }
        }

        //Destroy collected blocks with chance-based drops like vanilla MC (1/blastStrength probability).
        for (long packed : blocksToDestroy) {
            blockPos.set(unpackX(packed), unpackY(packed), unpackZ(packed));
            boolean spawnDrops = Math.random() < 1.0 / blastStrength;
            world.destroyBlock(blockPos, spawnDrops);
        }

        //Set fire to air blocks adjacent to destroyed blocks if incendiary.
        if (isIncendiary) {
            for (long packed : blocksToIgnite) {
                if (!blocksToDestroy.contains(packed)) {
                    blockPos.set(unpackX(packed), unpackY(packed), unpackZ(packed));
                    world.setToFire(blockPos, Axis.UP);
                }
            }
        }
    }

    /**
     * Packs block coordinates into a single long for use in HashSets.
     */
    private static long packBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (long) (z & 0x3FFFFFF);
    }

    private static int unpackX(long packed) {
        int x = (int) (packed >> 38);
        return (x & 0x2000000) != 0 ? x | 0xFC000000 : x;
    }

    private static int unpackY(long packed) {
        int y = (int) ((packed >> 26) & 0xFFF);
        return (y & 0x800) != 0 ? y | 0xFFFFF000 : y;
    }

    private static int unpackZ(long packed) {
        int z = (int) (packed & 0x3FFFFFF);
        return (z & 0x2000000) != 0 ? z | 0xFC000000 : z;
    }

    /**
     * Applies lingering potion effects to entities within the blast radius.
     * Called each tick from the static tick method.
     */
    private void applyLingeringEffects() {
        if (effects == null || effects.isEmpty() || blastRadiusXZ <= 0) {
            return;
        }

        BoundingBox blastBounds = new BoundingBox(position, blastRadiusXZ, blastRadiusY, blastRadiusXZ);

        //Apply to living entities.
        List<IWrapperEntity> entities = world.getEntitiesWithin(blastBounds);
        for (IWrapperEntity entity : entities) {
            Point3D entityPos = entity.getPosition();
            Point3D entityBlastPoint = entityPos.copy().add(0, entity.getEyeHeight(), 0);
            if (!isBlastPathClear(entityBlastPoint)) {
                continue;
            }

            double dx = entityPos.x - position.x;
            double dy = entityPos.y - position.y;
            double dz = entityPos.z - position.z;

            double falloff = getEllipticalFalloff(dx, dy, dz, blastRadiusXZ, blastRadiusY, maxDamageRadius);
            if (falloff > 0) {
                Damage effectDamage = new Damage(0, blastBounds, gun, entityResponsible, deathLanguage).setExplosive().ignoreCooldown();
                effectDamage.effects = effects;
                entity.attack(effectDamage);
            }
        }
    }

    /**
     * Calculates the elliptical falloff factor for a point at (dx, dy, dz) from the explosion center.
     * Uses elliptical normalized distance (0 at center, 1 at edge of radiusXZ/Y ellipsoid).
     * maxRadius is checked using actual 3D distance so it works correctly regardless of direction.
     * Returns 1.0 inside maxRadius, linear falloff to 0.0 at the edge, and 0.0 outside the ellipsoid.
     */
    private static double getEllipticalFalloff(double dx, double dy, double dz, double radiusXZ, double radiusY, double maxRadius) {
        //If maxRadius is set, first check actual 3D distance for full-value zone.
        if (maxRadius > 0) {
            double actualDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (actualDist <= maxRadius) {
                return 1.0;
            }
        }

        if (radiusXZ <= 0) {
            return 0;
        }
        if (radiusY <= 0) {
            radiusY = radiusXZ;
        }

        double normDist = Math.sqrt((dx * dx + dz * dz) / (radiusXZ * radiusXZ) + (dy * dy) / (radiusY * radiusY));

        if (normDist > 1.0) {
            return 0.0;
        }
        if (normDist <= 0) {
            return 1.0;
        }

        return 1.0 - normDist;
    }

    /**
     * Returns the best exposed attack box on a vehicle for this explosion, or null if the vehicle
     * is outside the damage ellipsoid or fully blocked by terrain.
     */
    private BlastHit getBestVehicleBlastHit(EntityVehicleF_Physics vehicle, BoundingBox blastBounds) {
        BlastHit bestHit = null;
        Point3D testPoint = new Point3D();
        for (BoundingBox box : vehicle.allCollisionBoxes) {
            if (box.collisionTypes != null && box.collisionTypes.contains(CollisionType.ATTACK) && box.intersects(blastBounds)) {
                getClosestPointOnBox(box, position, testPoint);
                double dx = testPoint.x - position.x;
                double dy = testPoint.y - position.y;
                double dz = testPoint.z - position.z;
                double falloff = getEllipticalFalloff(dx, dy, dz, blastRadiusXZ, blastRadiusY, maxDamageRadius);
                if (falloff > 0 && (bestHit == null || falloff > bestHit.falloff) && isBlastPathClear(testPoint)) {
                    if (bestHit == null) {
                        bestHit = new BlastHit();
                    }
                    bestHit.box = box;
                    bestHit.hitPosition.set(testPoint);
                    bestHit.falloff = falloff;
                }
            }
        }
        return bestHit;
    }

    /**
     * Gets the point on a bounding box closest to the explosion origin.  This makes blast radius
     * apply to large vehicles by their hull hitboxes rather than only by their entity center.
     */
    private static void getClosestPointOnBox(BoundingBox box, Point3D point, Point3D output) {
        output.x = Math.max(box.globalCenter.x - box.widthRadius, Math.min(point.x, box.globalCenter.x + box.widthRadius));
        output.y = Math.max(box.globalCenter.y - box.heightRadius, Math.min(point.y, box.globalCenter.y + box.heightRadius));
        output.z = Math.max(box.globalCenter.z - box.depthRadius, Math.min(point.z, box.globalCenter.z + box.depthRadius));
    }

    /**
     * Checks if terrain blocks the blast from reaching the target point.
     */
    private boolean isBlastPathClear(Point3D targetPosition) {
        Point3D delta = targetPosition.copy().subtract(position);
        return delta.length() < MIN_PATH_LENGTH || world.getBlockHit(position, delta) == null;
    }

    /**
     * Checks if terrain blocks the blast from reaching a block.  The target block itself is allowed
     * to be the first ray hit; any nearer block means this block is shielded.
     */
    private boolean isBlastPathClearToBlock(Point3D blockPosition, int blockX, int blockY, int blockZ) {
        Point3D targetPosition = blockPosition.copy().add(0.5, 0.5, 0.5);
        Point3D delta = targetPosition.subtract(position);
        if (delta.length() < MIN_PATH_LENGTH) {
            return true;
        }
        BlockHitResult hit = world.getBlockHit(position, delta);
        return hit == null || ((int) hit.blockPosition.x == blockX && (int) hit.blockPosition.y == blockY && (int) hit.blockPosition.z == blockZ);
    }

    private static class BlastHit {
        private BoundingBox box;
        private final Point3D hitPosition = new Point3D();
        private double falloff;
    }

    /**
     * Ticks all lingering explosions for the given world.  Should be called once per server tick.
     * Applies potion effects to entities within the blast area and removes expired explosions.
     */
    public static void tickLingeringExplosions(AWrapperWorld world) {
        Iterator<Explosion> it = lingeringExplosions.iterator();
        while (it.hasNext()) {
            Explosion explosion = it.next();
            if (explosion.world == world) {
                explosion.applyLingeringEffects();
                if (--explosion.remainingEffectTicks <= 0) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Clears all lingering explosions for the given world.  Should be called when a world is unloaded.
     */
    public static void clearLingeringExplosions(AWrapperWorld world) {
        lingeringExplosions.removeIf(explosion -> explosion.world == world);
    }
}
