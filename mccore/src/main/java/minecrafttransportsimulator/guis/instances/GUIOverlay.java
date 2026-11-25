package minecrafttransportsimulator.guis.instances;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

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
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentAimReticle;
import minecrafttransportsimulator.guis.components.GUIComponentTimedActionProgress;
import minecrafttransportsimulator.guis.components.GUIComponentCrosshair;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentPartSlotMarkers;
import minecrafttransportsimulator.guis.components.GUIComponentPartSlotScanner;
import minecrafttransportsimulator.jsondefs.JSONBullet.Bullet;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
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
    private GUIComponentPartSlotScanner scannerPreview;
    private GUIComponentCrosshair aimingCrosshair;
    private GUIComponentAimReticle aimReticle;
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
    private final RotationMatrix simFlightOrientation = new RotationMatrix();
    private final Point3D simAccelerationMotion = new Point3D();

    @Override
    public void setupComponents() {
        super.setupComponents();
        addComponent(new GUIComponentTimedActionProgress());

        addComponent(mouseoverLabel = new GUIComponentLabel(screenWidth / 2, screenHeight / 2 + 10, ColorRGB.WHITE, "", TextAlignment.CENTERED, 1.0F));
        addComponent(gunLabel = new GUIComponentLabel(screenWidth, 0, ColorRGB.WHITE, "", TextAlignment.RIGHT_ALIGNED, 1.0F));
        gunLabel.ignoreGUILightingState = true;
        addComponent(aimReticle = new GUIComponentAimReticle(screenWidth, screenHeight));
        aimReticle.ignoreGUILightingState = true;
        // Start crosshair at screen centre; setStates() repositions it every frame.
        addComponent(aimingCrosshair = new GUIComponentCrosshair(screenWidth / 2, screenHeight / 2));
        aimingCrosshair.visible = false;
        aimingCrosshair.ignoreGUILightingState = true;
        scannerPreview = new GUIComponentPartSlotScanner();
        addComponent(new GUIComponentPartSlotMarkers(scannerPreview));
        addComponent(scannerPreview);
    }

    @Override
    public void setStates() {
        setStates(1);
    }

    @Override
    protected void setStates(float partialTicks) {
        super.setStates();
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();

        //Set gun label text, if we are controlling a gun.
        gunLabel.visible = false;
        aimingCrosshair.visible = false;
        aimingCrosshair.pendingImpactPoint = null;
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
            if (activeGunGroup != null && ConfigSystem.client.renderingSettings.gunCrosshair.value) {
                updateAimingCrosshair(activeGunGroup, player.getWorld(), partialTicks);
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

        scannerPreview.updateTarget(player, interactResult, startPosition, endPosition);
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

                //Each tracked entity gets a small Z slice so multiple prompts can overlap without fighting.
                double zOffset = 325 + entityLayer * 5D;
                if (textDef.textureNames != null && !textDef.textureNames.isEmpty()) {
                    String textureName = entity.getGUITextureName(textDef);
                    if (textureName == null || textureName.isEmpty()) {
                        continue;
                    }
                    RenderText.drawGUITexture(textureName, textDef, screenWidth, screenHeight, zOffset, alpha);
                } else {
                    String textValue = entity.getGUITextValue(textDef, partialTicks);
                    if (textValue == null || textValue.isEmpty()) {
                        continue;
                    }
                    RenderText.drawGUIText(textValue, entity, textDef, screenWidth, screenHeight, zOffset, alpha);
                }
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
     * Computes the predicted impact point from the gun's interpolated muzzle pose.
     * Screen projection is deferred to {@link GUIComponentCrosshair#render}.
     *
     * <p>Aircraft guns firing without gravity use a straight-line raycast. The ballistic
     * prediction uses the projectile's tick physics and traces it through the loaded world.
     *
     * <p>When multiple guns are in the same group the muzzle positions are averaged so the crosshair
     * points to the geometric center of the salvo.  Velocity direction is taken from the first gun.
     */
    private void updateAimingCrosshair(List<PartGun> gunGroup, AWrapperWorld world, float partialTicks) {
        PartGun firstGun = gunGroup.get(0);
        JSONMuzzle muzzle = firstGun.getActiveMuzzle();
        if (muzzle == null) return;

        // Populate muzzle position and velocity from the first gun.
        firstGun.setBulletPreviewSpawn(simPos, simVel, simOri, muzzle, partialTicks);
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
                    otherGun.setBulletPreviewSpawn(tmpPos, tmpVel, tmpOri, otherMuzzle, partialTicks);
                    sumX += tmpPos.x; sumY += tmpPos.y; sumZ += tmpPos.z;
                    count++;
                }
            }
            simPos.set(sumX / count, sumY / count, sumZ / count);
        }

        Bullet bullet = firstGun.lastLoadedBullet != null ? firstGun.lastLoadedBullet.definition.bullet : null;
        double bulletSpeed = simVel.length();
        double acceleration = bullet != null ? EntityBullet.getFlightAcceleration(bullet, bulletSpeed) : 0;
        boolean hasGravity = bullet != null && bullet.gravitationalVelocity != 0;
        boolean isAircraft = firstGun.vehicleOn != null && firstGun.vehicleOn.definition.motorized.isAircraft;
        Point3D impactPoint;

        if (bulletSpeed < 0.001 && acceleration == 0 && !hasGravity) {
            impactPoint = simPos.copy();
        } else if (bullet == null || (isAircraft && !hasGravity)) {
            impactPoint = raycastBarrel(firstGun, world, firstGun.vehicleOn);
        } else {
            impactPoint = simulateBallistic(firstGun, world, bullet, acceleration);
        }

        // Smooth changes in the calculated impact point for aircraft.
        aimingCrosshair.applySmoothing = isAircraft;

        // Store the frame-time impact point for screen projection.
        aimingCrosshair.pendingImpactPoint = impactPoint;
        aimingCrosshair.visible = true;
    }

    /**
     * Traces the projectile from the interpolated muzzle to the first hit or last simulated position.
     * Uses the launch state populated by {@link PartGun#setBulletPreviewSpawn}.
     */
    private Point3D simulateBallistic(PartGun gun, AWrapperWorld world, Bullet bullet, double acceleration) {
        long maxTicks = Math.min(EntityBullet.getFlightLifetime(bullet), 500);
        Point3D pos = simPos.copy();
        Point3D vel = simVel.copy();
        simFlightOrientation.set(simOri);
        IWrapperEntity gunController = gun.getGunController();
        AEntityF_Multipart<?> ownVehicle = gun.vehicleOn;
        boolean isBomb = gun.definition.gun.muzzleVelocity == 0;

        for (int tick = 1; tick <= maxTicks; ++tick) {
            if (!world.chunkLoaded(pos) || !world.isInsideBorder(pos)) {
                break;
            }
            EntityBullet.applyFlightForces(bullet, tick, vel, simFlightOrientation, acceleration, simAccelerationMotion);
            if (acceleration != 0) {
                EntityBullet.alignFlightOrientation(bullet, tick, isBomb, vel, simFlightOrientation);
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
     * Used for aircraft with gravity-free projectiles and when no projectile is loaded.
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
