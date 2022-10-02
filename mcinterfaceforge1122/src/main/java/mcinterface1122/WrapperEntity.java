package mcinterface1122;

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
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemLead;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
        if (entity instanceof EntityPlayer) {
            return WrapperPlayer.getWrapperFor((EntityPlayer) entity);
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
        return entity != null && !entity.isDead && (!(entity instanceof EntityLivingBase) || ((EntityLivingBase) entity).deathTime == 0);
    }

    @Override
    public UUID getID() {
        return entity.getUniqueID();
    }

    @Override
    public String getName() {
        return entity.getName();
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
            Entity mcEntityRiding = entity.getRidingEntity();
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
                if (!entity.world.isRemote) {
                    BuilderEntityLinkedSeat seat = new BuilderEntityLinkedSeat(((WrapperWorld) entityToRide.world).world);
                    seat.loadedFromSavedNBT = true;
                    seat.setPositionAndRotation(entityToRide.position.x, entityToRide.position.y, entityToRide.position.z, 0, 0);
                    seat.entity = entityToRide;
                    entity.world.spawnEntity(seat);
                    entity.startRiding(seat, true);
                }
            } else {
                //Just change entity reference, we will already be a rider on the entity at this point.
                ((BuilderEntityLinkedSeat) entity.getRidingEntity()).entity = entityToRide;
            }
            cachedEntityRiding = entityToRide;
        } else {
            entity.dismountRidingEntity();
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
        mutablePosition.set(entity.posX, entity.posY, entity.posZ);
        return mutablePosition;
    }

    private final Point3D mutablePosition = new Point3D();

    @Override
    public void setPosition(Point3D position, boolean onGround) {
        entity.setPosition(position.x, position.y, position.z);
        //Set fallDistance to 0 to prevent damage.
        entity.fallDistance = 0;
        entity.onGround = onGround;
    }

    @Override
    public Point3D getVelocity() {
        //Need to manually put 0 here for Y since entities on ground have a constant -Y motion.
        mutableVelocity.set(entity.motionX, entity.onGround ? 0 : entity.motionY, entity.motionZ);
        return mutableVelocity;
    }

    private final Point3D mutableVelocity = new Point3D();

    @Override
    public void setVelocity(Point3D motion) {
        entity.motionX = motion.x;
        entity.motionY = motion.y;
        entity.motionZ = motion.z;
    }

    @Override
    public RotationMatrix getOrientation() {
        if (lastPitchChecked != entity.rotationPitch || lastYawChecked != entity.rotationYaw) {
            lastPitchChecked = entity.rotationPitch;
            lastYawChecked = entity.rotationYaw;
            mutableOrientation.angles.set(entity.rotationPitch, -entity.rotationYaw, 0);
            mutableOrientation.setToAngles(mutableOrientation.angles);
        }
        return mutableOrientation;
    }

    private final RotationMatrix mutableOrientation = new RotationMatrix();
    private float lastPitchChecked;
    private float lastYawChecked;

    @Override
    public void setOrientation(RotationMatrix rotation) {
        entity.rotationYaw = (float) -rotation.angles.y;
        entity.rotationPitch = (float) rotation.angles.x;
    }

    @Override
    public float getPitch() {
        return entity.rotationPitch;
    }

    @Override
    public float getPitchDelta() {
        float value = entity.rotationPitch - lastPitch;
        lastPitch = entity.rotationPitch;
        return value;
    }

    private float lastPitch;

    @Override
    public float getYaw() {
        return -entity.rotationYaw;
    }

    @Override
    public float getYawDelta() {
        float value = entity.rotationYaw - lastYaw;
        lastYaw = entity.rotationYaw;
        return -value;
    }

    private float lastYaw;

    @Override
    public float getBodyYaw() {
        return entity instanceof EntityLivingBase ? -((EntityLivingBase) entity).renderYawOffset : 0;
    }

    @Override
    public Point3D getLineOfSight(double distance) {
        mutableSight.set(0, 0, distance).rotate(getOrientation());
        return mutableSight;
    }

    private final Point3D mutableSight = new Point3D();

    @Override
    public void setYaw(double yaw) {
        entity.rotationYaw = (float) -yaw;
    }

    @Override
    public void setBodyYaw(double yaw) {
        if (entity instanceof EntityLivingBase) {
            entity.setRenderYawOffset((float) -yaw);
        }
    }

    @Override
    public void setPitch(double pitch) {
        entity.rotationPitch = (float) pitch;
    }

    @Override
    public BoundingBox getBounds() {
        mutableBounds.widthRadius = entity.width / 2F;
        mutableBounds.heightRadius = entity.height / 2F;
        mutableBounds.depthRadius = entity.width / 2F;
        mutableBounds.globalCenter.set(entity.posX, entity.posY + mutableBounds.heightRadius, entity.posZ);
        return mutableBounds;
    }

    private final BoundingBox mutableBounds = new BoundingBox(new Point3D(), 0, 0, 0);

    @Override
    public IWrapperNBT getData() {
        NBTTagCompound tag = new NBTTagCompound();
        entity.writeToNBT(tag);
        return new WrapperNBT(tag);
    }

    @Override
    public void setData(IWrapperNBT data) {
        entity.readFromNBT(((WrapperNBT) data).tag);
    }

    @Override
    public boolean leashTo(IWrapperPlayer player) {
        EntityPlayer mcPlayer = ((WrapperPlayer) player).player;
        if (entity instanceof EntityLiving) {
            ItemStack heldStack = mcPlayer.getHeldItemMainhand();
            if (((EntityLiving) entity).canBeLeashedTo(mcPlayer) && heldStack.getItem() instanceof ItemLead) {
                ((EntityLiving) entity).setLeashHolder(mcPlayer, true);
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
            public ITextComponent getDeathMessage(EntityLivingBase player) {
                if (damage.entityResponsible != null) {
                    return new TextComponentString(String.format(damage.language.value, player.getDisplayName().getFormattedText(), ((WrapperEntity) damage.entityResponsible).entity.getDisplayName().getFormattedText()));
                } else {
                    return new TextComponentString(String.format(damage.language.value, player.getDisplayName().getFormattedText()));
                }
            }
        };
        if (damage.isFire) {
            newSource.setFireDamage();
            entity.setFire(5);
        }
        if (damage.isWater) {
            entity.extinguish();
            //Don't attack this entity with water.
            return;
        }
        if (damage.isExplosion) {
            newSource.setExplosion();
        }
        if (damage.ignoreArmor) {
            newSource.setDamageBypassesArmor();
        }
        if (damage.ignoreCooldown && entity instanceof EntityLivingBase) {
            entity.hurtResistantTime = 0;
        }
        if (ConfigSystem.settings.general.creativeDamage.value) {
            newSource.setDamageAllowedInCreativeMode();
        }
        entity.attackEntityFrom(newSource, (float) damage.amount);

        if (damage.effects != null && entity instanceof EntityLivingBase) {
            for (JSONPotionEffect effect : damage.effects) {
                Potion potion = Potion.getPotionFromResourceLocation(effect.name);
                if (potion != null) {
                    ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(potion, effect.duration, effect.amplifier, false, true));
                } else {
                    throw new NullPointerException("Potion " + effect.name + " does not exist.");
                }
            }
        }
    }

    @Override
    public Point3D getRenderedPosition(float partialTicks) {
        mutableRenderPosition.x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        mutableRenderPosition.y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        mutableRenderPosition.z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        return mutableRenderPosition;
    }

    private final Point3D mutableRenderPosition = new Point3D();

    @Override
    public void addPotionEffect(JSONPotionEffect effect) {
        // Only instances of EntityLivingBase can receive potion effects
        if ((entity instanceof EntityLivingBase)) {
            Potion potion = Potion.getPotionFromResourceLocation(effect.name);
            if (potion != null) {
                ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(potion, effect.duration, effect.amplifier, false, false));
            } else {
                throw new NullPointerException("Potion " + effect.name + " does not exist.");
            }
        }
    }

    @Override
    public void removePotionEffect(JSONPotionEffect effect) {
        // Only instances of EntityLivingBase can have potion effects
        if ((entity instanceof EntityLivingBase)) {
            //Uses a potion here instead of potionEffect because the duration/amplifier is irrelevant
            Potion potion = Potion.getPotionFromResourceLocation(effect.name);
            if (potion != null) {
                ((EntityLivingBase) entity).removePotionEffect(potion);
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
        entityWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.world);
    }
}