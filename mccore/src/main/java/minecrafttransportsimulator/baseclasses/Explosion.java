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
 * irregular block destruction, AoE armor penetration, knockback,
 * and lingering potion effects.
 * Replaces vanilla Minecraft explosions when blastDamage is set on a bullet definition.
 *
 * @author dldev32
 */
public class Explosion {
    private static final List<Explosion> lingeringExplosions = new ArrayList<>();
    private static final double DEFAULT_BLOCK_RADIUS_FACTOR = 2.0D;
    private static final double BLOCK_RADIUS_NOISE = 0.10D;
    private static final double BLOCK_STRENGTH_NOISE = 0.12D;
    private static final double BLOCK_NOISE_START = 0.70D;
    private static final double LIQUID_HIT_FACE_EPSILON = 0.05D;
    private static final double MIN_PATH_LENGTH = 1.0E-7D;
    private static final double MIN_DROP_STRENGTH = 4.0D;

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
        this.deathLanguage = entityResponsible != null ? LanguageSystem.DEATH_EXPLOSION_PLAYER : LanguageSystem.DEATH_EXPLOSION_NULL;
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
        //maxStrengthRadius can stand alone as the block damage radius, but blastStrengthRadiusXZ/Y
        //remain the outer falloff radius when they are explicitly set.
        this.blastStrength = bulletDef.blastStrength;
        float resolvedStrengthRadiusXZ = bulletDef.blastStrengthRadiusXZ != 0 ? bulletDef.blastStrengthRadiusXZ : bulletDef.maxStrengthRadius;
        this.blastStrengthRadiusXZ = resolvedStrengthRadiusXZ;
        this.blastStrengthY = bulletDef.blastStrengthY != 0 ? bulletDef.blastStrengthY : resolvedStrengthRadiusXZ;
        this.maxStrengthRadius = bulletDef.maxStrengthRadius;

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
        if (blastStrength > 0 && ConfigSystem.settings.damage.bulletBlockBreaking.value && !isExplosionSubmerged()) {
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
     * Destroys blocks using an irregular ellipsoid.  Entity occlusion still uses raycasts,
     * but block damage is volume-based so high-power blasts do not leave ray-shaped scars.
     */
    private void doBlockDamage() {
        Set<Long> blocksToDestroy = new HashSet<>();
        Point3D blockPos = new Point3D();

        double radiusXZ = blastStrengthRadiusXZ > 0 ? blastStrengthRadiusXZ : blastStrength * DEFAULT_BLOCK_RADIUS_FACTOR;
        double radiusY = blastStrengthY > 0 ? blastStrengthY : radiusXZ;
        double innerRadiusSquared = maxStrengthRadius * maxStrengthRadius;
        double loopRadiusXZ = Math.max(radiusXZ, maxStrengthRadius);
        double loopRadiusY = Math.max(radiusY, maxStrengthRadius);
        int minX = (int) Math.floor(position.x - loopRadiusXZ);
        int maxX = (int) Math.ceil(position.x + loopRadiusXZ);
        int minY = (int) Math.floor(position.y - loopRadiusY);
        int maxY = (int) Math.ceil(position.y + loopRadiusY);
        int minZ = (int) Math.floor(position.z - loopRadiusXZ);
        int maxZ = (int) Math.ceil(position.z + loopRadiusXZ);

        for (int blockX = minX; blockX <= maxX; ++blockX) {
            for (int blockY = minY; blockY <= maxY; ++blockY) {
                for (int blockZ = minZ; blockZ <= maxZ; ++blockZ) {
                    double dx = blockX + 0.5D - position.x;
                    double dy = blockY + 0.5D - position.y;
                    double dz = blockZ + 0.5D - position.z;
                    double distanceSquared = dx * dx + dy * dy + dz * dz;
                    boolean isInnerBlock = maxStrengthRadius > 0 && distanceSquared <= innerRadiusSquared;
                    double normalizedDistance = 0.0D;

                    double edgeNoiseBlend = 0.0D;
                    if (!isInnerBlock) {
                        normalizedDistance = Math.sqrt((dx * dx + dz * dz) / (radiusXZ * radiusXZ) + (dy * dy) / (radiusY * radiusY));
                        edgeNoiseBlend = getEdgeNoiseBlend(normalizedDistance);
                        normalizedDistance += (getBlockNoise(blockX, blockY, blockZ) - 0.5D) * BLOCK_RADIUS_NOISE * edgeNoiseBlend;
                        if (normalizedDistance > 1.0D) {
                            continue;
                        }
                    }

                    blockPos.set(blockX, blockY, blockZ);
                    if (world.isAir(blockPos)) {
                        continue;
                    }

                    float hardness = world.getBlockHardness(blockPos);
                    if (hardness <= 0 || hardness >= Float.MAX_VALUE) {
                        continue;
                    }

                    double localStrength = blastStrength;
                    if (!isInnerBlock) {
                        double falloff = 1.0D - Math.max(normalizedDistance, 0.0D);
                        double strengthNoise = 1.0D + (getBlockNoise(blockX + 37, blockY - 19, blockZ + 53) - 0.5D) * BLOCK_STRENGTH_NOISE * edgeNoiseBlend;
                        localStrength *= falloff * strengthNoise;
                    }

                    if (hardness <= localStrength) {
                        blocksToDestroy.add(packBlockPos(blockX, blockY, blockZ));
                    }
                }
            }
        }
        smoothDestroyedBlocks(blocksToDestroy);

        //Destroy collected blocks with chance-based drops like vanilla MC.
        double dropChance = 1.0D / Math.max(blastStrength, MIN_DROP_STRENGTH);
        for (long packed : blocksToDestroy) {
            blockPos.set(unpackX(packed), unpackY(packed), unpackZ(packed));
            boolean spawnDrops = Math.random() < dropChance;
            world.destroyBlockQuietly(blockPos, spawnDrops);
        }

        //Set fire above destroyed blocks if incendiary.
        if (isIncendiary) {
            for (long packed : blocksToDestroy) {
                blockPos.set(unpackX(packed), unpackY(packed), unpackZ(packed));
                world.setToFire(blockPos, Axis.UP);
            }
        }
    }

    private void smoothDestroyedBlocks(Set<Long> blocksToDestroy) {
        Set<Long> candidateBlocks = new HashSet<>();
        for (long packedPos : blocksToDestroy) {
            int blockX = unpackX(packedPos);
            int blockY = unpackY(packedPos);
            int blockZ = unpackZ(packedPos);
            addSmoothingCandidate(candidateBlocks, blocksToDestroy, blockX - 1, blockY, blockZ);
            addSmoothingCandidate(candidateBlocks, blocksToDestroy, blockX + 1, blockY, blockZ);
            addSmoothingCandidate(candidateBlocks, blocksToDestroy, blockX, blockY - 1, blockZ);
            addSmoothingCandidate(candidateBlocks, blocksToDestroy, blockX, blockY + 1, blockZ);
            addSmoothingCandidate(candidateBlocks, blocksToDestroy, blockX, blockY, blockZ - 1);
            addSmoothingCandidate(candidateBlocks, blocksToDestroy, blockX, blockY, blockZ + 1);
        }

        Set<Long> extraBlocksToDestroy = new HashSet<>();
        Point3D blockPos = new Point3D();
        Point3D neighborPos = new Point3D();
        for (long packedPos : candidateBlocks) {
            int blockX = unpackX(packedPos);
            int blockY = unpackY(packedPos);
            int blockZ = unpackZ(packedPos);

            blockPos.set(blockX, blockY, blockZ);
            if (world.isAir(blockPos)) {
                continue;
            }

            float hardness = world.getBlockHardness(blockPos);
            if (hardness <= 0 || hardness >= Float.MAX_VALUE || hardness > blastStrength) {
                continue;
            }

            int openSides = 0;
            int openHorizontalSides = 0;
            boolean openBelow = isBlockOpen(blockX, blockY - 1, blockZ, blocksToDestroy, neighborPos);
            if (openBelow) {
                ++openSides;
            }
            if (isBlockOpen(blockX, blockY + 1, blockZ, blocksToDestroy, neighborPos)) {
                ++openSides;
            }
            if (isBlockOpen(blockX - 1, blockY, blockZ, blocksToDestroy, neighborPos)) {
                ++openSides;
                ++openHorizontalSides;
            }
            if (isBlockOpen(blockX + 1, blockY, blockZ, blocksToDestroy, neighborPos)) {
                ++openSides;
                ++openHorizontalSides;
            }
            if (isBlockOpen(blockX, blockY, blockZ - 1, blocksToDestroy, neighborPos)) {
                ++openSides;
                ++openHorizontalSides;
            }
            if (isBlockOpen(blockX, blockY, blockZ + 1, blocksToDestroy, neighborPos)) {
                ++openSides;
                ++openHorizontalSides;
            }

            if (openSides >= 5 || (openBelow && openHorizontalSides >= 3)) {
                extraBlocksToDestroy.add(packedPos);
            }
        }
        blocksToDestroy.addAll(extraBlocksToDestroy);
    }

    private static void addSmoothingCandidate(Set<Long> candidateBlocks, Set<Long> blocksToDestroy, int blockX, int blockY, int blockZ) {
        long packedPos = packBlockPos(blockX, blockY, blockZ);
        if (!blocksToDestroy.contains(packedPos)) {
            candidateBlocks.add(packedPos);
        }
    }

    private boolean isBlockOpen(int blockX, int blockY, int blockZ, Set<Long> blocksToDestroy, Point3D blockPos) {
        if (blocksToDestroy.contains(packBlockPos(blockX, blockY, blockZ))) {
            return true;
        }
        blockPos.set(blockX, blockY, blockZ);
        return world.isAir(blockPos);
    }

    private boolean isExplosionSubmerged() {
        int blockX = (int) Math.floor(position.x);
        int blockY = (int) Math.floor(position.y);
        int blockZ = (int) Math.floor(position.z);
        Point3D checkPosition = new Point3D();
        if (isLiquidAt(checkPosition, blockX, blockY, blockZ) || isLiquidAt(checkPosition, blockX, blockY + 1, blockZ)) {
            return true;
        }

        double localX = position.x - blockX;
        double localY = position.y - blockY;
        double localZ = position.z - blockZ;
        if (localY <= LIQUID_HIT_FACE_EPSILON && isLiquidAt(checkPosition, blockX, blockY - 1, blockZ)) {
            return true;
        }
        if (localX <= LIQUID_HIT_FACE_EPSILON && isLiquidAtOrAbove(checkPosition, blockX - 1, blockY, blockZ)) {
            return true;
        }
        if (localX >= 1.0D - LIQUID_HIT_FACE_EPSILON && isLiquidAtOrAbove(checkPosition, blockX + 1, blockY, blockZ)) {
            return true;
        }
        if (localZ <= LIQUID_HIT_FACE_EPSILON && isLiquidAtOrAbove(checkPosition, blockX, blockY, blockZ - 1)) {
            return true;
        }
        return localZ >= 1.0D - LIQUID_HIT_FACE_EPSILON && isLiquidAtOrAbove(checkPosition, blockX, blockY, blockZ + 1);
    }

    private boolean isLiquidAtOrAbove(Point3D checkPosition, int blockX, int blockY, int blockZ) {
        return isLiquidAt(checkPosition, blockX, blockY, blockZ) || isLiquidAt(checkPosition, blockX, blockY + 1, blockZ);
    }

    private boolean isLiquidAt(Point3D checkPosition, int blockX, int blockY, int blockZ) {
        checkPosition.set(blockX, blockY, blockZ);
        return world.isBlockLiquid(checkPosition);
    }

    /**
     * Packs block coordinates into a single long for use in HashSets.
     */
    private static long packBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (long) (z & 0x3FFFFFF);
    }

    private static double getBlockNoise(int x, int y, int z) {
        long hash = x * 3129871L ^ y * 116129781L ^ z * 42317861L;
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >>> 33;
        return (hash & 0xFFFFFFL) / (double) 0xFFFFFFL;
    }

    private static double getEdgeNoiseBlend(double normalizedDistance) {
        if (normalizedDistance <= BLOCK_NOISE_START) {
            return 0.0D;
        }
        double blend = (normalizedDistance - BLOCK_NOISE_START) / (1.0D - BLOCK_NOISE_START);
        if (blend >= 1.0D) {
            return 1.0D;
        }
        return blend * blend * (3.0D - 2.0D * blend);
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
