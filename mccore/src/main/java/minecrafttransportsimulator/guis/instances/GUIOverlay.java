package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentCrosshair;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * A GUI that is used to render overlay components.  These components are independent of
 * any vehicle or entity the player is riding, and are always visible.
 *
 * @author don_bruce
 */
public class GUIOverlay extends AGUIBase {
    private GUIComponentLabel mouseoverLabel;
    private GUIComponentLabel gunLabel;
    private GUIComponentItem scannerItem;
    private GUIComponentCrosshair aimingCrosshair;
    private final List<String> tooltipText = new ArrayList<>();
    private EntityInteractResult lastInteractResult;
    private AEntityE_Interactable<?> lastCollisionGroupHoverEntity;
    private int lastCollisionGroupHoverIndex;
    private int lastCollisionBoxHoverIndex;
    private final LinkedHashSet<AEntityD_Definable<?>> trackedGUITextEntities = new LinkedHashSet<>();

    // Re-used scratch objects for ballistic simulation — never allocated on the hot path.
    private final Point3D simPos = new Point3D();
    private final Point3D simVel = new Point3D();
    private final Point3D simDir = new Point3D();
    private final RotationMatrix simOri = new RotationMatrix();

    @Override
    public void setupComponents() {
        super.setupComponents();

        addComponent(mouseoverLabel = new GUIComponentLabel(screenWidth / 2, screenHeight / 2 + 10, ColorRGB.WHITE, "", TextAlignment.CENTERED, 1.0F));
        addComponent(gunLabel = new GUIComponentLabel(screenWidth, 0, ColorRGB.WHITE, "", TextAlignment.RIGHT_ALIGNED, 1.0F));
        gunLabel.ignoreGUILightingState = true;
        // Start crosshair at screen centre; setStates() repositions it every frame.
        addComponent(aimingCrosshair = new GUIComponentCrosshair(screenWidth / 2, screenHeight / 2));
        aimingCrosshair.visible = false;
        addComponent(scannerItem = new GUIComponentItem(0, screenHeight / 4, 6.0F) {
            //Render the item stats as a tooltip, as it's easier to see.
            @Override
            public boolean isMouseInBounds(int mouseX, int mouseY) {
                return true;
            }

            @Override
            public void renderTooltip(AGUIBase gui, int mouseX, int mouseY) {
                super.renderTooltip(gui, scannerItem.constructedX, scannerItem.constructedY + 24 * 6);
            }

            @Override
            public List<String> getTooltipText() {
                return tooltipText;
            }
        });
    }

    @Override
    public void setStates() {
        super.setStates();
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();

        //Set gun label text, if we are controlling a gun.
        gunLabel.visible = false;
        aimingCrosshair.visible = false;
        aimingCrosshair.pendingImpactPoint = null;
        aimingCrosshair.vehicleRef = null;
        if (!InterfaceManager.clientInterface.isChatOpen()) {
            EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
            List<PartGun> activeGunGroup = null;
            if (playerGun != null && playerGun.activeGun != null) {
                gunLabel.visible = true;
                gunLabel.text = "Gun:" + playerGun.activeGun.cachedItem.getItemName() + " Loaded:" + playerGun.activeGun.getBulletText();
                activeGunGroup = java.util.Collections.singletonList(playerGun.activeGun);
            } else {
                AEntityB_Existing entityRiding = player.getEntityRiding();
                if (entityRiding instanceof PartSeat) {
                    PartSeat seat = (PartSeat) entityRiding;
                    if (seat.canControlGuns) {
                        gunLabel.visible = true;
                        gunLabel.text = "Active Gun:";
                        if (seat.activeGunItem != null) {
                            gunLabel.text += seat.activeGunItem.getItemName();
                            if (seat.activeGunItem.definition.gun.fireSolo) {
                                gunLabel.text += " [" + (seat.gunIndex + 1) + "]";
                            }
                            List<PartGun> gunGroup = seat.gunGroups.get(seat.activeGunItem);
                            if (gunGroup != null && !gunGroup.isEmpty()) {
                                activeGunGroup = gunGroup;
                            }
                        } else {
                            gunLabel.text += "None";
                        }
                    }
                }
            }

            // Update the aiming crosshair position when a gun is active.
            if (activeGunGroup != null && ConfigSystem.client.controlSettings.arcadeMode.value) {
                updateAimingCrosshair(activeGunGroup, player.getWorld());
            }
        }

        Point3D startPosition = player.getEyePosition();
        Point3D endPosition = player.getLineOfSight(10).add(startPosition);
        EntityInteractResult interactResult = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);

        if (lastInteractResult != null && (interactResult == null || interactResult.entity != lastInteractResult.entity)) {
            lastInteractResult.entity.playerCursorHoveredVar.setActive(false, false);
            lastInteractResult = null;
        }
        if (lastInteractResult == null && interactResult != null) {
            interactResult.entity.playerCursorHoveredVar.setActive(true, false);
            lastInteractResult = interactResult;
        }
        
        int hoveredCollisionGroupIndex = getCollisionGroupIndex(interactResult);
        int hoveredCollisionBoxIndex = getCollisionBoxIndex(interactResult);
        boolean collisionGroupHoverChanged = interactResult == null || interactResult.entity != lastCollisionGroupHoverEntity || hoveredCollisionGroupIndex != lastCollisionGroupHoverIndex;
        boolean collisionBoxHoverChanged = collisionGroupHoverChanged || hoveredCollisionBoxIndex != lastCollisionBoxHoverIndex;
        if (lastCollisionGroupHoverEntity != null) {
            if (collisionBoxHoverChanged && lastCollisionBoxHoverIndex > 0) {
                //Box indexes are 1-based and follow the order of the collision entries in the hovered group.
                lastCollisionGroupHoverEntity.getOrCreateVariable("collision_" + lastCollisionGroupHoverIndex + "_" + lastCollisionBoxHoverIndex + "_player_cursor_hovered").setActive(false, false);
                lastCollisionBoxHoverIndex = 0;
            }
            if (collisionGroupHoverChanged) {
                lastCollisionGroupHoverEntity.getOrCreateVariable("collision_" + lastCollisionGroupHoverIndex + "_player_cursor_hovered").setActive(false, false);
                lastCollisionGroupHoverEntity = null;
                lastCollisionGroupHoverIndex = 0;
            }
        }
        if (interactResult != null && hoveredCollisionGroupIndex > 0) {
            if (lastCollisionGroupHoverEntity == null) {
                interactResult.entity.getOrCreateVariable("collision_" + hoveredCollisionGroupIndex + "_player_cursor_hovered").setActive(true, false);
                lastCollisionGroupHoverEntity = interactResult.entity;
                lastCollisionGroupHoverIndex = hoveredCollisionGroupIndex;
            }
            if (collisionBoxHoverChanged && hoveredCollisionBoxIndex > 0) {
                interactResult.entity.getOrCreateVariable("collision_" + hoveredCollisionGroupIndex + "_" + hoveredCollisionBoxIndex + "_player_cursor_hovered").setActive(true, false);
            }
            lastCollisionBoxHoverIndex = hoveredCollisionBoxIndex;
        }
        
        mouseoverLabel.text = "";
        if (interactResult != null && interactResult.entity instanceof PartInteractable) {
            PartInteractable interactable = (PartInteractable) interactResult.entity;
            if (interactable.tank != null) {
                String fluidName = interactable.tank.getFluid();
                if(fluidName.isEmpty()) {
                    mouseoverLabel.text = String.format("%.1f/%.1fb", interactable.tank.getFluidLevel() / 1000F, interactable.tank.getMaxLevel() / 1000F);
                }else {
                    mouseoverLabel.text = String.format("%s: %.1f/%.1fb", InterfaceManager.clientInterface.getFluidName(fluidName, interactable.tank.getFluidMod()), interactable.tank.getFluidLevel() / 1000F, interactable.tank.getMaxLevel() / 1000F);
                }
            }
        }

        scannerItem.stack = null;
        tooltipText.clear();
        if (player.isHoldingItemType(ItemComponentType.SCANNER)) {
            if (interactResult != null && interactResult.entity instanceof AEntityF_Multipart) {
                AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) interactResult.entity;
                BoundingBox mousedOverBox = null;
                JSONPartDefinition packVehicleDef = null;
                for (Entry<BoundingBox, JSONPartDefinition> boxEntry : multipart.activeClientPartSlotBoxes.entrySet()) {
                    BoundingBox box = boxEntry.getKey();
                    if (box.getIntersection(startPosition, endPosition) != null) {
                        if (mousedOverBox == null || (box.globalCenter.distanceTo(startPosition) < mousedOverBox.globalCenter.distanceTo(startPosition))) {
                            mousedOverBox = box;
                            packVehicleDef = boxEntry.getValue();
                        }
                    }
                }

                if (mousedOverBox != null) {
                    //Populate stacks.
                    List<AItemPart> validParts = new ArrayList<>();
                    for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                        if (packItem instanceof AItemPart) {
                            AItemPart part = (AItemPart) packItem;
                            if (part.isPartValidForPackDef(packVehicleDef, multipart.subDefinition, true)) {
                                validParts.add(part);
                            }
                        }
                    }

                    //Get the slot info.
                    tooltipText.add("Types: " + packVehicleDef.types.toString());
                    tooltipText.add("Min/Max: " + packVehicleDef.minValue + "/" + packVehicleDef.maxValue);
                    if (packVehicleDef.customTypes != null) {
                        tooltipText.add("CustomTypes: " + packVehicleDef.customTypes);
                    } else {
                        tooltipText.add("CustomTypes: None");
                    }

                    //Get the stack to render..
                    if (!validParts.isEmpty()) {
                        //Get current part to render based on the cycle.
                        int cycle = player.isSneaking() ? 30 : 15;
                        AItemPart partToRender = validParts.get((int) ((multipart.ticksExisted / cycle) % validParts.size()));
                        tooltipText.add(partToRender.getItemName());
                        scannerItem.stack = partToRender.getNewStack(null);

                        //If we are on the start of the cycle, beep.
                        if (multipart.ticksExisted % cycle == 0) {
                            InterfaceManager.soundInterface.playQuickSound(new SoundInstance(multipart, InterfaceManager.coreModID + ":scanner_beep"));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean renderBackground() {
        return CameraSystem.customCameraOverlay != null;
    }

    @Override
    protected boolean renderBackgroundFullTexture() {
        return true;
    }

    @Override
    protected boolean canStayOpen() {
        return true;
    }

    @Override
    public boolean capturesPlayer() {
        return false;
    }

    @Override
    public int getWidth() {
        return screenWidth;
    }

    @Override
    public int getHeight() {
        return screenHeight;
    }

    @Override
    public boolean renderFlushBottom() {
        return true;
    }

    @Override
    public boolean renderTranslucent() {
        return true;
    }

    @Override
    protected void renderCustomElements(int mouseX, int mouseY, boolean blendingEnabled, float partialTicks) {
        if (!blendingEnabled || InterfaceManager.clientInterface.isGUIHidden()) {
            return;
        }

        LinkedHashSet<AEntityD_Definable<?>> currentGUITextEntities = new LinkedHashSet<>();
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        addGUITextEntityChain(currentGUITextEntities, player.getEntityRiding());
        if (lastInteractResult != null) {
            addGUITextEntityChain(currentGUITextEntities, lastInteractResult.entity);
        }

        trackedGUITextEntities.addAll(currentGUITextEntities);
        int entityLayer = 0;
        Iterator<AEntityD_Definable<?>> iterator = trackedGUITextEntities.iterator();
        while (iterator.hasNext()) {
            AEntityD_Definable<?> entity = iterator.next();
            if (!entity.isValid || !entity.hasGUIText()) {
                iterator.remove();
                continue;
            }

            boolean isCurrentEntity = currentGUITextEntities.contains(entity);
            boolean shouldKeepTracking = isCurrentEntity;
            for (JSONText textDef : entity.definition.rendering.guiTextObjects) {
                float alpha = entity.getGUITextAlpha(textDef, isCurrentEntity, partialTicks);
                if (alpha <= 0.0F) {
                    continue;
                }

                String textValue = entity.getGUITextValue(textDef, partialTicks);
                if (textValue == null || textValue.isEmpty()) {
                    continue;
                }

                //Each tracked entity gets a small Z slice so multiple prompts can overlap without fighting.
                RenderText.drawGUIText(textValue, entity, textDef, screenWidth, screenHeight, 325 + entityLayer * 5D, alpha);
                shouldKeepTracking = true;
            }

            if (!shouldKeepTracking) {
                iterator.remove();
            } else {
                ++entityLayer;
            }
        }
    }

    @Override
    protected String getTexture() {
        return CameraSystem.customCameraOverlay;
    }

    /**
     * Computes the predicted impact point for the given gun and stores it in the crosshair
     * component.  Screen projection (including partial-tick vehicle position correction) is
     * deferred to {@link GUIComponentCrosshair#render} where {@code partialTicks} is available.
     *
     * <p>Aircraft guns use a straight barrel-direction raycast (no gravity simulation) since
     * ballistic arcing is negligible at aircraft speeds.  Ground-vehicle and hand-held guns use
     * a full ballistic simulation; if the simulation exits loaded terrain, the result falls back
     * to the barrel-direction raycast so the crosshair remains stable.
     *
     * <p>When multiple guns are in the same group the muzzle positions are averaged so the crosshair
     * points to the geometric center of the salvo.  Velocity direction is taken from the first gun.
     */
    private void updateAimingCrosshair(List<PartGun> gunGroup, AWrapperWorld world) {
        PartGun firstGun = gunGroup.get(0);
        JSONMuzzle muzzle = firstGun.getActiveMuzzle();
        if (muzzle == null) return;

        // Populate muzzle position and velocity from the first gun.
        firstGun.setBulletSpawn(simPos, simVel, simOri, muzzle, false);
        simDir.set(0, 0, 1).rotate(simOri).normalize();

        // If multiple guns share the group, shift simPos to the group's muzzle center.
        if (gunGroup.size() > 1) {
            double sumX = simPos.x, sumY = simPos.y, sumZ = simPos.z;
            int count = 1;
            Point3D tmpPos = new Point3D();
            Point3D tmpVel = new Point3D();
            RotationMatrix tmpOri = new RotationMatrix();
            for (int i = 1; i < gunGroup.size(); i++) {
                PartGun otherGun = gunGroup.get(i);
                JSONMuzzle otherMuzzle = otherGun.getActiveMuzzle();
                if (otherMuzzle != null) {
                    otherGun.setBulletSpawn(tmpPos, tmpVel, tmpOri, otherMuzzle, false);
                    sumX += tmpPos.x; sumY += tmpPos.y; sumZ += tmpPos.z;
                    count++;
                }
            }
            simPos.set(sumX / count, sumY / count, sumZ / count);
        }

        double bulletSpeed = simVel.length();
        Point3D impactPoint;

        // Determine if this weapon has significant gravity (bomb/mortar).
        boolean isBallistic = firstGun.lastLoadedBullet != null
                && firstGun.lastLoadedBullet.definition.bullet.gravitationalVelocity > 0.001;
        boolean acceleratesAfterLaunch = firstGun.lastLoadedBullet != null
                && firstGun.lastLoadedBullet.definition.bullet.maxVelocity > 0
                && firstGun.lastLoadedBullet.definition.bullet.accelerationTime > 0;

        if (bulletSpeed < 0.001 && !acceleratesAfterLaunch) {
            // Zero-velocity non-accelerating projectile — use muzzle position.
            impactPoint = simPos.copy();
        } else if (firstGun.vehicleOn != null && firstGun.vehicleOn.definition.motorized.isAircraft && !isBallistic) {
            // Aircraft with flat-trajectory ammo (guns/cannons): barrel direction only.
            impactPoint = raycastBarrel(firstGun, world, firstGun.vehicleOn);
        } else {
            // Ground vehicle, hand-held, OR aircraft bomb (gravitationalVelocity > 0): ballistic.
            impactPoint = simulateBallistic(firstGun, world, firstGun.vehicleOn);
            if (impactPoint == null) {
                // Ballistic simulation left loaded terrain — fall back to barrel direction.
                impactPoint = raycastBarrel(firstGun, world, firstGun.vehicleOn);
            }
        }

        // Enable screen-space smoothing for aircraft to hide tick-boundary barrel direction jumps.
        boolean isAircraft = firstGun.vehicleOn != null && firstGun.vehicleOn.definition.motorized.isAircraft;
        aimingCrosshair.applySmoothing = isAircraft;

        // Store world-space impact point; render() will project with partial-tick correction.
        aimingCrosshair.pendingImpactPoint = impactPoint;
        aimingCrosshair.vehicleRef = firstGun.vehicleOn;
        aimingCrosshair.visible = true;
    }

    /**
     * Simulates the bullet trajectory tick-by-tick and returns the approximate world position
     * where it would first hit a block or entity, or the final resting position if the despawn
     * timer expires.  Returns {@code null} if the simulation crosses into an unloaded chunk
     * (caller should fall back to {@link #raycastBarrel}).
     * Uses the scratch fields {@link #simPos} and {@link #simVel} which must be initialised by the
     * caller before this is invoked (i.e. via {@link PartGun#setBulletSpawn}).
     */
    private Point3D simulateBallistic(PartGun gun, AWrapperWorld world, AEntityF_Multipart<?> ownVehicle) {
        double gravity   = (gun.lastLoadedBullet != null) ? gun.lastLoadedBullet.definition.bullet.gravitationalVelocity : 0;
        double slowdown  = (gun.lastLoadedBullet != null) ? gun.lastLoadedBullet.definition.bullet.slowdownSpeed : 0;
        int    burnTime  = (gun.lastLoadedBullet != null) ? gun.lastLoadedBullet.definition.bullet.burnTime : 0;
        int    accelerationTime  = (gun.lastLoadedBullet != null) ? gun.lastLoadedBullet.definition.bullet.accelerationTime : 0;
        int    accelerationDelay = (gun.lastLoadedBullet != null) ? gun.lastLoadedBullet.definition.bullet.accelerationDelay : 0;
        double velocityToAddEachTick = accelerationTime > 0 ? (gun.lastLoadedBullet.definition.bullet.maxVelocity / 20D - simVel.length()) / accelerationTime : 0;
        // Cap iterations to avoid freezing on mortars/artillery with large despawnTime values.
        int    maxTicks  = Math.min(
                (gun.lastLoadedBullet != null && gun.lastLoadedBullet.definition.bullet.despawnTime != 0)
                    ? gun.lastLoadedBullet.definition.bullet.despawnTime : 200,
                500);

        // Work on a copy so we don't pollute the scratch objects used elsewhere this frame.
        Point3D pos = simPos.copy();
        Point3D vel = simVel.copy();
        Point3D startPos = pos.copy();
        Point3D accelerationDirection = simDir.copy().normalize();
        IWrapperEntity gunController = gun.getGunController();

        if (vel.length() < 0.001 && velocityToAddEachTick == 0) {
            return pos;
        }

        for (int tick = 0; tick < maxTicks; tick++) {
            // Hard distance cap — prevent runaway loops on high-arc trajectories.
            if (startPos.distanceTo(pos) > 512) {
                return pos;
            }

            // Stop if we have entered an unloaded chunk — getBlockHit returns null there,
            // causing the crosshair to oscillate between chunk boundaries and empty space.
            // Return null so the caller can fall back to barrel-direction aiming.
            if (!world.chunkLoaded(pos)) {
                return null;
            }

            // Apply physics after burn time (mirrors EntityBullet.update: ticksExisted > burnTime).
            if (tick > burnTime) {
                if (slowdown > 0) {
                    double speed = vel.length();
                    if (speed > slowdown) {
                        vel.addScaled(vel.copy().normalize(), -slowdown);
                    } else {
                        break; // Bullet stopped.
                    }
                }
                vel.y -= gravity;
            }
            boolean notAcceleratingYet = accelerationDelay != 0 && tick < accelerationDelay;
            if (velocityToAddEachTick != 0 && !notAcceleratingYet && tick - accelerationDelay < accelerationTime) {
                vel.addScaled(accelerationDirection, velocityToAddEachTick);
            }

            // Next position for this step.
            Point3D nextPos = pos.copy().add(vel);

            // Check for entity collision (MTS multiparts: vehicles, placed parts).
            EntityInteractResult mtsHit = world.getMultipartEntityIntersect(pos, nextPos, ownVehicle, CollisionType.ATTACK, CollisionType.BULLET);

            // Check for vanilla entity collision (players, mobs, etc.).
            List<IWrapperEntity> extEntities = world.getEntitiesWithin(new BoundingBox(pos, nextPos));

            // Check for block collision along this tick's motion vector.
            BlockHitResult blockHit = world.getBlockHit(pos, vel);

            // Pick the closest hit among block, MTS entity, and vanilla entity.
            Point3D result = null;
            double closestDist = Double.MAX_VALUE;

            if (blockHit != null) {
                double d = pos.distanceTo(blockHit.hitPosition);
                if (d < closestDist) {
                    closestDist = d;
                    result = blockHit.hitPosition.copy();
                }
            }
            if (mtsHit != null) {
                double d = pos.distanceTo(mtsHit.position);
                if (d < closestDist) {
                    closestDist = d;
                    result = mtsHit.position.copy();
                }
            }
            if (!extEntities.isEmpty()) {
                for (IWrapperEntity entity : extEntities) {
                    if (shouldIgnoreExternalEntity(entity, gunController)) {
                        continue;
                    }
                    BoundingBoxHitResult entityHit = entity.getBounds().getIntersection(pos, nextPos);
                    if (entityHit != null) {
                        double d = pos.distanceTo(entityHit.position);
                        if (d < closestDist) {
                            closestDist = d;
                            result = entityHit.position.copy();
                        }
                    }
                }
            }
            if (result != null) {
                return result;
            }

            pos.set(nextPos);
        }

        return pos;
    }

    /**
     * Straight-line raycast along the barrel direction (normalised {@link #simVel}).
     * Used for aircraft and as a fallback when the ballistic simulation exits loaded terrain.
     * Steps one block at a time up to 512 blocks; stops at chunk boundaries, block hits, or
     * entity hits.  Uses the scratch fields {@link #simPos} and {@link #simVel} (must be
     * pre-populated).
     */
    private Point3D raycastBarrel(PartGun gun, AWrapperWorld world, AEntityF_Multipart<?> ownVehicle) {
        Point3D dir = simVel.length() > 0.001 ? simVel.copy().normalize() : simDir.copy().normalize();
        Point3D pos = simPos.copy();
        IWrapperEntity gunController = gun.getGunController();
        if (dir.length() < 0.001) {
            return pos;
        }
        for (int step = 0; step < 512; step++) {
            if (!world.chunkLoaded(pos)) {
                return pos;
            }

            Point3D nextPos = pos.copy().add(dir);

            // Check entities first (same logic as simulateBallistic).
            EntityInteractResult mtsHit = world.getMultipartEntityIntersect(pos, nextPos, ownVehicle, CollisionType.ATTACK, CollisionType.BULLET);
            List<IWrapperEntity> extEntities = world.getEntitiesWithin(new BoundingBox(pos, nextPos));
            BlockHitResult blockHit = world.getBlockHit(pos, dir);

            Point3D result = null;
            double closestDist = Double.MAX_VALUE;

            if (blockHit != null) {
                double d = pos.distanceTo(blockHit.hitPosition);
                if (d < closestDist) {
                    closestDist = d;
                    result = blockHit.hitPosition.copy();
                }
            }
            if (mtsHit != null) {
                double d = pos.distanceTo(mtsHit.position);
                if (d < closestDist) {
                    closestDist = d;
                    result = mtsHit.position.copy();
                }
            }
            if (!extEntities.isEmpty()) {
                for (IWrapperEntity entity : extEntities) {
                    if (shouldIgnoreExternalEntity(entity, gunController)) {
                        continue;
                    }
                    BoundingBoxHitResult entityHit = entity.getBounds().getIntersection(pos, nextPos);
                    if (entityHit != null) {
                        double d = pos.distanceTo(entityHit.position);
                        if (d < closestDist) {
                            closestDist = d;
                            result = entityHit.position.copy();
                        }
                    }
                }
            }
            if (result != null) {
                return result;
            }

            pos.set(nextPos);
        }
        return pos;
    }

    private static boolean shouldIgnoreExternalEntity(IWrapperEntity entity, IWrapperEntity entityToIgnore) {
        return entityToIgnore != null && entity.getID().equals(entityToIgnore.getID());
    }

    private void addGUITextEntityChain(LinkedHashSet<AEntityD_Definable<?>> entities, AEntityB_Existing startEntity) {
        AEntityB_Existing entity = startEntity;
        while (entity instanceof APart) {
            if (entity instanceof AEntityD_Definable<?>) {
                AEntityD_Definable<?> definable = (AEntityD_Definable<?>) entity;
                if (definable.hasGUIText()) {
                    entities.add(definable);
                }
            }
            entity = ((APart) entity).entityOn;
        }

        if (entity instanceof AEntityD_Definable<?>) {
            AEntityD_Definable<?> definable = (AEntityD_Definable<?>) entity;
            if (definable.hasGUIText()) {
                entities.add(definable);
            }
        }
    }

   
    private static int getCollisionGroupIndex(EntityInteractResult interactResult) {
        if (interactResult != null && interactResult.box.groupDef != null && interactResult.entity.definition.collisionGroups != null) {
            return interactResult.entity.definition.collisionGroups.indexOf(interactResult.box.groupDef) + 1;
        }
        return 0;
    }

    private static int getCollisionBoxIndex(EntityInteractResult interactResult) {
        if (interactResult != null && interactResult.box.groupDef != null && interactResult.entity.definition.collisionGroups != null) {
            int groupIndex = interactResult.entity.definition.collisionGroups.indexOf(interactResult.box.groupDef);
            if (groupIndex >= 0 && interactResult.entity.definitionCollisionBoxes.size() > groupIndex) {
                return interactResult.entity.definitionCollisionBoxes.get(groupIndex).indexOf(interactResult.box) + 1;
            }
        }
        return 0;
    }
}
