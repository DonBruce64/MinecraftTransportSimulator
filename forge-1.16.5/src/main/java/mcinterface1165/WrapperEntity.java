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
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LeadItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
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
        if (entity instanceof PlayerEntity) {
            return WrapperPlayer.getWrapperFor((PlayerEntity) entity);
        } else if (entity != null) {
            Map<Entity, WrapperEntity> entityWrappers = entity.world.isClient ? entityClientWrappers : entityServerWrappers;
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
        return entity.getUuid();
    }

    @Override
    public String getName() {
        return entity.getName().getString();
    }

    @Override
    public AWrapperWorld getWorld() {
        return WrapperWorld.getWrapperFor(entity.world);
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
                if (!entity.world.isClient) {
                    BuilderEntityLinkedSeat seat = new BuilderEntityLinkedSeat(BuilderEntityLinkedSeat.ENTITY_SEAT.get(), ((WrapperWorld) entityToRide.world).world);
                    seat.loadedFromSavedNBT = true;
                    seat.setPos(entityToRide.position.x, entityToRide.position.y, entityToRide.position.z);
                    seat.entity = entityToRide;
                    entity.world.spawnEntity(seat);
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
        return 1.0;
    }

    @Override
    public double getSeatOffset() {
        //Vanilla entities (boat/minecart) normally have a 0.14 pixel delta from their base to where the entity sits.
        //We account for this here.
        AEntityB_Existing riding = getEntityRiding();
        if (riding instanceof PartSeat && !((PartSeat) riding).definition.seat.standing) {
            if (entity instanceof VillagerEntity) {
                //Need to add-on a factor for villagers since we make them sit whereas they don't normally do this.
                //Actual factor is based on what players have for their offsets, see the player wrapper method for details.
                return entity.getHeightOffset() - 0.14D - 0.485D;
            } else {
                return entity.getHeightOffset() - 0.14D;
            }
        }
        return 0;
    }

    @Override
    public double getEyeHeight() {
        return entity.getStandingEyeHeight();
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
        entity.setVelocity(entity.getVelocity().add(motion.x, motion.y, motion.z));
    }

    @Override
    public Point3D getVelocity() {
        //Need to manually put 0 here for Y since entities on ground have a constant -Y motion.
        mutableVelocity.set(entity.getVelocity().x, entity.isOnGround() ? 0 : entity.getVelocity().y, entity.getVelocity().z);
        return mutableVelocity;
    }

    private final Point3D mutableVelocity = new Point3D();

    @Override
    public void setVelocity(Point3D motion) {
        entity.setVelocity(motion.x, motion.y, motion.z);
    }

    @Override
    public RotationMatrix getOrientation() {
        if (lastPitchChecked != entity.pitch || lastYawChecked != entity.yaw) {
            lastPitchChecked = entity.pitch;
            lastYawChecked = entity.yaw;
            mutableOrientation.angles.set(entity.pitch, -entity.yaw, 0);
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
        if (entity.world.isClient) {
            //Client-side expects the yaw keep going and not reset at the 360 bounds like our matrix does.
            //Therefore, we need to check our delta from our rotation matrix and apply that VS the raw value.
            //Clamp delta to +/- 180 to ensure that we don't go 360 backwards when crossing the 0/360 zone.
            float yawDelta = ((float) -rotation.angles.y - lastYawApplied) % 360;
            if (yawDelta > 180) {
                yawDelta -= 360;
            } else if (yawDelta < -180) {
                yawDelta -= 360;
            }
            entity.yaw = lastYawApplied + yawDelta;
            lastYawApplied = entity.yaw;
        } else {
            entity.yaw = (float) -rotation.angles.y;
        }
        entity.pitch = (float) rotation.angles.x;
    }

    @Override
    public float getPitch() {
        return entity.pitch;
    }

    @Override
    public float getPitchDelta() {
        float value = entity.pitch - lastPitch;
        lastPitch = entity.pitch;
        return value;
    }

    private float lastPitch;

    @Override
    public float getYaw() {
        return -entity.yaw;
    }

    @Override
    public float getYawDelta() {
        float value = entity.yaw - lastYaw;
        lastYaw = entity.yaw;
        return -value;
    }

    private float lastYaw;

    @Override
    public float getBodyYaw() {
        return entity instanceof LivingEntity ? -((LivingEntity) entity).bodyYaw : 0;
    }

    @Override
    public Point3D getLineOfSight(double distance) {
        mutableSight.set(0, 0, distance).rotate(getOrientation());
        return mutableSight;
    }

    private final Point3D mutableSight = new Point3D();

    @Override
    public void setYaw(double yaw) {
        entity.yaw = (float) -yaw;
    }

    @Override
    public void setBodyYaw(double yaw) {
        if (entity instanceof LivingEntity) {
            entity.setBodyYaw((float) -yaw);
        }
    }

    @Override
    public void setPitch(double pitch) {
        entity.pitch = (float) pitch;
    }

    @Override
    public BoundingBox getBounds() {
        mutableBounds.widthRadius = entity.getWidth() / 2F;
        mutableBounds.heightRadius = entity.getHeight() / 2F;
        mutableBounds.depthRadius = entity.getWidth() / 2F;
        mutableBounds.globalCenter.set(entity.getX(), entity.getY() + mutableBounds.heightRadius, entity.getZ());
        return mutableBounds;
    }

    private final BoundingBox mutableBounds = new BoundingBox(new Point3D(), 0, 0, 0);

    @Override
    public IWrapperNBT getData() {
        NbtCompound nbt = new NbtCompound();
        entity.writeNbt(nbt);
        return new WrapperNBT(nbt);
    }

    @Override
    public void setData(IWrapperNBT data) {
        entity.readNbt(((WrapperNBT) data).compound);
    }

    @Override
    public boolean leashTo(IWrapperPlayer player) {
        PlayerEntity mcPlayer = ((WrapperPlayer) player).player;
        if (entity instanceof MobEntity) {
            ItemStack heldStack = mcPlayer.getMainHandStack();
            if (((MobEntity) entity).canBeLeashedBy(mcPlayer) && heldStack.getItem() instanceof LeadItem) {
                ((MobEntity) entity).attachLeash(mcPlayer, true);
                if (!mcPlayer.isCreative()) {
                    heldStack.decrement(1);
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
            public Text getDeathMessage(LivingEntity player) {
                if (damage.entityResponsible != null) {
                    return new LiteralText(String.format(damage.language.getCurrentValue(), player.getDisplayName().getString(), ((WrapperEntity) damage.entityResponsible).entity.getDisplayName().getString()));
                } else {
                    return new LiteralText(String.format(damage.language.getCurrentValue(), player.getDisplayName().getString()));
                }
            }
        };
        if (damage.isFire) {
            newSource.setFire();
            entity.setFireTicks(5);
        }
        if (damage.knockback != null) {
            applyMotion(damage.knockback);
        }
        if (damage.isWater) {
            entity.extinguish();
            //Don't attack this entity with water.
            return;
        }
        if (damage.isExplosion) {
            newSource.setExplosive();
        }
        if (damage.ignoreArmor) {
            newSource.setBypassesArmor();
        }
        if (damage.ignoreCooldown && entity instanceof LivingEntity) {
            entity.timeUntilRegen = 0;
        }
        if (ConfigSystem.settings.damage.creativePlayerDamage.value) {
            newSource.setOutOfWorld();
        }
        entity.damage(newSource, (float) damage.amount);

        if (damage.effects != null) {
            damage.effects.forEach(this::addPotionEffect);
        }
    }

    @Override
    public void addPotionEffect(JSONPotionEffect effect) {
        if ((entity instanceof LivingEntity)) {
            Potion potion = Potion.byId(effect.name);
            if (potion != null) {
                potion.getEffects().forEach(mcEffect -> ((LivingEntity) entity).addStatusEffect(new StatusEffectInstance(mcEffect.getEffectType(), effect.duration, effect.amplifier, false, false)));
            } else {
                throw new NullPointerException("Potion " + effect.name + " does not exist.");
            }
        }
    }

    @Override
    public void removePotionEffect(JSONPotionEffect effect) {
        if ((entity instanceof LivingEntity)) {
            Potion potion = Potion.byId(effect.name);
            if (potion != null) {
                potion.getEffects().forEach(mcEffect -> ((LivingEntity) entity).removeStatusEffect(mcEffect.getEffectType()));
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
        if (event.getWorld().isClient()) {
            entityClientWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.world);
        } else {
            entityServerWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.world);
        }
    }
}