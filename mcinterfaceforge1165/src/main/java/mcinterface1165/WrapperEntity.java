package mcinterface1165;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LeadItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class WrapperEntity implements IWrapperEntity {
    private static final Map<Entity, WrapperEntity> entityWrappers = new HashMap<>();

    protected final Entity entity;
    private AEntityB_Existing cachedEntityRiding;

    /**
     * Returns a wrapper instance for the passed-in entity instance.
     * Null may be passed-in safely to ease function-forwarding.
     * Wrapper is cached to avoid re-creating the wrapper each time it is requested.
     * If the entity is a player, then a player wrapper is returned.
     */
    public static WrapperEntity getWrapperFor(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return WrapperPlayer.getWrapperFor((PlayerEntity) entity);
        } else if (entity != null) {
            WrapperEntity wrapper = entityWrappers.get(entity);
            if (wrapper == null || !wrapper.isValid() || entity != wrapper.entity) {
                wrapper = new WrapperEntity(entity);
                entityWrappers.put(entity, wrapper);
            }
            return wrapper;
        } else {
            return null;
        }
    }

    protected WrapperEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public boolean equals(Object obj) {
        return entity.equals(obj instanceof WrapperEntity ? ((WrapperEntity) obj).entity : obj);
    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }

    @Override
    public boolean isValid() {
        return entity != null && entity.isAlive() && (!(entity instanceof LivingEntity) || ((LivingEntity) entity).deathTime == 0);
    }

    @Override
    public UUID getID() {
        return entity.getUUID();
    }

    @Override
    public String getName() {
        return entity.getName().getString();
    }

    @Override
    public AWrapperWorld getWorld() {
        return WrapperWorld.getWrapperFor(entity.level);
    }

    @Override
    public AEntityB_Existing getEntityRiding() {
        if (cachedEntityRiding != null) {
            return cachedEntityRiding;
        } else {
            Entity mcEntityRiding = entity.getVehicle();
            if (mcEntityRiding instanceof BuilderEntityLinkedSeat) {
                AEntityB_Existing entityRiding = ((BuilderEntityLinkedSeat) mcEntityRiding).entity;
                //Need to check this as MC might have us as a rider on the builer, but we might not be a rider on the entity.
                if (entityRiding != null && this.equals(entityRiding.rider)) {
                    return entityRiding;
                }
            }
            return null;
        }
    }

    @Override
    public void setRiding(AEntityB_Existing entityToRide) {
        if (entityToRide != null) {
            //Don't re-add a seat entity if we are just changing seats.
            //This just causes extra execution logic.
            AEntityB_Existing entityRiding = getEntityRiding();
            if (entityRiding == null) {
                //Only spawn and start riding on the server, clients will get packets.
                if (!entity.level.isClientSide) {
                    BuilderEntityLinkedSeat seat = new BuilderEntityLinkedSeat(BuilderEntityLinkedSeat.E_TYPE3, ((WrapperWorld) entityToRide.world).world);
                    seat.loadedFromSavedNBT = true;
                    seat.setPos(entityToRide.position.x, entityToRide.position.y, entityToRide.position.z);
                    seat.entity = entityToRide;
                    entity.level.addFreshEntity(seat);
                    entity.startRiding(seat, true);
                }
            } else {
                //Just change entity reference, we will already be a rider on the entity at this point.
                ((BuilderEntityLinkedSeat) entity.getVehicle()).entity = entityToRide;
            }
            cachedEntityRiding = entityToRide;
        } else {
            entity.stopRiding();
            cachedEntityRiding = null;
        }
    }

    @Override
    public double getVerticalScale() {
        AEntityB_Existing riding = getEntityRiding();
        if (riding instanceof PartSeat) {
            PartSeat seat = (PartSeat) riding;
            if (seat != null) {
                if (seat.placementDefinition.playerScale != null) {
                    if (seat.definition.seat.playerScale != null) {
                        return seat.scale.y * seat.placementDefinition.playerScale.y * seat.definition.seat.playerScale.y;
                    } else {
                        return seat.scale.y * seat.placementDefinition.playerScale.y;
                    }
                } else if (seat.definition.seat.playerScale != null) {
                    return seat.scale.y * seat.definition.seat.playerScale.y;
                } else {
                    return seat.scale.y;
                }
            }
        }
        return 1.0;
    }

    @Override
    public double getSeatOffset() {
        return 0D;
    }

    @Override
    public double getEyeHeight() {
        return entity.getEyeHeight();
    }

    @Override
    public Point3D getPosition() {
        mutablePosition.set(entity.getX(), entity.getY(), entity.getZ());
        return mutablePosition;
    }

    private final Point3D mutablePosition = new Point3D();

    @Override
    public void setPosition(Point3D position, boolean onGround) {
        entity.setPos(position.x, position.y, position.z);
        //Set fallDistance to 0 to prevent damage.
        entity.fallDistance = 0;
        entity.setOnGround(onGround);
    }

    @Override
    public Point3D getVelocity() {
        //Need to manually put 0 here for Y since entities on ground have a constant -Y motion.
        mutableVelocity.set(entity.getDeltaMovement().x, entity.isOnGround() ? 0 : entity.getDeltaMovement().y, entity.getDeltaMovement().z);
        return mutableVelocity;
    }

    private final Point3D mutableVelocity = new Point3D();

    @Override
    public void setVelocity(Point3D motion) {
        entity.setDeltaMovement(motion.x, motion.y, motion.z);
    }

    @Override
    public RotationMatrix getOrientation() {
        if (lastPitchChecked != entity.xRot || lastYawChecked != entity.yRot) {
            lastPitchChecked = entity.xRot;
            lastYawChecked = entity.yRot;
            mutableOrientation.angles.set(entity.xRot, -entity.yRot, 0);
            mutableOrientation.setToAngles(mutableOrientation.angles);
        }
        return mutableOrientation;
    }

    private final RotationMatrix mutableOrientation = new RotationMatrix();
    private float lastPitchChecked;
    private float lastYawChecked;

    @Override
    public void setOrientation(RotationMatrix rotation) {
        entity.yRot = (float) -rotation.angles.y;
        entity.xRot = (float) rotation.angles.x;
    }

    @Override
    public float getPitch() {
        return entity.xRot;
    }

    @Override
    public float getPitchDelta() {
        float value = entity.xRot - lastPitch;
        lastPitch = entity.xRot;
        return value;
    }

    private float lastPitch;

    @Override
    public float getYaw() {
        return -entity.yRot;
    }

    @Override
    public float getYawDelta() {
        float value = entity.yRot - lastYaw;
        lastYaw = entity.yRot;
        return -value;
    }

    private float lastYaw;

    @Override
    public float getBodyYaw() {
        return entity instanceof LivingEntity ? -((LivingEntity) entity).yBodyRot : 0;
    }

    @Override
    public Point3D getLineOfSight(double distance) {
        mutableSight.set(0, 0, distance).rotate(getOrientation());
        return mutableSight;
    }

    private final Point3D mutableSight = new Point3D();

    @Override
    public void setYaw(double yaw) {
        entity.yRot = (float) -yaw;
    }

    @Override
    public void setBodyYaw(double yaw) {
        if (entity instanceof LivingEntity) {
            entity.setYBodyRot((float) -yaw);
        }
    }

    @Override
    public void setPitch(double pitch) {
        entity.xRot = (float) pitch;
    }

    @Override
    public BoundingBox getBounds() {
        mutableBounds.widthRadius = entity.getBbWidth() / 2F;
        mutableBounds.heightRadius = entity.getBbHeight() / 2F;
        mutableBounds.depthRadius = entity.getBbWidth() / 2F;
        mutableBounds.globalCenter.set(entity.getX(), entity.getY() + mutableBounds.heightRadius, entity.getZ());
        return mutableBounds;
    }

    private final BoundingBox mutableBounds = new BoundingBox(new Point3D(), 0, 0, 0);

    @Override
    public IWrapperNBT getData() {
        CompoundNBT tag = new CompoundNBT();
        entity.save(tag);
        return new WrapperNBT(tag);
    }

    @Override
    public void setData(IWrapperNBT data) {
        entity.load(((WrapperNBT) data).tag);
    }

    @Override
    public boolean leashTo(IWrapperPlayer player) {
        PlayerEntity mcPlayer = ((WrapperPlayer) player).player;
        if (entity instanceof MobEntity) {
            ItemStack heldStack = mcPlayer.getMainHandItem();
            if (((MobEntity) entity).canBeLeashed(mcPlayer) && heldStack.getItem() instanceof LeadItem) {
                ((MobEntity) entity).setLeashedTo(mcPlayer, true);
                if (!mcPlayer.isCreative()) {
                    heldStack.shrink(1);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void attack(Damage damage) {
        if (damage.language == null) {
            throw new IllegalArgumentException("ERROR: Cannot attack an entity with a damage of no type and language component!");
        }
        DamageSource newSource = new DamageSource(damage.language.value) {
            @Override
            public ITextComponent getLocalizedDeathMessage(LivingEntity player) {
                if (damage.entityResponsible != null) {
                    return new StringTextComponent(String.format(damage.language.value, player.getDisplayName().getString(), ((WrapperEntity) damage.entityResponsible).entity.getDisplayName().getString()));
                } else {
                    return new StringTextComponent(String.format(damage.language.value, player.getDisplayName().getString()));
                }
            }
        };
        if (damage.isFire) {
            newSource.setIsFire();
            entity.setRemainingFireTicks(5);
        }
        if (damage.isWater) {
            entity.clearFire();
            //Don't attack this entity with water.
            return;
        }
        if (damage.isExplosion) {
            newSource.setExplosion();
        }
        if (damage.ignoreArmor) {
            newSource.bypassArmor();
        }
        if (damage.ignoreCooldown && entity instanceof LivingEntity) {
            entity.invulnerableTime = 0;
        }
        if (ConfigSystem.settings.general.creativeDamage.value) {
            newSource.bypassInvul();
        }
        entity.hurt(newSource, (float) damage.amount);

        if (damage.effects != null && entity instanceof LivingEntity) {
            damage.effects.forEach(effect -> addPotionEffect(effect));
        }
    }

    @Override
    public Point3D getRenderedPosition(float partialTicks) {
        mutableRenderPosition.x = entity.xOld + (entity.position().x - entity.xOld) * partialTicks;
        mutableRenderPosition.y = entity.yOld + (entity.position().y - entity.yOld) * partialTicks;
        mutableRenderPosition.z = entity.zOld + (entity.position().z - entity.zOld) * partialTicks;
        return mutableRenderPosition;
    }

    private final Point3D mutableRenderPosition = new Point3D();

    @Override
    public void addPotionEffect(JSONPotionEffect effect) {
        if ((entity instanceof LivingEntity)) {
            Potion potion = Potion.byName(effect.name);
            if (potion != null) {
                potion.getEffects().forEach(mcEffect -> {
                    ((LivingEntity) entity).addEffect(new EffectInstance(mcEffect.getEffect(), effect.duration, effect.amplifier, false, true));
                });
            } else {
                throw new NullPointerException("Potion " + effect.name + " does not exist.");
            }
        }
    }

    @Override
    public void removePotionEffect(JSONPotionEffect effect) {
        if ((entity instanceof LivingEntity)) {
            Potion potion = Potion.byName(effect.name);
            if (potion != null) {
                potion.getEffects().forEach(mcEffect -> {
                    ((LivingEntity) entity).removeEffect(mcEffect.getEffect());
                });
            } else {
                throw new NullPointerException("Potion " + effect.name + " does not exist.");
            }
        }
    }

    /**
     * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
     */
    @SubscribeEvent
    public static void on(WorldEvent.Unload event) {
        entityWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.level);
    }
}