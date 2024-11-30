package mcinterface1182;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class WrapperEntity implements IWrapperEntity {
    private static final Map<Entity, WrapperEntity> entityClientWrappers = new HashMap<>();
    private static final Map<Entity, WrapperEntity> entityServerWrappers = new HashMap<>();

    protected final Entity entity;
    private AEntityB_Existing cachedEntityRiding;

    /**
     * Returns a wrapper instance for the passed-in entity instance.
     * Null may be passed-in safely to ease function-forwarding.
     * Wrapper is cached to avoid re-creating the wrapper each time it is requested.
     * If the entity is a player, then a player wrapper is returned.
     */
    public static WrapperEntity getWrapperFor(Entity entity) {
        if (entity instanceof Player) {
            return WrapperPlayer.getWrapperFor((Player) entity);
        } else if (entity != null) {
            Map<Entity, WrapperEntity> entityWrappers = entity.level.isClientSide ? entityClientWrappers : entityServerWrappers;
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
        return entity != null && entity.isAlive();
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
                    BuilderEntityLinkedSeat seat = new BuilderEntityLinkedSeat(BuilderEntityLinkedSeat.E_TYPE3.get(), ((WrapperWorld) entityToRide.world).world);
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
        //Vanilla entities (boat/minecart) normally have a 0.14 pixel delta from their base to where the entity sits.
        //We account for this here.
        AEntityB_Existing riding = getEntityRiding();
        if (riding instanceof PartSeat && !((PartSeat) riding).definition.seat.standing) {
            if (entity instanceof Animal) {
                //Animals are moved up 0.14 pixels (~2.25), for their sitting positions.  Un-do this.
                return entity.getMyRidingOffset() - 0.14D;
            } else if (entity instanceof Villager) {
                //Villagers get the same offset as players.
                return (-12D / 16D) * (30D / 32D);
            } else {
                ResourceLocation registration = entity.getType().getRegistryName();
                if (registration != null && registration.getNamespace().equals("customnpcs")) {
                    //CNPCs seem to be offset by 3, but invert their model scaling for their sitting position.
                    return -3D / 16D * (32D / 30D);
                } else {
                    return entity.getMyRidingOffset();
                }
            }
        }
        return 0;
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
    public Point3D getEyePosition() {
        AEntityB_Existing riding = getEntityRiding();
        return riding != null ? riding.riderEyePosition : getPosition().add(0, getEyeHeight() + getSeatOffset(), 0);
    }

    @Override
    public Point3D getHeadPosition() {
        AEntityB_Existing riding = getEntityRiding();
        return riding != null ? riding.riderHeadPosition : getPosition().add(0, getEyeHeight() + getSeatOffset(), 0);
    }

    @Override
    public void setPosition(Point3D position, boolean onGround) {
        if (cachedEntityRiding != null) {
            //Need to offset down to make bounding hitbox go down like normal. 
            entity.setPos(position.x, position.y + getSeatOffset(), position.z);
        } else {
            entity.setPos(position.x, position.y, position.z);
        }
        //Set fallDistance to 0 to prevent damage.
        entity.fallDistance = 0;
        entity.setOnGround(onGround);
    }

    @Override
    public void applyMotion(Point3D motion) {
        entity.push(motion.x, motion.y, motion.z);
        entity.hurtMarked = true;
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
        float pitchChecked = entity.getXRot();
        float yawChecked = entity.getYRot();
        if (lastPitchChecked != pitchChecked || lastYawChecked != yawChecked) {
            lastPitchChecked = pitchChecked;
            lastYawChecked = yawChecked;
            mutableOrientation.angles.set(lastPitchChecked, -lastYawChecked, 0);
            mutableOrientation.setToAngles(mutableOrientation.angles);
        }
        return mutableOrientation;
    }

    private final RotationMatrix mutableOrientation = new RotationMatrix();
    private float lastPitchChecked;
    private float lastYawChecked;
    private float lastYawApplied;

    @Override
    public void setOrientation(RotationMatrix rotation) {
        if (entity.level.isClientSide) {
            //Client-side expects the yaw keep going and not reset at the 360 bounds like our matrix does.
            //Therefore, we need to check our delta from our rotation matrix and apply that VS the raw value.
            //Clamp delta to +/- 180 to ensure that we don't go 360 backwards when crossing the 0/360 zone.
            float yawDelta = ((float) -rotation.angles.y - lastYawApplied) % 360;
            if (yawDelta > 180) {
                yawDelta -= 360;
            } else if (yawDelta < -180) {
                yawDelta -= 360;
            }
            entity.setYRot(lastYawApplied + yawDelta);
            lastYawApplied += yawDelta;
        } else {
            entity.setYRot((float) -rotation.angles.y);
        }
        entity.setXRot((float) rotation.angles.x);
    }

    @Override
    public float getPitch() {
        return entity.getXRot();
    }

    @Override
    public float getPitchDelta() {
        float currentPitch = entity.getXRot();
        float value = currentPitch - lastPitch;
        lastPitch = currentPitch;
        return value;
    }

    private float lastPitch;

    @Override
    public float getYaw() {
        return -entity.getYRot();
    }

    @Override
    public float getYawDelta() {
        float currentYaw = entity.getYRot();
        float value = currentYaw - lastYaw;
        lastYaw = currentYaw;
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
        entity.setYRot((float) -yaw);
    }

    @Override
    public void setBodyYaw(double yaw) {
        if (entity instanceof LivingEntity) {
            entity.setYBodyRot((float) -yaw);
        }
    }

    @Override
    public void setPitch(double pitch) {
        entity.setXRot((float) pitch);
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
        CompoundTag tag = new CompoundTag();
        entity.save(tag);
        return new WrapperNBT(tag);
    }

    @Override
    public void setData(IWrapperNBT data) {
        entity.load(((WrapperNBT) data).tag);
    }

    @Override
    public boolean leashTo(IWrapperPlayer player) {
        Player mcPlayer = ((WrapperPlayer) player).player;
        if (entity instanceof Mob) {
            ItemStack heldStack = mcPlayer.getMainHandItem();
            if (((Mob) entity).canBeLeashed(mcPlayer) && heldStack.getItem() instanceof LeadItem) {
                ((Mob) entity).setLeashedTo(mcPlayer, true);
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
        DamageSource newSource = new EntityDamageSource(damage.language.getCurrentValue(), damage.entityResponsible != null ? ((WrapperEntity) damage.entityResponsible).entity : null) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity player) {
                if (damage.entityResponsible != null) {
                    return new TextComponent(String.format(damage.language.getCurrentValue(), player.getDisplayName().getString(), ((WrapperEntity) damage.entityResponsible).entity.getDisplayName().getString()));
                } else {
                    return new TextComponent(String.format(damage.language.getCurrentValue(), player.getDisplayName().getString()));
                }
            }
        };
        if (damage.isFire) {
            newSource.setIsFire();
            entity.setRemainingFireTicks(5);
        }
        if (damage.knockback != null) {
            applyMotion(damage.knockback);
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
        if (ConfigSystem.settings.damage.creativePlayerDamage.value) {
            newSource.bypassInvul();
        }
        entity.hurt(newSource, (float) damage.amount);

        if (damage.effects != null) {
            damage.effects.forEach(effect -> addPotionEffect(effect));
        }
    }

    @Override
    public void addPotionEffect(JSONPotionEffect effect) {
        if ((entity instanceof LivingEntity)) {
            Potion potion = Potion.byName(effect.name);
            if (potion != null) {
                potion.getEffects().forEach(mcEffect -> {
                    ((LivingEntity) entity).addEffect(new MobEffectInstance(mcEffect.getEffect(), effect.duration, effect.amplifier, false, false));
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
    public static void onIVWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            entityClientWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.level);
        } else {
            entityServerWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.level);
        }
    }
}