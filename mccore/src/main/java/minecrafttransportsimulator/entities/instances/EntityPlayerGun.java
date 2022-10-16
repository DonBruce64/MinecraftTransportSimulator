package minecrafttransportsimulator.entities.instances;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartGun.GunState;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONDummyPartProvider;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Entity class responsible for storing and syncing information about the current gun
 * any player is holding.  This entity will trigger rendering of the held gun, if it exists.
 * The current item the player is holding is stored, and whenever the player either changes
 * this item, or stops firing, the data is saved back to that item to ensure that the gun's
 * state is maintained.
 *
 * @author don_bruce
 */
public class EntityPlayerGun extends AEntityF_Multipart<JSONDummyPartProvider> {
    public static final Map<UUID, EntityPlayerGun> playerClientGuns = new HashMap<>();
    public static final Map<UUID, EntityPlayerGun> playerServerGuns = new HashMap<>();

    public final IWrapperPlayer player;
    private final RotationMatrix handRotation = new RotationMatrix();
    private int hotbarSelected = -1;
    private IWrapperItemStack gunStack;
    private boolean didGunFireLastTick;
    public PartGun activeGun;

    public EntityPlayerGun(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, data);
        //Get the player spawning us.
        if (placingPlayer != null) {
            //Newly-spawned entity.
            this.player = placingPlayer;
            position.set(player.getPosition());
            prevPosition.set(position);
        } else {
            //Saved entity.  Either on the server or client.
            //Get player via saved NBT.  If the player isn't found, we're not valid.
            UUID playerUUID = data.getUUID("playerUUID");
            IWrapperPlayer foundPlayer = null;
            for (IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))) {
                if (entity instanceof IWrapperPlayer) {
                    if (entity.getID().equals(playerUUID)) {
                        foundPlayer = (IWrapperPlayer) entity;
                        break;
                    }
                }
            }
            if (foundPlayer != null) {
                this.player = foundPlayer;
            } else {
                this.player = null;
                return;
            }
        }

        //Don't load duplicates.  However, do ensure if we replace a gun we remove it.
        //These come after the player joins the world where a gun was already present.
        if (world.isClient()) {
            if (playerClientGuns.containsKey(player.getID())) {
                playerClientGuns.get(player.getID()).remove();
            }
            playerClientGuns.put(player.getID(), this);
        } else {
            if (playerServerGuns.containsKey(player.getID())) {
                playerServerGuns.get(player.getID()).remove();
            }
            playerServerGuns.put(player.getID(), this);
        }
    }

    @Override
    public JSONDummyPartProvider generateDefaultDefinition() {
        JSONDummyPartProvider defaultDefinition = JSONDummyPartProvider.generateDummy();

        //Look though all gun types and add them.
        JSONPartDefinition fakeDef = defaultDefinition.parts.get(0);
        for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
            if (packItem instanceof ItemPartGun) {
                ItemPartGun gunItem = (ItemPartGun) packItem;
                if (gunItem.definition.gun.handHeld) {
                    if (!fakeDef.types.contains(gunItem.definition.generic.type)) {
                        fakeDef.types.add(gunItem.definition.generic.type);
                    }
                }
            }
        }

        return defaultDefinition;
    }

    @Override
    public void update() {
        super.update();
        //Make sure player is still valid and haven't left the server.
        if (player != null && player.isValid()) {
            //Set our position to the player's position.  We may update this later if we have a gun.
            //We can't update position without the gun as it has an offset defined in it.
            position.set(player.getPosition());
            motion.set(player.getVelocity());

            //Get the current gun.
            activeGun = parts.isEmpty() ? null : (PartGun) parts.get(0);

            //If we have a gun, but the player's held stack is null, get it now.
            //This happens if we load a gun as a saved part.
            if (activeGun != null && gunStack == null) {
                AItemBase heldItem = player.getHeldItem();
                if (heldItem instanceof ItemPartGun) {
                    ItemPartGun heldGun = (ItemPartGun) heldItem;
                    if (heldGun.definition.gun.handHeld) {
                        gunStack = player.getHeldStack();
                        hotbarSelected = player.getHotbarIndex();
                    }
                }
                if (activeGun != null && gunStack == null) {
                    //Either the player's held item changed, or the pack did.
                    //Held gun is invalid, so don't use or save it.
                    removePart(activeGun, null);
                    activeGun = null;
                }
            }

            if (!world.isClient()) {
                //Check to make sure if we had a gun, that it didn't change.
                if (activeGun != null && (!activeGun.getItem().equals(player.getHeldItem()) || hotbarSelected != player.getHotbarIndex())) {
                    saveGun(true);
                }

                //If we don't have a gun yet, try to get the current one if the player is holding one.
                if (activeGun == null) {
                    AItemBase heldItem = player.getHeldItem();
                    if (heldItem instanceof ItemPartGun) {
                        ItemPartGun heldGun = (ItemPartGun) heldItem;
                        if (heldGun.definition.gun.handHeld) {
                            gunStack = player.getHeldStack();
                            addPartFromItem(heldGun, player, gunStack.getData(), 0);
                            hotbarSelected = player.getHotbarIndex();
                        }
                    }
                }
            }

            //If we have a gun, do updates to it.
            //Only change firing command on servers to prevent de-syncs.
            //Packets will get sent to clients to change them.
            if (activeGun != null) {
                //First rotate transform to match the player's hand.
                handRotation.setToZero().rotateX(player.getPitch());

                //Offset to the end of the hand with our offset and current rotation.
                if (activeGun.isHandHeldGunAimed) {
                    position.set(activeGun.definition.gun.handHeldAimedOffset);
                } else {
                    position.set(activeGun.definition.gun.handHeldNormalOffset);
                }
                position.rotate(handRotation);

                //Arm center is 0.3125 blocks away in X, 1.375 blocks up in Y.
                //Sneaking lowers arm by 0.2 blocks.
                //We adjust this by the seated offset and scale to get the actual position.
                position.add(-0.3125, ((player.isSneaking() ? 1.3125 - 0.2 : 1.3125) + player.getSeatOffset()) * player.getVerticalScale(), 0);

                //Rotate to player's yaw to match facing direction.
                handRotation.setToZero().rotateY(player.getYaw());
                position.rotate(handRotation);

                //While riding an entity, the player will always be updated
                //after this entity, as the player always updates first, and will be "lagging" in their actual position.
                AEntityB_Existing ridingEntity = player.getEntityRiding();
                if (ridingEntity != null) {
                    position.add(ridingEntity.position).subtract(ridingEntity.prevPosition);
                }

                //Now do final player operations.
                orientation.set(player.getOrientation());
                position.add(player.getPosition());

                if (!world.isClient()) {
                    //Save gun data if we stopped firing the prior tick.
                    if (activeGun.state.isAtLeast(GunState.FIRING_CURRENTLY)) {
                        didGunFireLastTick = true;
                    } else if (didGunFireLastTick) {
                        saveGun(false);
                    }
                }
            }
        } else {
            //Player is either null or not valid.  Remove us.
            //Don't update post movement, as the gun will crash on update.
            remove();
            return;
        }

        //If we have a gun, and the player is spectating, don't allow the gun to render.
        if (activeGun != null) {
            activeGun.isInvisible = player != null && player.isSpectator();
        }
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return true;
    }

    @Override
    public void remove() {
        super.remove();
        if (player != null) {
            if (world.isClient()) {
                playerClientGuns.remove(player.getID());
            } else {
                playerServerGuns.remove(player.getID());
            }
        }
    }

    @Override
    public boolean shouldRenderBeams() {
        return ConfigSystem.client.renderingSettings.vehicleBeams.value;
    }

    @Override
    protected void updateCollisionBoxes() {
        //Do nothing and don't add any collision.  This could block player actions.
    }

    private void saveGun(boolean remove) {
        gunStack.setData(activeGun.save(gunStack.getData()));
        didGunFireLastTick = false;
        if (remove) {
            removePart(activeGun, null);
            activeGun = null;
        }
    }

    @Override
    public boolean disableRendering(float partialTicks) {
        //Don't render the player gun entity.  Only render the gun itself.
        return true;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        if (player != null) {
            data.setUUID("playerUUID", player.getID());
        }
        return data;
    }
}
