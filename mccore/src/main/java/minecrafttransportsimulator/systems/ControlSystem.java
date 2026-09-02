package minecrafttransportsimulator.systems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.*;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPanel;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONConfigClient.ConfigJoystick;
import minecrafttransportsimulator.jsondefs.JSONConfigClient.ConfigKeyboard;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityCameraChange;
import minecrafttransportsimulator.packets.instances.PacketEntityCustomKeypress;
import minecrafttransportsimulator.packets.instances.PacketEntityInteract;
import minecrafttransportsimulator.packets.instances.PacketEntityInteractGUI;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.packets.instances.PacketPartSeat.SeatAction;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlNotification;
import minecrafttransportsimulator.packets.instances.PacketVehicleDeployment;
import minecrafttransportsimulator.packets.instances.PacketVehicleDeployment.Action;
import minecrafttransportsimulator.packets.instances.PacketVehiclePacking;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Class that handles all control operations.
 *
 * @author don_bruce
 */
public final class ControlSystem {
    private static final int NULL_COMPONENT = 999;
    private static final double INTERACTION_DISTANCE = 3.5;
    private static final double VEHICLE_DEPLOYMENT_DISTANCE = 5.0D;
    private static final int TIMED_ACTION_COMPLETION_RETRY_INTERVAL_TICKS = 5;
    private static final int VEHICLE_ACTION_VALIDATION_INTERVAL_TICKS = 5;
    private static final int VEHICLE_DEPLOYMENT_GRANT_TIMEOUT_TICKS = 40;
    private static final long DISMOUNT_CONFIRM_WINDOW_MILLIS = 3000L;
    private static boolean joysticksInhibited = false;
    private static IWrapperPlayer clientPlayer;

    private static boolean clickingLeft = false;
    private static boolean clickingRight = false;

    private static double throttleRequestLastCheck;
    private static double brakeRequestLastCheck;

    private static boolean mouseYokeEnabledLastCall;
    private static double mouseYokePosX = Double.NaN;
    private static double mouseYokePosY = Double.NaN;
    private static PartSeat dismountConfirmationSeat;
    private static long dismountConfirmationExpireTime;
    private static boolean dismountInputPressedLastCall;

    private static EntityInteractResult interactResult = null;
    private static AEntityF_Multipart<?> partInstallationEntity;
    private static BoundingBox partInstallationBox;
    private static IWrapperItemStack partInstallationStack;
    private static int partInstallationSlotIndex = -1;
    private static int partInstallationElapsedTicks;
    private static int partInstallationTime;
    private static APart partRemovalPart;
    private static BoundingBox partRemovalBox;
    private static IWrapperItemStack partRemovalToolStack;
    private static int partRemovalElapsedTicks;
    private static int partRemovalTime;
    private static int partRemovalCompletionRetryTicks;
    private static final List<VehicleDeploymentState> vehicleDeployments = new ArrayList<>();
    private static EntityVehicleF_Physics vehiclePackingVehicle;
    private static IWrapperItemStack vehiclePackingToolStack;
    private static Point3D vehiclePackingMarkerPosition;
    private static int vehiclePackingElapsedTicks;
    private static int vehiclePackingTime;
    private static int vehiclePackingCompletionRetryTicks;
    private static int vehiclePackingValidationTicks;
    private static int vehiclePackingOperationID;
    private static int nextVehiclePackingOperationID;
    private static boolean timedLeftClickInputCaptured;
    private static boolean vehicleDeploymentInputCaptured;
    private static boolean timedActionOverlayVisible;

    /**
     * Static initializer for the IWrapper inputs, as we need to iterate through the enums to initialize them
     * prior to using them in any of the methods contained in this IWrapper (cause they'll be null).
     * Joystick enums need to come first, as the Keyboard enums take them as constructor args.
     * After we initialize the keboard enums, we set their default values.
     * Once all this is done, save the results back to the disk to ensure the systems are synced.
     * Note that since this class won't be called until the world loads because we won't process inputs
     * out-of-world, it can be assumed that the ConfigSystem has already been initialized.
     */
    static {
        for (ControlsJoystick control : ControlsJoystick.values()) {
            ConfigSystem.client.controls.joystick.put(control.systemName, control.config);
        }
        for (ControlsKeyboard control : ControlsKeyboard.values()) {
            ConfigSystem.client.controls.keyboard.put(control.systemName, control.config);
        }
        for (ControlsKeyboard control : ControlsKeyboard.values()) {
            if (control.config.keyCode <= 0 && !control.config.isMouseButton) {
                control.config.keyCode = InterfaceManager.inputInterface.getKeyCodeForName(control.defaultKeyName);
            }
        }
        ConfigSystem.saveToDisk();
    }

    public static void controlGlobal(IWrapperPlayer player) {
        EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
        if (InterfaceManager.inputInterface.isLeftMouseButtonDown()) {
            if (!clickingLeft) {
                clickingLeft = true;
                handleClick(player, playerGun, true, false, false, false);
            } else if (isVehiclePackingInProgress()) {
                updateVehiclePacking(player);
            } else if (isPartRemovalInProgress()) {
                updatePartRemoval(player);
            }
        } else if (clickingLeft) {
            clickingLeft = false;
            handleClick(player, playerGun, false, true, false, false);
        }
        if (InterfaceManager.inputInterface.isRightMouseButtonDown()) {
            if (!clickingRight) {
                clickingRight = true;
                handleClick(player, playerGun, false, false, true, false);
            } else if (isPartInstallationInProgress()) {
                updatePartInstallation(player);
            }
        } else if (clickingRight) {
            clickingRight = false;
            vehicleDeploymentInputCaptured = false;
            handleClick(player, playerGun, false, false, false, true);
        }
        if (isVehicleDeploymentInProgress()) {
            updateVehicleDeployment(player);
        }
        LanguageEntry timedActionOverlayMessage = null;
        if (isPartInstallationInProgress() && partInstallationElapsedTicks < partInstallationTime) {
            timedActionOverlayMessage = LanguageSystem.GUI_PARTINSTALL_INSTALLING;
        } else if (isPartRemovalInProgress() && partRemovalElapsedTicks < partRemovalTime) {
            timedActionOverlayMessage = LanguageSystem.GUI_PARTREMOVE_REMOVING;
        } else if (isVehiclePackingInProgress() && vehiclePackingElapsedTicks < vehiclePackingTime) {
            timedActionOverlayMessage = LanguageSystem.GUI_VEHICLEPACK_PACKING;
        } else if (hasVehicleDeploymentOverlay()) {
            timedActionOverlayMessage = LanguageSystem.GUI_VEHICLEDEPLOY_DEPLOYING;
        }
        if (timedActionOverlayMessage != null) {
            InterfaceManager.clientInterface.displayOverlayMessage(timedActionOverlayMessage.getCurrentValue());
            timedActionOverlayVisible = true;
        } else if (timedActionOverlayVisible) {
            InterfaceManager.clientInterface.displayOverlayMessage("");
            timedActionOverlayVisible = false;
        }

        if (playerGun != null && playerGun.activeGun != null && !InterfaceManager.clientInterface.isGUIOpen() && ControlsKeyboard.GENERAL_RELOAD.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.RELOAD_HAND));
        }
    }

    public static boolean isPartInstallationInProgress() {
        return partInstallationEntity != null;
    }

    public static float getPartInstallationProgress(float partialTicks) {
        if (!isPartInstallationInProgress() || partInstallationTime <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (partInstallationElapsedTicks + partialTicks) / partInstallationTime));
    }

    public static boolean isPartInstallationTarget(AEntityF_Multipart<?> multipart, BoundingBox box) {
        return partInstallationEntity == multipart
                && partInstallationBox == box;
    }

    public static boolean isPartRemovalInProgress() {
        return partRemovalPart != null;
    }

    public static float getPartRemovalProgress(float partialTicks) {
        if (!isPartRemovalInProgress() || partRemovalTime <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (partRemovalElapsedTicks + partialTicks) / partRemovalTime));
    }

    public static boolean isPartRemovalTarget(AEntityF_Multipart<?> multipart) {
        return partRemovalPart == multipart;
    }

    public static boolean isVehicleDeploymentInProgress() {
        return !vehicleDeployments.isEmpty();
    }

    public static int getVehicleDeploymentCount() {
        return vehicleDeployments.size();
    }

    public static float getVehicleDeploymentProgress(int index, float partialTicks) {
        if (index < 0 || index >= vehicleDeployments.size()) {
            return 0.0F;
        }
        VehicleDeploymentState deployment = vehicleDeployments.get(index);
        if (deployment.deployTime <= 0 || deployment.operationID == 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (deployment.elapsedTicks + partialTicks) / deployment.deployTime));
    }

    public static boolean getVehicleDeploymentMarkerPosition(int index, Point3D markerPosition) {
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (index >= 0 && index < vehicleDeployments.size()) {
            VehicleDeploymentState deployment = vehicleDeployments.get(index);
            if (player != null && player.getWorld() == deployment.world) {
                markerPosition.set(deployment.markerPosition);
                return true;
            }
        }
        return false;
    }

    private static boolean hasVehicleDeploymentOverlay() {
        for (VehicleDeploymentState deployment : vehicleDeployments) {
            if (deployment.elapsedTicks < deployment.deployTime || !deployment.requiresHold) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasVehicleDeploymentInputCaptured() {
        for (VehicleDeploymentState deployment : vehicleDeployments) {
            if (deployment.inputCaptured) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVehiclePackingInProgress() {
        return vehiclePackingVehicle != null;
    }

    public static float getVehiclePackingProgress(float partialTicks) {
        if (!isVehiclePackingInProgress() || vehiclePackingTime <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (vehiclePackingElapsedTicks + partialTicks) / vehiclePackingTime));
    }

    public static boolean getVehiclePackingMarkerPosition(Point3D markerPosition) {
        if (isVehiclePackingInProgress()) {
            markerPosition.set(vehiclePackingMarkerPosition);
            return true;
        }
        return false;
    }

    public static void finishVehiclePacking(UUID vehicleID, int operationID) {
        if (isVehiclePackingInProgress()
                && vehiclePackingVehicle.uniqueUUID.equals(vehicleID)
                && vehiclePackingOperationID == operationID) {
            clearVehiclePacking();
            interactResult = null;
        }
    }

    /** Clears timed interaction state when normal controls are disabled for spectator mode. */
    public static void cancelTimedActionsForSpectator(IWrapperPlayer player) {
        if (player != null && player.isSpectator()) {
            boolean timedActionInProgress = isPartInstallationInProgress()
                    || isPartRemovalInProgress()
                    || isVehiclePackingInProgress();
            EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
            if (playerGun != null && playerGun.activeGun != null) {
                if (clickingLeft) {
                    InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.TRIGGER_OFF));
                }
                if (clickingRight) {
                    InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.AIM_OFF));
                }
            }
            if (!timedActionInProgress && interactResult != null) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(interactResult.entity, player, interactResult.box, false, false));
            }
            if (isPartInstallationInProgress()) {
                cancelPartInstallation(player);
            }
            if (isPartRemovalInProgress()) {
                cancelPartRemoval(player);
            }
            if (isVehicleDeploymentInProgress()) {
                cancelVehicleDeployment(player);
            }
            if (isVehiclePackingInProgress()) {
                cancelVehiclePacking(player);
            }
            timedLeftClickInputCaptured = false;
            vehicleDeploymentInputCaptured = false;
            clickingLeft = false;
            clickingRight = false;
            interactResult = null;
            if (timedActionOverlayVisible) {
                InterfaceManager.clientInterface.displayOverlayMessage("");
                timedActionOverlayVisible = false;
            }
        }
    }

    public static void resetMouseYoke() {
        mouseYokePosX = Double.NaN;
        mouseYokePosY = Double.NaN;
    }

    /**
     * Returns true when vanilla use-item handling should be suppressed because the player is
     * targeting an IV click hitbox.  IV handles these clicks separately in {@link #handleClick},
     * so allowing vanilla to process the same input may place or use an item on a block behind
     * the IV entity.
     */
    public static boolean shouldSuppressVanillaRightClick(IWrapperPlayer player) {
        if (vehicleDeploymentInputCaptured) {
            return true;
        }
        if (player != null && player.getWorld() != null) {
            Point3D startPosition = player.getEyePosition();
            Point3D endPosition = player.getLineOfSight(INTERACTION_DISTANCE).add(startPosition);
            return player.getWorld().getMultipartEntityIntersect(startPosition, endPosition) != null;
        }
        return false;
    }

    /**
     * Returns true when vanilla attack handling should be suppressed because left-click is being
     * used for a timed part or vehicle operation.  This prevents the same input from striking an
     * entity or block behind the IV hitbox.
     */
    public static boolean shouldSuppressVanillaLeftClick(IWrapperPlayer player) {
        if (timedLeftClickInputCaptured) {
            return true;
        }
        if (player != null && player.getWorld() != null && isHoldingPartRemovalTool(player)) {
            Point3D startPosition = player.getEyePosition();
            Point3D endPosition = player.getLineOfSight(INTERACTION_DISTANCE).add(startPosition);
            EntityInteractResult target = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);
            if (target != null && target.entity instanceof AEntityF_Multipart) {
                if (player.isSneaking()) {
                    EntityVehicleF_Physics vehicle = getVehicleForMultipart((AEntityF_Multipart<?>) target.entity);
                    return vehicle != null && vehicle.definition.motorized.packTime > 0 && vehicle.isValid;
                } else if (target.entity instanceof APart) {
                    APart part = (APart) target.entity;
                    return part.definition.generic.removeTime > 0
                            && part.isValid
                            && !part.isFake()
                            && part.canBeClicked();
                }
            }
        }
        return false;
    }

    public static void setMouseYokeEnabled(boolean enabled, boolean displayMessage) {
        ConfigSystem.client.controlSettings.mouseYoke.value = enabled;
        ConfigSystem.saveToDisk();
        resetMouseYoke();
        mouseYokeEnabledLastCall = enabled;
        if (displayMessage && InterfaceManager.clientInterface != null) {
            InterfaceManager.clientInterface.displayOverlayMessage((enabled ? LanguageSystem.INTERACT_MOUSEYOKE_ENABLED : LanguageSystem.INTERACT_MOUSEYOKE_DISABLED).getCurrentValue());
        }
    }

    public static void toggleMouseYoke() {
        setMouseYokeEnabled(!ConfigSystem.client.controlSettings.mouseYoke.value, true);
    }

    public static boolean shouldSuppressDismount(IWrapperPlayer player, boolean dismountRequested) {
        PartSeat currentSeat = getClientVehicleSeat(player);
        if (currentSeat != dismountConfirmationSeat) {
            clearDismountConfirmation();
        }

        if (!dismountRequested) {
            dismountInputPressedLastCall = false;
            return false;
        }

        if (currentSeat == null) {
            clearDismountConfirmation();
            return false;
        }

        boolean justPressed = !dismountInputPressedLastCall;
        dismountInputPressedLastCall = true;
        boolean confirmationActive = dismountConfirmationSeat == currentSeat;
        boolean confirmationValid = confirmationActive && System.currentTimeMillis() <= dismountConfirmationExpireTime;
        if (justPressed) {
            if (confirmationValid) {
                clearDismountConfirmation();
                return false;
            } else if (requiresDismountConfirmation(currentSeat)) {
                dismountConfirmationSeat = currentSeat;
                dismountConfirmationExpireTime = System.currentTimeMillis() + DISMOUNT_CONFIRM_WINDOW_MILLIS;
                if (InterfaceManager.clientInterface != null) {
                    InterfaceManager.clientInterface.displayOverlayMessage(LanguageSystem.INTERACT_VEHICLE_DISMOUNTCONFIRM.getCurrentValue());
                }
                return true;
            } else {
                clearDismountConfirmation();
                return false;
            }
        } else {
            return confirmationActive;
        }
    }

    private static void handleClick(IWrapperPlayer player, EntityPlayerGun playerGun, boolean leftClickDown, boolean leftClickUp, boolean rightClickDown, boolean rightClickUp) {
        //Either change the gun trigger state (if we are holding a gun),
        //or try to interact with entities if we are not.
        if (playerGun != null && playerGun.activeGun != null) {
            if (leftClickDown) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.TRIGGER_ON));
            } else if (leftClickUp) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.TRIGGER_OFF));
            } else if (rightClickDown) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.AIM_ON));
            } else if (rightClickUp) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.AIM_OFF));
            }
        }
        if (leftClickUp) {
            timedLeftClickInputCaptured = false;
            if (isVehiclePackingInProgress()) {
                cancelVehiclePacking(player);
                return;
            } else if (isPartRemovalInProgress()) {
                cancelPartRemoval(player);
                return;
            }
        }
        if (rightClickUp) {
            vehicleDeploymentInputCaptured = false;
            if (isPartInstallationInProgress()) {
                cancelPartInstallation(player);
                return;
            } else if (hasVehicleDeploymentInputCaptured()) {
                cancelHeldVehicleDeployment(player);
                return;
            }
        }
        if (leftClickDown || rightClickDown) {
            Point3D startPosition = player.getEyePosition();
            Point3D endPosition = player.getLineOfSight(INTERACTION_DISTANCE).add(startPosition);

            interactResult = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);
            if (interactResult != null) {
                boolean timedActionStarted = rightClickDown && startPartInstallation(player, interactResult)
                        || leftClickDown && (startVehiclePacking(player, interactResult) || startPartRemoval(player, interactResult));
                if (!timedActionStarted) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(interactResult.entity, player, interactResult.box, leftClickDown, rightClickDown));
                }
            }
        } else if (interactResult != null) {
            //Fire off un-click to entity last clicked.
            InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(interactResult.entity, player, interactResult.box, false, false));
            interactResult = null;
        }
    }

    private static boolean startPartInstallation(IWrapperPlayer player, EntityInteractResult target) {
        if (!(target.entity instanceof AEntityF_Multipart) || player.isSneaking()) {
            return false;
        }

        AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) target.entity;
        IWrapperItemStack heldStack = player.getHeldStack();
        AItemBase heldItem = heldStack.getItem();
        if (!(heldItem instanceof AItemPart)) {
            return false;
        }

        AItemPart heldPart = (AItemPart) heldItem;
        int installTime = heldPart.definition.generic.installTime;
        if (installTime <= 0 || isPartActionVehicleLocked(multipart)) {
            return false;
        }

        int slotIndex = getPartInstallationSlotIndex(multipart, target.box.localCenter);
        if (slotIndex < 0 || multipart.partsInSlots.get(slotIndex) != null) {
            return false;
        }

        JSONPartDefinition slotDefinition = multipart.definition.parts.get(slotIndex);
        if (!multipart.isVariableListTrue(slotDefinition.interactableVariables) || !heldPart.isPartValidForPackDef(slotDefinition, multipart.subDefinition, !slotDefinition.bypassSlotMinMax)) {
            return false;
        }

        if (isPartRemovalInProgress()) {
            cancelPartRemoval(player);
        }
        if (isVehicleDeploymentInProgress()) {
            cancelHeldVehicleDeployment(player);
        }
        if (isVehiclePackingInProgress()) {
            cancelVehiclePacking(player);
        }
        partInstallationEntity = multipart;
        partInstallationBox = target.box;
        partInstallationStack = heldStack.copy();
        partInstallationSlotIndex = slotIndex;
        partInstallationElapsedTicks = 0;
        partInstallationTime = installTime;
        InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(target.entity, player, target.box, false, true, false));
        return true;
    }

    private static void updatePartInstallation(IWrapperPlayer player) {
        Point3D startPosition = player.getEyePosition();
        Point3D endPosition = player.getLineOfSight(INTERACTION_DISTANCE).add(startPosition);
        EntityInteractResult currentTarget = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);
        IWrapperItemStack heldStack = player.getHeldStack();
        AItemBase heldItem = heldStack.getItem();
        JSONPartDefinition slotDefinition = partInstallationSlotIndex >= 0 && partInstallationSlotIndex < partInstallationEntity.partsInSlots.size() ? partInstallationEntity.definition.parts.get(partInstallationSlotIndex) : null;
        boolean validTarget = currentTarget != null && currentTarget.entity == partInstallationEntity && currentTarget.box.localCenter.equals(partInstallationBox.localCenter);
        boolean validItem = heldItem instanceof AItemPart && heldStack.isCompleteMatch(partInstallationStack);
        boolean validSlot = slotDefinition != null && partInstallationEntity.partsInSlots.get(partInstallationSlotIndex) == null && getPartInstallationSlotIndex(partInstallationEntity, partInstallationBox.localCenter) == partInstallationSlotIndex;
        if (!validTarget || !validItem || !validSlot || player.isSneaking() || isPartActionVehicleLocked(partInstallationEntity) || !partInstallationEntity.isVariableListTrue(slotDefinition.interactableVariables) || !((AItemPart) heldItem).isPartValidForPackDef(slotDefinition, partInstallationEntity.subDefinition, !slotDefinition.bypassSlotMinMax)) {
            cancelPartInstallation(player);
            return;
        }

        if (partInstallationElapsedTicks < partInstallationTime) {
            ++partInstallationElapsedTicks;
        }
        if (partInstallationElapsedTicks >= partInstallationTime) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(partInstallationEntity, player, partInstallationBox, false, true, true));
        }
    }

    private static void cancelPartInstallation(IWrapperPlayer player) {
        InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(partInstallationEntity, player, partInstallationBox, false, false, true));
        clearPartInstallation();
        interactResult = null;
    }

    private static void clearPartInstallation() {
        partInstallationEntity = null;
        partInstallationBox = null;
        partInstallationStack = null;
        partInstallationSlotIndex = -1;
        partInstallationElapsedTicks = 0;
        partInstallationTime = 0;
    }

    private static boolean startPartRemoval(IWrapperPlayer player, EntityInteractResult target) {
        if (!(target.entity instanceof APart) || player.isSneaking()) {
            return false;
        }

        APart part = (APart) target.entity;
        IWrapperItemStack heldStack = player.getHeldStack();
        int removeTime = part.definition.generic.removeTime;
        if (removeTime <= 0
                || !isHoldingPartRemovalTool(player)
                || !part.isValid
                || part.isFake()
                || part.isPermanent
                || !part.canBeClicked()
                || isPartActionVehicleLocked(part)
                || part.checkForRemoval(player) != null) {
            return false;
        }

        if (isPartInstallationInProgress()) {
            cancelPartInstallation(player);
        }
        if (isVehicleDeploymentInProgress()) {
            cancelHeldVehicleDeployment(player);
        }
        if (isVehiclePackingInProgress()) {
            cancelVehiclePacking(player);
        }
        partRemovalPart = part;
        partRemovalBox = target.box;
        partRemovalToolStack = heldStack.copy();
        partRemovalElapsedTicks = 0;
        partRemovalTime = removeTime;
        partRemovalCompletionRetryTicks = 0;
        timedLeftClickInputCaptured = true;
        InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(target.entity, player, target.box, true, false, false));
        return true;
    }

    private static void updatePartRemoval(IWrapperPlayer player) {
        if (!partRemovalPart.isValid) {
            cancelPartRemoval(player);
            return;
        }

        Point3D startPosition = player.getEyePosition();
        Point3D endPosition = player.getLineOfSight(INTERACTION_DISTANCE).add(startPosition);
        EntityInteractResult currentTarget = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);
        IWrapperItemStack heldStack = player.getHeldStack();
        boolean validTarget = currentTarget != null && currentTarget.entity == partRemovalPart;
        boolean validTool = isHoldingPartRemovalTool(player) && heldStack.isCompleteMatch(partRemovalToolStack);
        boolean validPart = !partRemovalPart.isFake()
                && !partRemovalPart.isPermanent
                && partRemovalPart.canBeClicked()
                && partRemovalPart.definition.generic.removeTime == partRemovalTime
                && partRemovalPart.checkForRemoval(player) == null;
        if (!validTarget || !validTool || !validPart || player.isSneaking() || isPartActionVehicleLocked(partRemovalPart)) {
            cancelPartRemoval(player);
            return;
        }

        if (partRemovalElapsedTicks < partRemovalTime) {
            ++partRemovalElapsedTicks;
        }
        if (partRemovalElapsedTicks >= partRemovalTime) {
            if (partRemovalCompletionRetryTicks > 0) {
                --partRemovalCompletionRetryTicks;
            }
            if (partRemovalCompletionRetryTicks == 0) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(partRemovalPart, player, partRemovalBox, true, false, true));
                partRemovalCompletionRetryTicks = TIMED_ACTION_COMPLETION_RETRY_INTERVAL_TICKS;
            }
        }
    }

    private static void cancelPartRemoval(IWrapperPlayer player) {
        AEntityF_Multipart<?> cancellationEntity = partRemovalPart.isValid ? partRemovalPart : partRemovalPart.entityOn;
        if (cancellationEntity != null && cancellationEntity.isValid) {
            BoundingBox cancellationBox = cancellationEntity == partRemovalPart ? partRemovalBox : cancellationEntity.boundingBox;
            InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(cancellationEntity, player, cancellationBox, false, false, true));
        }
        clearPartRemoval();
        interactResult = null;
    }

    private static void clearPartRemoval() {
        partRemovalPart = null;
        partRemovalBox = null;
        partRemovalToolStack = null;
        partRemovalElapsedTicks = 0;
        partRemovalTime = 0;
        partRemovalCompletionRetryTicks = 0;
    }

    public static void startVehicleDeployment(IWrapperPlayer player, ItemVehicle vehicleItem, Point3D blockPosition, Axis blockSide) {
        IWrapperItemStack heldStack = player.getHeldStack();
        int deployTime = vehicleItem.definition.motorized.deployTime;
        BlockHitResult currentTarget = player.getWorld().getBlockHit(player.getEyePosition(), player.getLineOfSight(VEHICLE_DEPLOYMENT_DISTANCE));
        if (deployTime <= 0
                || heldStack.getItem() != vehicleItem
                || currentTarget == null
                || !currentTarget.blockPosition.equals(blockPosition)
                || currentTarget.side != blockSide) {
            return;
        }

        if (isPartInstallationInProgress()) {
            cancelPartInstallation(player);
        }
        if (isPartRemovalInProgress()) {
            cancelPartRemoval(player);
        }
        if (isVehiclePackingInProgress()) {
            cancelVehiclePacking(player);
        }

        vehicleDeployments.add(new VehicleDeploymentState(vehicleItem, player.getWorld(), heldStack.copy(), blockPosition, blockSide, currentTarget.hitPosition, deployTime, player.getYaw()));
        clickingRight = true;
        vehicleDeploymentInputCaptured = true;
    }

    public static boolean authorizeVehicleDeployment(Point3D blockPosition, Axis blockSide, int operationID, boolean requiresHold) {
        for (VehicleDeploymentState deployment : vehicleDeployments) {
            if (deployment.operationID == operationID) {
                return true;
            }
        }

        Iterator<VehicleDeploymentState> iterator = vehicleDeployments.iterator();
        while (iterator.hasNext()) {
            VehicleDeploymentState deployment = iterator.next();
            if (deployment.operationID == 0
                    && deployment.blockPosition.equals(blockPosition)
                    && deployment.blockSide == blockSide) {
                if (requiresHold && !deployment.inputCaptured) {
                    deployment.removePreview();
                    iterator.remove();
                    return false;
                }
                deployment.operationID = operationID;
                deployment.requiresHold = requiresHold;
                deployment.elapsedTicks = requiresHold ? 0 : Math.min(deployment.grantWaitTicks, deployment.deployTime);
                deployment.completionRetryTicks = 0;
                deployment.validationTicks = 0;
                deployment.grantWaitTicks = 0;
                deployment.preview = new EntityVehiclePreview(deployment.world, deployment.item, deployment.stack, deployment.blockPosition, deployment.yaw);
                deployment.world.addEntity(deployment.preview);
                return true;
            }
        }
        return false;
    }

    public static void finishVehicleDeployment(Point3D blockPosition, Axis blockSide, int operationID) {
        Iterator<VehicleDeploymentState> iterator = vehicleDeployments.iterator();
        while (iterator.hasNext()) {
            VehicleDeploymentState deployment = iterator.next();
            if (deployment.blockPosition.equals(blockPosition)
                    && deployment.blockSide == blockSide
                    && (operationID == 0 ? deployment.operationID == 0 : deployment.operationID == operationID)) {
                deployment.removePreview();
                iterator.remove();
                return;
            }
        }
    }

    private static void updateVehicleDeployment(IWrapperPlayer player) {
        Iterator<VehicleDeploymentState> iterator = vehicleDeployments.iterator();
        while (iterator.hasNext()) {
            VehicleDeploymentState deployment = iterator.next();
            if (deployment.operationID == 0) {
                if (++deployment.grantWaitTicks >= VEHICLE_DEPLOYMENT_GRANT_TIMEOUT_TICKS) {
                    deployment.removePreview();
                    iterator.remove();
                }
                continue;
            }
            if (deployment.elapsedTicks < deployment.deployTime) {
                ++deployment.elapsedTicks;
            }
            if (!deployment.requiresHold) {
                continue;
            }

            IWrapperItemStack heldStack = player.getHeldStack();
            BlockHitResult currentTarget = player.getWorld().getBlockHit(player.getEyePosition(), player.getLineOfSight(VEHICLE_DEPLOYMENT_DISTANCE));
            boolean validTarget = player.getWorld() == deployment.world
                    && deployment.inputCaptured
                    && currentTarget != null
                    && currentTarget.blockPosition.equals(deployment.blockPosition)
                    && currentTarget.side == deployment.blockSide;
            boolean validItem = heldStack.getItem() == deployment.item
                    && heldStack.isCompleteMatch(deployment.stack)
                    && deployment.item.definition.motorized.deployTime == deployment.deployTime;
            if (!validTarget || !validItem) {
                cancelVehicleDeployment(player, deployment);
                iterator.remove();
                interactResult = null;
                continue;
            }
            deployment.markerPosition.set(currentTarget.hitPosition);

            if (deployment.elapsedTicks < deployment.deployTime
                    && ++deployment.validationTicks >= VEHICLE_ACTION_VALIDATION_INTERVAL_TICKS) {
                InterfaceManager.packetInterface.sendToServer(new PacketVehicleDeployment(player, deployment.blockPosition, deployment.blockSide, Action.HEARTBEAT, deployment.operationID));
                deployment.validationTicks = 0;
            }
            if (deployment.elapsedTicks >= deployment.deployTime) {
                if (deployment.completionRetryTicks > 0) {
                    --deployment.completionRetryTicks;
                }
                if (deployment.completionRetryTicks == 0) {
                    InterfaceManager.packetInterface.sendToServer(new PacketVehicleDeployment(player, deployment.blockPosition, deployment.blockSide, Action.COMPLETE, deployment.operationID));
                    deployment.completionRetryTicks = TIMED_ACTION_COMPLETION_RETRY_INTERVAL_TICKS;
                }
            }
        }
    }

    private static boolean isWithinVehicleDeploymentDistance(IWrapperPlayer player, Point3D blockPosition) {
        Point3D eyePosition = player.getEyePosition();
        double deltaX = eyePosition.x < blockPosition.x ? blockPosition.x - eyePosition.x : eyePosition.x > blockPosition.x + 1.0D ? eyePosition.x - blockPosition.x - 1.0D : 0.0D;
        double deltaY = eyePosition.y < blockPosition.y ? blockPosition.y - eyePosition.y : eyePosition.y > blockPosition.y + 1.0D ? eyePosition.y - blockPosition.y - 1.0D : 0.0D;
        double deltaZ = eyePosition.z < blockPosition.z ? blockPosition.z - eyePosition.z : eyePosition.z > blockPosition.z + 1.0D ? eyePosition.z - blockPosition.z - 1.0D : 0.0D;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= VEHICLE_DEPLOYMENT_DISTANCE * VEHICLE_DEPLOYMENT_DISTANCE;
    }

    private static void cancelVehicleDeployment(IWrapperPlayer player) {
        for (VehicleDeploymentState deployment : vehicleDeployments) {
            if (deployment.requiresHold) {
                cancelVehicleDeployment(player, deployment);
            } else {
                deployment.removePreview();
            }
        }
        vehicleDeployments.clear();
        interactResult = null;
    }

    private static void cancelHeldVehicleDeployment(IWrapperPlayer player) {
        Iterator<VehicleDeploymentState> iterator = vehicleDeployments.iterator();
        while (iterator.hasNext()) {
            VehicleDeploymentState deployment = iterator.next();
            deployment.inputCaptured = false;
            if (deployment.requiresHold && deployment.operationID != 0) {
                cancelVehicleDeployment(player, deployment);
                iterator.remove();
            }
        }
        interactResult = null;
    }

    private static void cancelVehicleDeployment(IWrapperPlayer player, VehicleDeploymentState deployment) {
        if (deployment.operationID != 0 && deployment.requiresHold) {
            InterfaceManager.packetInterface.sendToServer(new PacketVehicleDeployment(player, deployment.blockPosition, deployment.blockSide, Action.CANCEL, deployment.operationID));
        }
        deployment.removePreview();
    }

    private static boolean startVehiclePacking(IWrapperPlayer player, EntityInteractResult target) {
        if (!(target.entity instanceof AEntityF_Multipart) || !player.isSneaking() || !isHoldingPartRemovalTool(player)) {
            return false;
        }

        EntityVehicleF_Physics vehicle = getVehicleForMultipart((AEntityF_Multipart<?>) target.entity);
        if (vehicle == null || !canPackVehicle(vehicle, player)) {
            return false;
        }

        if (isPartInstallationInProgress()) {
            cancelPartInstallation(player);
        }
        if (isPartRemovalInProgress()) {
            cancelPartRemoval(player);
        }
        if (isVehicleDeploymentInProgress()) {
            cancelHeldVehicleDeployment(player);
        }
        vehiclePackingVehicle = vehicle;
        vehiclePackingToolStack = player.getHeldStack().copy();
        vehiclePackingMarkerPosition = target.position.copy();
        vehiclePackingElapsedTicks = 0;
        vehiclePackingTime = vehicle.definition.motorized.packTime;
        vehiclePackingCompletionRetryTicks = 0;
        vehiclePackingValidationTicks = 0;
        vehiclePackingOperationID = ++nextVehiclePackingOperationID;
        if (vehiclePackingOperationID == 0) {
            vehiclePackingOperationID = ++nextVehiclePackingOperationID;
        }
        timedLeftClickInputCaptured = true;
        InterfaceManager.packetInterface.sendToServer(new PacketVehiclePacking(player, vehicle, PacketVehiclePacking.Action.START, vehiclePackingOperationID));
        return true;
    }

    private static void updateVehiclePacking(IWrapperPlayer player) {
        if (!vehiclePackingVehicle.isValid) {
            cancelVehiclePacking(player);
            return;
        }

        Point3D startPosition = player.getEyePosition();
        Point3D endPosition = player.getLineOfSight(INTERACTION_DISTANCE).add(startPosition);
        EntityInteractResult currentTarget = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);
        IWrapperItemStack heldStack = player.getHeldStack();
        boolean validTarget = currentTarget != null
                && currentTarget.entity instanceof AEntityF_Multipart
                && getVehicleForMultipart((AEntityF_Multipart<?>) currentTarget.entity) == vehiclePackingVehicle;
        boolean validTool = isHoldingPartRemovalTool(player) && heldStack.isCompleteMatch(vehiclePackingToolStack);
        if (!validTarget
                || !validTool
                || !canPackVehicle(vehiclePackingVehicle, player)
                || vehiclePackingVehicle.definition.motorized.packTime != vehiclePackingTime) {
            cancelVehiclePacking(player);
            return;
        }

        vehiclePackingMarkerPosition.set(currentTarget.position);
        if (vehiclePackingElapsedTicks < vehiclePackingTime) {
            ++vehiclePackingElapsedTicks;
        }
        if (vehiclePackingElapsedTicks < vehiclePackingTime
                && ++vehiclePackingValidationTicks >= VEHICLE_ACTION_VALIDATION_INTERVAL_TICKS) {
            InterfaceManager.packetInterface.sendToServer(new PacketVehiclePacking(player, vehiclePackingVehicle, PacketVehiclePacking.Action.HEARTBEAT, vehiclePackingOperationID));
            vehiclePackingValidationTicks = 0;
        }
        if (vehiclePackingElapsedTicks >= vehiclePackingTime) {
            if (vehiclePackingCompletionRetryTicks > 0) {
                --vehiclePackingCompletionRetryTicks;
            }
            if (vehiclePackingCompletionRetryTicks == 0) {
                InterfaceManager.packetInterface.sendToServer(new PacketVehiclePacking(player, vehiclePackingVehicle, PacketVehiclePacking.Action.COMPLETE, vehiclePackingOperationID));
                vehiclePackingCompletionRetryTicks = TIMED_ACTION_COMPLETION_RETRY_INTERVAL_TICKS;
            }
        }
    }

    private static void cancelVehiclePacking(IWrapperPlayer player) {
        if (vehiclePackingVehicle != null) {
            InterfaceManager.packetInterface.sendToServer(new PacketVehiclePacking(player, vehiclePackingVehicle, PacketVehiclePacking.Action.CANCEL, vehiclePackingOperationID));
        }
        clearVehiclePacking();
        interactResult = null;
    }

    private static void clearVehiclePacking() {
        vehiclePackingVehicle = null;
        vehiclePackingToolStack = null;
        vehiclePackingMarkerPosition = null;
        vehiclePackingElapsedTicks = 0;
        vehiclePackingTime = 0;
        vehiclePackingCompletionRetryTicks = 0;
        vehiclePackingValidationTicks = 0;
        vehiclePackingOperationID = 0;
    }

    private static boolean canPackVehicle(EntityVehicleF_Physics vehicle, IWrapperPlayer player) {
        return vehicle.definition.motorized.packTime > 0
                && vehicle.isValid
                && player.isSneaking()
                && !vehicle.lockedVar.isActive
                && (!ConfigSystem.settings.general.opPickupVehiclesOnly.value || player.isOP())
                && (!ConfigSystem.settings.general.creativePickupVehiclesOnly.value || player.isCreative());
    }

    private static boolean isHoldingPartRemovalTool(IWrapperPlayer player) {
        return player.isHoldingItemType(ItemComponentType.WRENCH) || player.isHoldingItemType(ItemComponentType.SCREWDRIVER);
    }

    private static EntityVehicleF_Physics getVehicleForMultipart(AEntityF_Multipart<?> multipart) {
        return multipart instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) multipart : (multipart instanceof APart ? ((APart) multipart).vehicleOn : null);
    }

    private static int getPartInstallationSlotIndex(AEntityF_Multipart<?> multipart, Point3D localCenter) {
        for (Entry<BoundingBox, JSONPartDefinition> slotEntry : multipart.partSlotBoxes.entrySet()) {
            if (slotEntry.getKey().localCenter.equals(localCenter)) {
                return multipart.definition.parts.indexOf(slotEntry.getValue());
            }
        }
        return -1;
    }

    private static boolean isPartActionVehicleLocked(AEntityF_Multipart<?> multipart) {
        EntityVehicleF_Physics vehicle = getVehicleForMultipart(multipart);
        return vehicle != null && vehicle.lockedVar.isActive;
    }

    private static class VehicleDeploymentState {
        private final ItemVehicle item;
        private final AWrapperWorld world;
        private final IWrapperItemStack stack;
        private final Point3D blockPosition;
        private final Axis blockSide;
        private final Point3D markerPosition;
        private final int deployTime;
        private final float yaw;
        private int elapsedTicks;
        private int completionRetryTicks;
        private int validationTicks;
        private int grantWaitTicks;
        private int operationID;
        private boolean requiresHold = true;
        private boolean inputCaptured = true;
        private EntityVehiclePreview preview;

        private VehicleDeploymentState(ItemVehicle item, AWrapperWorld world, IWrapperItemStack stack, Point3D blockPosition, Axis blockSide, Point3D markerPosition, int deployTime, float yaw) {
            this.item = item;
            this.world = world;
            this.stack = stack;
            this.blockPosition = blockPosition.copy();
            this.blockSide = blockSide;
            this.markerPosition = markerPosition.copy();
            this.deployTime = deployTime;
            this.yaw = yaw;
        }

        private void removePreview() {
            if (preview != null && preview.isValid) {
                preview.remove();
            }
            preview = null;
        }
    }

    private static PartSeat getClientVehicleSeat(IWrapperPlayer player) {
        if (player == null) {
            return null;
        }
        AEntityB_Existing ridingEntity = player.getEntityRiding();
        if (ridingEntity instanceof PartSeat) {
            PartSeat seat = (PartSeat) ridingEntity;
            if (seat.vehicleOn != null) {
                return seat;
            }
        }
        return null;
    }

    private static boolean requiresDismountConfirmation(PartSeat seat) {
        double dismountSafetySpeed = ConfigSystem.client.controlSettings.DismountSafteySpeed.value;
        return dismountSafetySpeed <= 0 || seat.vehicleOn.velocity * 20D > dismountSafetySpeed;
    }

    private static void clearDismountConfirmation() {
        dismountConfirmationSeat = null;
        dismountConfirmationExpireTime = 0;
    }

    public static void controlMultipart(AEntityF_Multipart<?> multipart, boolean isPlayerController, double mouseXDelta, double mouseYDelta) {
        clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
        if (multipart instanceof EntityVehicleF_Physics) {
            EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) multipart;
            if (vehicle.definition.motorized.isAircraft) {
                controlAircraft(vehicle, isPlayerController, mouseXDelta, mouseYDelta);
            } else {
                controlGroundVehicle(vehicle, isPlayerController);
            }
        } else if (multipart instanceof EntityPlacedPart) {
            controlCamera(ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsKeyboard.CAR_CHANGEVIEW, ControlsJoystick.CAR_LOOK_UD, ControlsJoystick.CAR_LOOK_LR);
            rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
            controlGun(multipart, ControlsKeyboard.CAR_GUN_FIRE, ControlsKeyboard.CAR_GUN_SWITCH);
        }

        if (ControlsKeyboard.GENERAL_CUSTOM1.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 1, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM1.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 1, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM2.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 2, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM2.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 2, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM3.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 3, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM3.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 3, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM4.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 4, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM4.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 4, false));
        }
		if (ControlsKeyboard.GENERAL_CUSTOM5.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 5, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM5.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 5, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM6.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 6, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM6.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 6, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM7.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 7, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM7.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 7, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM8.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 8, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM8.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 8, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM9.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 9, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM9.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 9, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM10.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 10, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM10.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 10, false));
        }
    }

    private static void controlCamera(ControlsKeyboard zoomIn, ControlsKeyboard zoomOut, ControlsKeyboard changeView, ControlsJoystick viewUD, ControlsJoystick viewLR) {
        AEntityB_Existing riding = clientPlayer.getEntityRiding();
        if (riding instanceof PartSeat) {
            PartSeat sittingSeat = (PartSeat) riding;
            if (zoomIn.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartSeat(sittingSeat, SeatAction.ZOOM_IN));
            }
            if (zoomOut.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartSeat(sittingSeat, SeatAction.ZOOM_OUT));
            }
            if (changeView.isPressed()) {
            	InterfaceManager.packetInterface.sendToServer(new PacketEntityCameraChange(sittingSeat));
            }
            if (!(viewLR.isJoystickActive() || viewUD.isJoystickActive())) {
                riding.hasHeadTracking = false;
                riding.headTrackingOrientation.set(0, 0, 0);
            } else {
                riding.hasHeadTracking = true;
                riding.headTrackingOrientation.x = -(viewUD.getAxisState(true) - 0.5) * 170;
                riding.headTrackingOrientation.y = -(viewLR.getAxisState(true) - 0.5) * 180;
            }
        }
    }

    private static void controlFreecam(ControlsKeyboard camLock) {
        if (camLock.isPressed()) {
            ConfigSystem.client.renderingSettings.freecam_3P.value = !ConfigSystem.client.renderingSettings.freecam_3P.value;
            ConfigSystem.saveToDisk();
        }
    }

    private static void rotateCamera(ControlsJoystick lookR, ControlsJoystick lookL, ControlsJoystick lookU, ControlsJoystick lookD, ControlsJoystick lookA) {
        //TODO this causes yaw de-syncs.
        if (lookR.isPressed()) {
            clientPlayer.setYaw(clientPlayer.getYaw() - 3);
        }
        if (lookL.isPressed()) {
            clientPlayer.setYaw(clientPlayer.getYaw() + 3);
        }
        if (lookU.isPressed()) {
            clientPlayer.setPitch(clientPlayer.getPitch() - 3);
        }
        if (lookD.isPressed()) {
            clientPlayer.setPitch(clientPlayer.getPitch() + 3);
        }

        float pollData = lookA.getMultistateValue();
        if (pollData != 0) {
            if (pollData >= 0.125F && pollData <= 0.375F) {
                clientPlayer.setPitch(clientPlayer.getPitch() + 3);
            }
            if (pollData >= 0.375F && pollData <= 0.625F) {
                clientPlayer.setYaw(clientPlayer.getYaw() - 3);
            }
            if (pollData >= 0.625F && pollData <= 0.875F) {
                clientPlayer.setPitch(clientPlayer.getPitch() - 3);
            }
            if (pollData >= 0.875F || pollData <= 0.125F) {
                clientPlayer.setYaw(clientPlayer.getYaw() + 3);
            }
        }
    }

    private static void controlBrake(EntityVehicleF_Physics vehicle, ControlsJoystick joystickBrakeAxis, ControlsJoystick joystickBrakeButton, ControlsKeyboard keyboardBrakeButton, ControlsKeyboard parkingBrakeButton) {
        if (parkingBrakeButton.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.parkingBrakeVar));
        }
        double brakeValue = joystickBrakeAxis.isJoystickActive() ? joystickBrakeAxis.getAxisState(true) : ((joystickBrakeButton.isPressed() || keyboardBrakeButton.isPressed()) ? EntityVehicleF_Physics.MAX_BRAKE : 0);
        if (brakeValue != brakeRequestLastCheck) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle.brakeVar, brakeValue));
        }
        brakeRequestLastCheck = brakeValue;
    }

    private static void controlGun(AEntityF_Multipart<?> multipart, ControlsKeyboard gunTrigger, ControlsKeyboard gunSwitch) {
        boolean gunSwitchPressedThisScan = gunSwitch.isPressed();
        for (APart part : multipart.allParts) {
            if (part instanceof PartGun) {
                PartGun gun = (PartGun) part;
                if (clientPlayer.equals(gun.getGunController())) {
                    if (gunTrigger.isPressed()) {
                        InterfaceManager.packetInterface.sendToServer(new PacketPartGun(gun, PacketPartGun.Request.TRIGGER_ON));
                    } else {
                        InterfaceManager.packetInterface.sendToServer(new PacketPartGun(gun, PacketPartGun.Request.TRIGGER_OFF));
                    }
                }
            } else if (part instanceof PartSeat) {
                if (gunSwitchPressedThisScan) {
                    if (clientPlayer.equals(part.rider)) {
                        InterfaceManager.packetInterface.sendToServer(new PacketPartSeat((PartSeat) part, SeatAction.CHANGE_GUN));
                    }
                }
            }
        }
    }

    private static void controlPanel(EntityVehicleF_Physics vehicle, ControlsKeyboard panel) {
        if (panel.isPressed()) {
            if (vehicle.canPlayerStartEngines(clientPlayer)) {
                if (AGUIBase.activeInputGUI instanceof GUIPanel && !AGUIBase.activeInputGUI.editingText) {
                    AGUIBase.activeInputGUI.close();
                } else if (!InterfaceManager.clientInterface.isGUIOpen()) {
                    new GUIPanel(vehicle);
                }
            }
        }
    }

    private static void controlRadio(EntityVehicleF_Physics vehicle, ControlsKeyboard radio) {
        if (radio.isPressed() && vehicle.hasRadio()) {
            if (AGUIBase.activeInputGUI instanceof GUIRadio) {
                AGUIBase.activeInputGUI.close();
            } else if (!InterfaceManager.clientInterface.isGUIOpen()) {
                new GUIRadio(vehicle.radio);
                InterfaceManager.packetInterface.sendToServer(new PacketEntityInteractGUI(vehicle, InterfaceManager.clientInterface.getClientPlayer(), true));
            }
        }
    }

    private static void controlJoystick(EntityVehicleF_Physics vehicle, ControlsKeyboard joystickInhibit) {
        if (joystickInhibit.isPressed()) {
            joysticksInhibited = !joysticksInhibited;
        }
    }

    private static void controlControlSurface(EntityVehicleF_Physics vehicle, ControlsJoystick axis, ControlsKeyboard increment, ControlsKeyboard decrement, double rate, double bounds, ComputedVariable variable, double dampenRate) {
        if (axis.isJoystickActive()) {
            double axisValue = axis.getAxisState(false);
            if (axisValue == 0) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(variable, 0));
            } else {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(variable, bounds * (-1 + 2 * axisValue)));
            }
        } else {
            if (increment.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, rate * (variable.currentValue < 0 ? 2 : 1), -bounds, bounds));
                InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(vehicle, clientPlayer));
            } else if (decrement.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, -rate * (variable.currentValue > 0 ? 2 : 1), -bounds, bounds));
                InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(vehicle, clientPlayer));
            } else if (clientPlayer.equals(vehicle.lastController)) {
                if (variable.currentValue > dampenRate) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, -dampenRate, 0, bounds));
                } else if (variable.currentValue < -dampenRate) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, dampenRate, -bounds, 0));
                } else if (variable.currentValue != 0) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(variable, 0));
                }
            }
        }
    }

    private static void controlControlTrim(EntityVehicleF_Physics vehicle, ControlsJoystick increment, ControlsJoystick decrement, double bounds, ComputedVariable variable) {
        if (increment.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, 0.1, -bounds, bounds));
        } else if (decrement.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, -0.1, -bounds, bounds));
        }
    }

    private static boolean controlMouseYoke(EntityVehicleF_Physics aircraft, double mouseXDelta, double mouseYDelta) {
        boolean mouseYokeEnabled = ConfigSystem.client.controlSettings.mouseYoke.value && !ConfigSystem.client.controlSettings.arcadeMode.value;
        if (mouseYokeEnabled != mouseYokeEnabledLastCall) {
            resetMouseYoke();
            mouseYokeEnabledLastCall = mouseYokeEnabled;
        }
        if (!mouseYokeEnabled) {
            return false;
        }

        long packedDisplaySize = InterfaceManager.clientInterface.getPackedDisplaySize();
        int screenWidth = (int) (packedDisplaySize >> Integer.SIZE);
        int screenHeight = (int) packedDisplaySize;
        if (screenWidth <= 0 || screenHeight <= 0) {
            return false;
        }

        double halfWidth = screenWidth / 2D;
        double halfHeight = screenHeight / 2D;
        if (Double.isNaN(mouseYokePosX) || Double.isNaN(mouseYokePosY)) {
            mouseYokePosX = halfWidth;
            mouseYokePosY = halfHeight;
        }

        double pitchBounds = EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE;
        double rollBounds = aircraft.definition.motorized.isBlimp ? EntityVehicleF_Physics.MAX_RUDDER_ANGLE : EntityVehicleF_Physics.MAX_AILERON_ANGLE;
        double mouseRate = ConfigSystem.client.controlSettings.mouseYokeRate.value;
        if (mouseRate > 0) {
            mouseYokePosX += mouseXDelta * mouseRate * halfWidth / rollBounds;
            mouseYokePosY += mouseYDelta * mouseRate * halfHeight / pitchBounds;
        }

        mouseYokePosX = Math.max(0, Math.min(screenWidth, mouseYokePosX));
        mouseYokePosY = Math.max(0, Math.min(screenHeight, mouseYokePosY));

        double rollInput = rollBounds * (halfWidth - mouseYokePosX) / halfWidth;
        double pitchInput = pitchBounds * (mouseYokePosY - halfHeight) / halfHeight;

        ComputedVariable rollVariable = aircraft.definition.motorized.isBlimp ? aircraft.rudderInputVar : aircraft.aileronInputVar;
        if (Math.abs(rollInput - rollVariable.currentValue) > 0.001) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(rollVariable, rollInput));
        }
        if (Math.abs(pitchInput - aircraft.elevatorInputVar.currentValue) > 0.001) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.elevatorInputVar, pitchInput));
        }
        return true;
    }

    private static void controlAircraft(EntityVehicleF_Physics aircraft, boolean isPlayerController, double mouseXDelta, double mouseYDelta) {
        controlCamera(ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsKeyboard.AIRCRAFT_CHANGEVIEW, ControlsJoystick.AIRCRAFT_LOOK_UD, ControlsJoystick.AIRCRAFT_LOOK_LR);
        rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
        controlFreecam(ControlsKeyboard.AIRCRAFT_CAMLOCK);
        controlGun(aircraft, ControlsKeyboard.AIRCRAFT_GUN_FIRE, ControlsKeyboard.AIRCRAFT_GUN_SWITCH);
        controlRadio(aircraft, ControlsKeyboard.AIRCRAFT_RADIO);
        controlJoystick(aircraft, ControlsKeyboard.AIRCRAFT_JS_INHIBIT);

        if (!isPlayerController) {
            resetMouseYoke();
            return;
        }

        if (ControlsKeyboard.AIRCRAFT_ARCADE.isPressed()) {
            ConfigSystem.client.controlSettings.arcadeMode.value = !ConfigSystem.client.controlSettings.arcadeMode.value;
            ConfigSystem.saveToDisk();
            InterfaceManager.clientInterface.displayOverlayMessage((ConfigSystem.client.controlSettings.arcadeMode.value ? LanguageSystem.INTERACT_ARCADEMODE_ENABLED : LanguageSystem.INTERACT_ARCADEMODE_DISABLED).getCurrentValue());
        }

        //Open or close the panel.
        controlPanel(aircraft, ControlsKeyboard.AIRCRAFT_PANEL);

        //Check brake status.
        controlBrake(aircraft, ControlsJoystick.AIRCRAFT_BRAKE, ControlsJoystick.AIRCRAFT_BRAKE_DIGITAL, ControlsKeyboard.AIRCRAFT_BRAKE, ControlsKeyboard.AIRCRAFT_PARK);

        //Check for thrust reverse button.
        if (ControlsJoystick.AIRCRAFT_REVERSE.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(aircraft.reverseThrustVar));
        }

        //Check for gear button.
        if (ControlsJoystick.AIRCRAFT_GEAR.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(aircraft.retractGearVar));
        }

        //Increment or decrement throttle.
        if (ControlsJoystick.AIRCRAFT_THROTTLE.isJoystickActive()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.throttleVar, ControlsJoystick.AIRCRAFT_THROTTLE.getAxisState(true) * EntityVehicleF_Physics.MAX_THROTTLE));
        } else {
            if (ControlsKeyboard.AIRCRAFT_THROTTLE_U.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(aircraft.throttleVar, EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
            }
            if (ControlsKeyboard.AIRCRAFT_THROTTLE_D.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(aircraft.throttleVar, -EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
            }
        }

        //Check flaps.
        if (aircraft.definition.motorized.flapNotches != null && !aircraft.definition.motorized.flapNotches.isEmpty()) {
            if (ControlsKeyboard.AIRCRAFT_FLAPS_D.isPressed()) {
                int currentFlapSetting = aircraft.definition.motorized.flapNotches.indexOf((float) aircraft.flapDesiredAngleVar.currentValue);
                if (currentFlapSetting == -1) {
                    //Get next-highest notch since we're going down.
                    for (int i = 0; i < aircraft.definition.motorized.flapNotches.size(); ++i) {
                        float flapNotch = aircraft.definition.motorized.flapNotches.get(i);
                        if (flapNotch > aircraft.flapDesiredAngleVar.currentValue) {
                            currentFlapSetting = i;
                            break;
                        }
                    }
                }
                if (currentFlapSetting != -1 && currentFlapSetting + 1 < aircraft.definition.motorized.flapNotches.size()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.flapDesiredAngleVar, aircraft.definition.motorized.flapNotches.get(currentFlapSetting + 1)));
                }
            } else if (ControlsKeyboard.AIRCRAFT_FLAPS_U.isPressed()) {
                int currentFlapSetting = aircraft.definition.motorized.flapNotches.indexOf((float) aircraft.flapDesiredAngleVar.currentValue);
                if (currentFlapSetting == -1) {
                    //Get next-lowest notch since we're going up.
                    for (int i = aircraft.definition.motorized.flapNotches.size() - 1; i <= 0; --i) {
                        float flapNotch = aircraft.definition.motorized.flapNotches.get(i);
                        if (flapNotch < aircraft.flapDesiredAngleVar.currentValue) {
                            currentFlapSetting = i;
                            break;
                        }
                    }
                }
                if (currentFlapSetting > 0) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.flapDesiredAngleVar, aircraft.definition.motorized.flapNotches.get(currentFlapSetting - 1)));
                }
            }
        }

        //Arcade mode: enabled via config, uses mouse-based flight controller (War Thunder style).
        //Keyboard overrides still work on top of the mouse autopilot.
        //Excluded for blimps (different control scheme).
        boolean hasRotorPropeller = false;
        for (APart part : aircraft.allParts) {
            if (part instanceof PartPropeller && ((PartPropeller) part).definition.propeller.isRotor) {
                hasRotorPropeller = true;
                break;
            }
        }
        boolean useMouseFlight = ConfigSystem.client.controlSettings.arcadeMode.value
                && !aircraft.definition.motorized.isBlimp
                && !ControlsJoystick.AIRCRAFT_PITCH.isJoystickActive()
                && !ControlsJoystick.AIRCRAFT_ROLL.isJoystickActive()
                && !ControlsJoystick.AIRCRAFT_YAW.isJoystickActive();

        if (useMouseFlight) {
            //Activate mouse flight if not already active.  Refresh it if the
            //installed parts changed the aircraft between rotor and fixed-wing.
            if (MouseFlightController.isMouseFlightActive && MouseFlightController.isHelicopter != hasRotorPropeller) {
                MouseFlightController.deactivate();
            }
            if (!MouseFlightController.isMouseFlightActive) {
                MouseFlightController.activate(aircraft, hasRotorPropeller);
            }

            //Check which axes are being overridden by keyboard.
            boolean keyboardYaw = ControlsKeyboard.AIRCRAFT_YAW_R.isPressed() || ControlsKeyboard.AIRCRAFT_YAW_L.isPressed();
            boolean keyboardPitch = ControlsKeyboard.AIRCRAFT_PITCH_U.isPressed() || ControlsKeyboard.AIRCRAFT_PITCH_D.isPressed();
            boolean keyboardRoll = ControlsKeyboard.AIRCRAFT_ROLL_R.isPressed() || ControlsKeyboard.AIRCRAFT_ROLL_L.isPressed();

            //Feed stored mouse deltas to the mouse flight controller.
            //Keyboard override flags tell the autopilot to skip those axes.
            MouseFlightController.update(aircraft, MouseFlightController.storedYawDelta, MouseFlightController.storedPitchDelta,
                    keyboardYaw, keyboardPitch, keyboardRoll);
            MouseFlightController.storedYawDelta = 0;
            MouseFlightController.storedPitchDelta = 0;

            //For axes overridden by keyboard, use the standard keyboard control.
            if (keyboardYaw) {
                controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_YAW, ControlsKeyboard.AIRCRAFT_YAW_R, ControlsKeyboard.AIRCRAFT_YAW_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, aircraft.rudderInputVar, EntityVehicleF_Physics.RUDDER_DAMPEN_RATE);
            }
            if (keyboardPitch) {
                controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_PITCH, ControlsKeyboard.AIRCRAFT_PITCH_U, ControlsKeyboard.AIRCRAFT_PITCH_D, ConfigSystem.client.controlSettings.flightControlRate.value, EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, aircraft.elevatorInputVar, EntityVehicleF_Physics.ELEVATOR_DAMPEN_RATE);
            }
            if (keyboardRoll) {
                controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_ROLL, ControlsKeyboard.AIRCRAFT_ROLL_R, ControlsKeyboard.AIRCRAFT_ROLL_L, ConfigSystem.client.controlSettings.flightControlRate.value, EntityVehicleF_Physics.MAX_AILERON_ANGLE, aircraft.aileronInputVar, EntityVehicleF_Physics.AILERON_DAMPEN_RATE);
            }
        } else {
            //Deactivate mouse flight if it was active.
            if (MouseFlightController.isMouseFlightActive) {
                MouseFlightController.deactivate();
            }

            //Check yaw.  Blimps don't use rudder keys.
            if (!aircraft.definition.motorized.isBlimp) {
                controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_YAW, ControlsKeyboard.AIRCRAFT_YAW_R, ControlsKeyboard.AIRCRAFT_YAW_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, aircraft.rudderInputVar, EntityVehicleF_Physics.RUDDER_DAMPEN_RATE);
            }

            boolean usingMouseYoke = controlMouseYoke(aircraft, mouseXDelta, mouseYDelta);

            //Check pitch.
            if (!usingMouseYoke) {
                controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_PITCH, ControlsKeyboard.AIRCRAFT_PITCH_U, ControlsKeyboard.AIRCRAFT_PITCH_D, ConfigSystem.client.controlSettings.flightControlRate.value, EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, aircraft.elevatorInputVar, EntityVehicleF_Physics.ELEVATOR_DAMPEN_RATE);
            }

            //Check roll.  Blimps use roll for rudder for steering.
            if (!usingMouseYoke) {
                if (aircraft.definition.motorized.isBlimp) {
                    controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_ROLL, ControlsKeyboard.AIRCRAFT_ROLL_R, ControlsKeyboard.AIRCRAFT_ROLL_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, aircraft.rudderInputVar, EntityVehicleF_Physics.RUDDER_DAMPEN_RATE);
                } else {
                    controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_ROLL, ControlsKeyboard.AIRCRAFT_ROLL_R, ControlsKeyboard.AIRCRAFT_ROLL_L, ConfigSystem.client.controlSettings.flightControlRate.value, EntityVehicleF_Physics.MAX_AILERON_ANGLE, aircraft.aileronInputVar, EntityVehicleF_Physics.AILERON_DAMPEN_RATE);
                }
            }
        }

        //Trim controls always available regardless of mouse flight mode.
        controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_YAW_R, ControlsJoystick.AIRCRAFT_TRIM_YAW_L, EntityVehicleF_Physics.MAX_RUDDER_TRIM, aircraft.rudderTrimVar);
        controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_PITCH_U, ControlsJoystick.AIRCRAFT_TRIM_PITCH_D, EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, aircraft.elevatorTrimVar);
        controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_ROLL_R, ControlsJoystick.AIRCRAFT_TRIM_ROLL_L, EntityVehicleF_Physics.MAX_AILERON_TRIM, aircraft.aileronTrimVar);

        //Check to see if we request a different auto-level state.
        if (ConfigSystem.client.controlSettings.heliAutoLevel.value ^ aircraft.autolevelEnabledVar.isActive) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.autolevelEnabledVar, ConfigSystem.client.controlSettings.heliAutoLevel.value ? 1 : 0));
        }
    }

    private static void controlGroundVehicle(EntityVehicleF_Physics powered, boolean isPlayerController) {
        controlCamera(ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsKeyboard.CAR_CHANGEVIEW, ControlsJoystick.CAR_LOOK_UD, ControlsJoystick.CAR_LOOK_LR);
        rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
        controlFreecam(ControlsKeyboard.CAR_CAMLOCK);
        controlGun(powered, ControlsKeyboard.CAR_GUN_FIRE, ControlsKeyboard.CAR_GUN_SWITCH);
        controlRadio(powered, ControlsKeyboard.CAR_RADIO);
        controlJoystick(powered, ControlsKeyboard.CAR_JS_INHIBIT);

        if (!isPlayerController) {
            return;
        }
        //Open or close the panel.
        controlPanel(powered, ControlsKeyboard.CAR_PANEL);

        //Check brake and gas.  Depends on how the controls are configured.
        if (powered.definition.motorized.hasIncrementalThrottle) {
            //Check brake and gas.  Brake always changes, gas goes up-down.
            controlBrake(powered, ControlsJoystick.CAR_BRAKE, ControlsJoystick.CAR_BRAKE_DIGITAL, ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_PARK);
            if (ControlsJoystick.CAR_GAS.isJoystickActive()) {
                //Send throttle over if throttle if cruise control is off, or if throttle is less than the axis level.
                double throttleLevel = ControlsJoystick.CAR_GAS.getAxisState(true) * EntityVehicleF_Physics.MAX_THROTTLE;
                if (!powered.autopilotValueVar.isActive || powered.throttleVar.currentValue < throttleLevel) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.throttleVar, throttleLevel));
                }
            } else {
                if (ControlsKeyboard.CAR_GAS.isPressed()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(powered.throttleVar, EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
                }
                if (ControlsKeyboard.CAR_BRAKE.isPressed() || ControlsJoystick.CAR_BRAKE_DIGITAL.isPressed()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(powered.throttleVar, -EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
                }
            }
        } else {
            double throttleRequest = -999;
            if (ConfigSystem.client.controlSettings.simpleThrottle.value) {
                if (!powered.engines.isEmpty()) {
                    //Get the brake value.
                    double brakeRequest = -999;
                    final double brakeValue;
                    if (ControlsJoystick.CAR_BRAKE.isJoystickActive()) {
                        brakeValue = ControlsJoystick.CAR_BRAKE.getAxisState(true);
                    } else if (ControlsKeyboard.CAR_BRAKE.isPressed() || ControlsJoystick.CAR_BRAKE_DIGITAL.isPressed()) {
                        brakeValue = EntityVehicleF_Physics.MAX_BRAKE;
                    } else {
                        brakeValue = 0;
                    }

                    //Get the throttle value.
                    final double throttleValue;
                    if (ControlsJoystick.CAR_GAS.isJoystickActive()) {
                        throttleValue = ControlsJoystick.CAR_GAS.getAxisState(true) * EntityVehicleF_Physics.MAX_THROTTLE;
                    } else if (ControlsKeyboardDynamic.CAR_SLOW.isPressed()) {
                        throttleValue = ConfigSystem.client.controlSettings.halfThrottle.value ? EntityVehicleF_Physics.MAX_THROTTLE : EntityVehicleF_Physics.MAX_THROTTLE / 2D;
                    } else if (ControlsKeyboard.CAR_GAS.isPressed()) {
                        throttleValue = ConfigSystem.client.controlSettings.halfThrottle.value ? EntityVehicleF_Physics.MAX_THROTTLE / 2D : EntityVehicleF_Physics.MAX_THROTTLE;
                    } else {
                        throttleValue = 0;
                    }

                    //If we are going slow, and don't have gas or brake, automatically set the brake.
                    //Otherwise send normal values if we are in neutral or forwards,
                    //and invert controls if we are in a reverse gear (and not using a shifter).
                    //Use only the first engine for this.
                    if (throttleValue == 0 && brakeValue == 0 && powered.axialVelocity < PartEngine.MAX_SHIFT_SPEED) {
                        throttleRequest = 0;
                        brakeRequest = EntityVehicleF_Physics.MAX_BRAKE;
                    } else if (powered.engines.get(0).currentGearVar.currentValue >= 0 || ConfigSystem.client.controlSettings.useShifter.value) {
                        brakeRequest = brakeValue;

                        //Send throttle over if throttle if cruise control is off, or if the throttle is pressed, or was released this check.
                        if (!powered.autopilotValueVar.isActive || throttleValue > 0 || throttleRequestLastCheck > 0) {
                            throttleRequest = throttleValue;
                        }
                    } else {
                        throttleRequest = brakeValue;
                        brakeRequest = throttleValue;
                    }

                    if (!ConfigSystem.client.controlSettings.useShifter.value) {
                        powered.engines.forEach(engine -> {
                            //If we don't have velocity, and we have the appropriate control, shift.
                            if (brakeValue > EntityVehicleF_Physics.MAX_BRAKE / 4F && engine.currentGearVar.currentValue >= 0 && powered.axialVelocity < 0.01F) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.shiftDownVar, 1));
                            } else if (throttleValue > EntityVehicleF_Physics.MAX_THROTTLE / 4F && engine.currentGearVar.currentValue <= 0 && powered.axialVelocity < 0.01F) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.shiftUpVar, 1));
                            }
                        });
                    }

                    if (brakeRequest != -999 && brakeRequestLastCheck != brakeRequest) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.brakeVar, brakeRequest));
                    }
                    brakeRequestLastCheck = brakeRequest;
                }
            } else {
                //Check brake and gas and set to on or off.
                controlBrake(powered, ControlsJoystick.CAR_BRAKE, ControlsJoystick.CAR_BRAKE_DIGITAL, ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_PARK);
                if (ControlsJoystick.CAR_GAS.isJoystickActive()) {
                    //Send throttle over if throttle if cruise control is off, or if throttle is greater than the current value.
                    double throttleLevel = ControlsJoystick.CAR_GAS.getAxisState(true);
                    if (!powered.autopilotValueVar.isActive || throttleLevel > powered.throttleVar.currentValue) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.throttleVar, throttleLevel));
                    }
                } else {
                    if (ControlsKeyboardDynamic.CAR_SLOW.isPressed()) {
                        if (!ConfigSystem.client.controlSettings.halfThrottle.value) {
                            throttleRequest = EntityVehicleF_Physics.MAX_THROTTLE / 2D;
                        } else {
                            throttleRequest = EntityVehicleF_Physics.MAX_THROTTLE;
                        }
                    } else if (ControlsKeyboard.CAR_GAS.isPressed()) {
                        if (!ConfigSystem.client.controlSettings.halfThrottle.value) {
                            throttleRequest = EntityVehicleF_Physics.MAX_THROTTLE;
                        } else {
                            throttleRequest = EntityVehicleF_Physics.MAX_THROTTLE / 2D;
                        }
                    } else {
                        //Send gas off packet if we don't have cruise on, or if we do and we pressed the throttle last check.
                        if (!powered.autopilotValueVar.isActive || throttleRequestLastCheck > 0) {
                            throttleRequest = 0;
                        }
                    }
                }
            }
            if (throttleRequest != -999 && throttleRequestLastCheck != throttleRequest) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.throttleVar, throttleRequest));
            }
            //Check if we have throttle request with brake on.  Brakes can be left on from simple throttle and such of other players.
            //Take the brake off here if so, since otherwise it will stay on unless we press the brake key.
            if (throttleRequest > 0 && powered.brakeVar.currentValue > 0) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.brakeVar, 0));
            }
            throttleRequestLastCheck = throttleRequest;

        }

        //Check steering.  Don't check while on a road, since we auto-drive on those.
        if (!powered.lockedOnRoad) {
            controlControlSurface(powered, ControlsJoystick.CAR_TURN, ControlsKeyboard.CAR_TURN_R, ControlsKeyboard.CAR_TURN_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, powered.rudderInputVar, ConfigSystem.client.controlSettings.steeringReturnRate.value);
        }

        //Check if we are shifting.
        if (ConfigSystem.client.controlSettings.useShifter.value) {
            final int gearNumber;
            if (ControlsJoystick.CAR_SHIFT_1.isPressed()) {
                gearNumber = 1;
            } else if (ControlsJoystick.CAR_SHIFT_2.isPressed()) {
                gearNumber = 2;
            } else if (ControlsJoystick.CAR_SHIFT_3.isPressed()) {
                gearNumber = 3;
            } else if (ControlsJoystick.CAR_SHIFT_4.isPressed()) {
                gearNumber = 4;
            } else if (ControlsJoystick.CAR_SHIFT_5.isPressed()) {
                gearNumber = 5;
            } else if (ControlsJoystick.CAR_SHIFT_6.isPressed()) {
                gearNumber = 6;
            } else if (ControlsJoystick.CAR_SHIFT_7.isPressed()) {
                gearNumber = 7;
            } else if (ControlsJoystick.CAR_SHIFT_8.isPressed()) {
                gearNumber = 8;
            } else if (ControlsJoystick.CAR_SHIFT_9.isPressed()) {
                gearNumber = 9;
            } else if (ControlsJoystick.CAR_SHIFT_R.isPressed()) {
                gearNumber = 10;
            } else {
                gearNumber = 11;
            }
            powered.engines.forEach(engine -> {
                if (engine.shiftSelectionVar.currentValue != gearNumber) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.shiftSelectionVar, gearNumber));
                }
            });
        } else {
            powered.engines.forEach(engine -> {
                if (engine.shiftSelectionVar.isActive) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.shiftSelectionVar, 0));
                }
            });
            if (ControlsKeyboardDynamic.CAR_SHIFT_NU.isPressed() || ControlsKeyboardDynamic.CAR_SHIFT_ND.isPressed()) {
                powered.engines.forEach(engine -> {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftNeutralVar));
                });
            } else {
                if (ControlsKeyboard.CAR_SHIFT_U.isPressed()) {
                    powered.engines.forEach(engine -> {
                        if (engine.isAutomaticVar.isActive || powered.isSimpleThrottleVar.isActive) {
                            if (engine.currentGearVar.currentValue < 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftNeutralVar));
                            } else if (engine.currentGearVar.currentValue == 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftUpVar));
                            }
                        } else {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftUpVar));
                        }
                    });
                }
                if (ControlsKeyboard.CAR_SHIFT_D.isPressed()) {
                    powered.engines.forEach(engine -> {
                        if (engine.isAutomaticVar.isActive || powered.isSimpleThrottleVar.isActive) {
                            if (engine.currentGearVar.currentValue > 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftNeutralVar));
                            } else if (engine.currentGearVar.currentValue == 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftDownVar));
                            }
                        } else {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftDownVar));
                        }
                    });
                }
            }
            //Check if we are simpleThrottle and if so, kindly ask vehicles to treat their manual transmissions as auto transmissions. Also has us send auto-type shift packets when enabled.
            if (ConfigSystem.client.controlSettings.simpleThrottle.value && !powered.isSimpleThrottleVar.isActive) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.isSimpleThrottleVar, 1));
             } else if (!ConfigSystem.client.controlSettings.simpleThrottle.value && powered.isSimpleThrottleVar.isActive) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.isSimpleThrottleVar, 0));
             }
        }

        //Check if horn button is pressed.
        if (ControlsKeyboard.CAR_HORN.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.hornVar, 1));
        } else if (ControlsKeyboard.CAR_HORN.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.hornVar, 0));
        }

        //Check for lights.
        if (ControlsKeyboard.CAR_LIGHTS.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.runningLightVar));
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.headLightVar));
        }
        if (ControlsKeyboard.CAR_TURNSIGNAL_L.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.leftTurnLightVar));
        }
        if (ControlsKeyboard.CAR_TURNSIGNAL_R.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.rightTurnLightVar));
        }

        //Change turn signal status depending on turning status.
        //Keep signals on until we have been moving without turning in the
        //pressed direction for 2 seconds, or if we turn in the other direction.
        //This only happens if the signals are set to automatic.  For manual signals, we let the player control them.
        if (ConfigSystem.client.controlSettings.autoTrnSignals.value) {
            if (!powered.turningLeft && powered.rudderInputVar.currentValue < -20) {
                powered.turningLeft = true;
                powered.turningCooldown = 40;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.leftTurnLightVar));
            }
            if (!powered.turningRight && powered.rudderInputVar.currentValue > 20) {
                powered.turningRight = true;
                powered.turningCooldown = 40;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.rightTurnLightVar));
            }
            if (powered.turningLeft && (powered.rudderInputVar.currentValue > 0 || powered.turningCooldown == 0)) {
                powered.turningLeft = false;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.leftTurnLightVar));
            }
            if (powered.turningRight && (powered.rudderInputVar.currentValue < 0 || powered.turningCooldown == 0)) {
                powered.turningRight = false;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.rightTurnLightVar));
            }
            if (powered.velocity != 0 && powered.turningCooldown > 0 && powered.rudderInputVar.currentValue == 0) {
                --powered.turningCooldown;
            }
        }
    }

    /**
     * List of enums representing all controls present.  Add new controls by adding their enum values here
     *
     * @author don_bruce
     */
    public enum ControlsKeyboard {
        GENERAL_CUSTOM1(ControlsJoystick.GENERAL_CUSTOM1, true, "NUMPAD0", LanguageSystem.INPUT_CUSTOM1),
        GENERAL_CUSTOM2(ControlsJoystick.GENERAL_CUSTOM2, true, "NUMPAD1", LanguageSystem.INPUT_CUSTOM2),
        GENERAL_CUSTOM3(ControlsJoystick.GENERAL_CUSTOM3, true, "NUMPAD2", LanguageSystem.INPUT_CUSTOM3),
        GENERAL_CUSTOM4(ControlsJoystick.GENERAL_CUSTOM4, true, "NUMPAD3", LanguageSystem.INPUT_CUSTOM4),
		GENERAL_CUSTOM5(ControlsJoystick.GENERAL_CUSTOM5, true, "NUMPAD4", LanguageSystem.INPUT_CUSTOM5),
        GENERAL_CUSTOM6(ControlsJoystick.GENERAL_CUSTOM6, true, "NUMPAD5", LanguageSystem.INPUT_CUSTOM6),
        GENERAL_CUSTOM7(ControlsJoystick.GENERAL_CUSTOM7, true, "NUMPAD6", LanguageSystem.INPUT_CUSTOM7),
        GENERAL_CUSTOM8(ControlsJoystick.GENERAL_CUSTOM8, true, "NUMPAD7", LanguageSystem.INPUT_CUSTOM8),
        GENERAL_CUSTOM9(ControlsJoystick.GENERAL_CUSTOM9, true, "NUMPAD8", LanguageSystem.INPUT_CUSTOM9),
        GENERAL_CUSTOM10(ControlsJoystick.GENERAL_CUSTOM10, true, "NUMPAD9", LanguageSystem.INPUT_CUSTOM10),
        GENERAL_RELOAD(ControlsJoystick.GENERAL_RELOAD, true, "R", LanguageSystem.INPUT_GUN_RELOAD),

        AIRCRAFT_YAW_R(ControlsJoystick.AIRCRAFT_YAW, false, "L", LanguageSystem.INPUT_YAW_R),
        AIRCRAFT_YAW_L(ControlsJoystick.AIRCRAFT_YAW, false, "J", LanguageSystem.INPUT_YAW_L),
        AIRCRAFT_PITCH_U(ControlsJoystick.AIRCRAFT_PITCH, false, "S", LanguageSystem.INPUT_PITCH_U),
        AIRCRAFT_PITCH_D(ControlsJoystick.AIRCRAFT_PITCH, false, "W", LanguageSystem.INPUT_PITCH_D),
        AIRCRAFT_ROLL_R(ControlsJoystick.AIRCRAFT_ROLL, false, "D", LanguageSystem.INPUT_ROLL_R),
        AIRCRAFT_ROLL_L(ControlsJoystick.AIRCRAFT_ROLL, false, "A", LanguageSystem.INPUT_ROLL_L),
        AIRCRAFT_THROTTLE_U(ControlsJoystick.AIRCRAFT_THROTTLE, false, "I", LanguageSystem.INPUT_THROTTLE_U),
        AIRCRAFT_THROTTLE_D(ControlsJoystick.AIRCRAFT_THROTTLE, false, "K", LanguageSystem.INPUT_THROTTLE_D),
        AIRCRAFT_FLAPS_U(ControlsJoystick.AIRCRAFT_FLAPS_U, true, "Y", LanguageSystem.INPUT_FLAPS_U),
        AIRCRAFT_FLAPS_D(ControlsJoystick.AIRCRAFT_FLAPS_D, true, "H", LanguageSystem.INPUT_FLAPS_D),
        AIRCRAFT_ARCADE(ControlsJoystick.AIRCRAFT_ARCADE, true, "C", LanguageSystem.INPUT_ARCADE),
        AIRCRAFT_BRAKE(ControlsJoystick.AIRCRAFT_BRAKE, false, "B", LanguageSystem.INPUT_BRAKE),
        AIRCRAFT_PARK(ControlsJoystick.AIRCRAFT_PARK, true, "N", LanguageSystem.INPUT_PARK),
        AIRCRAFT_PANEL(ControlsJoystick.AIRCRAFT_PANEL, true, "U", LanguageSystem.INPUT_PANEL),
        AIRCRAFT_RADIO(ControlsJoystick.AIRCRAFT_RADIO, true, "MINUS", LanguageSystem.INPUT_RADIO),
        AIRCRAFT_GUN_FIRE(ControlsJoystick.AIRCRAFT_GUN_FIRE, false, "SPACE", LanguageSystem.INPUT_GUN_FIRE),
        AIRCRAFT_GUN_SWITCH(ControlsJoystick.AIRCRAFT_GUN_SWITCH, true, "V", LanguageSystem.INPUT_GUN_SWITCH),
        AIRCRAFT_ZOOM_I(ControlsJoystick.AIRCRAFT_ZOOM_I, true, "PRIOR", LanguageSystem.INPUT_ZOOM_I),
        AIRCRAFT_ZOOM_O(ControlsJoystick.AIRCRAFT_ZOOM_O, true, "NEXT", LanguageSystem.INPUT_ZOOM_O),
        AIRCRAFT_CHANGEVIEW(ControlsJoystick.AIRCRAFT_CHANGEVIEW, true, "X", LanguageSystem.INPUT_CHANGEVIEW),
        AIRCRAFT_CAMLOCK(ControlsJoystick.AIRCRAFT_CAMLOCK, true, "LMENU", LanguageSystem.INPUT_CAMLOCK),
        AIRCRAFT_JS_INHIBIT(ControlsJoystick.AIRCRAFT_JS_INHIBIT, true, "SCROLL", LanguageSystem.INPUT_JS_INHIBIT),

        CAR_MOD(ControlsJoystick.CAR_MOD, false, "RSHIFT", LanguageSystem.INPUT_MOD),
        CAR_TURN_R(ControlsJoystick.CAR_TURN, false, "D", LanguageSystem.INPUT_TURN_R),
        CAR_TURN_L(ControlsJoystick.CAR_TURN, false, "A", LanguageSystem.INPUT_TURN_L),
        CAR_GAS(ControlsJoystick.CAR_GAS, false, "W", LanguageSystem.INPUT_GAS),
        CAR_BRAKE(ControlsJoystick.CAR_BRAKE, false, "S", LanguageSystem.INPUT_BRAKE),
        CAR_PARK(ControlsJoystick.CAR_PARK, true, "N", LanguageSystem.INPUT_PARK),
        CAR_PANEL(ControlsJoystick.CAR_PANEL, true, "U", LanguageSystem.INPUT_PANEL),
        CAR_SHIFT_U(ControlsJoystick.CAR_SHIFT_U, true, "R", LanguageSystem.INPUT_SHIFT_U),
        CAR_SHIFT_D(ControlsJoystick.CAR_SHIFT_D, true, "F", LanguageSystem.INPUT_SHIFT_D),
        CAR_HORN(ControlsJoystick.CAR_HORN, true, "C", LanguageSystem.INPUT_HORN),
        CAR_RADIO(ControlsJoystick.CAR_RADIO, true, "MINUS", LanguageSystem.INPUT_RADIO),
        CAR_GUN_FIRE(ControlsJoystick.CAR_GUN_FIRE, false, "SPACE", LanguageSystem.INPUT_GUN_FIRE),
        CAR_GUN_SWITCH(ControlsJoystick.CAR_GUN_SWITCH, true, "V", LanguageSystem.INPUT_GUN_SWITCH),
        CAR_ZOOM_I(ControlsJoystick.CAR_ZOOM_I, true, "PRIOR", LanguageSystem.INPUT_ZOOM_I),
        CAR_ZOOM_O(ControlsJoystick.CAR_ZOOM_O, true, "NEXT", LanguageSystem.INPUT_ZOOM_O),
        CAR_CHANGEVIEW(ControlsJoystick.CAR_CHANGEVIEW, true, "X", LanguageSystem.INPUT_CHANGEVIEW),
        CAR_LIGHTS(ControlsJoystick.CAR_LIGHTS, true, "G", LanguageSystem.INPUT_LIGHTS),
        CAR_CAMLOCK(ControlsJoystick.CAR_CAMLOCK, true, "LMENU", LanguageSystem.INPUT_CAMLOCK),
        CAR_TURNSIGNAL_L(ControlsJoystick.CAR_TURNSIGNAL_L, true, "COMMA", LanguageSystem.INPUT_TURNSIGNAL_L),
        CAR_TURNSIGNAL_R(ControlsJoystick.CAR_TURNSIGNAL_R, true, "PERIOD", LanguageSystem.INPUT_TURNSIGNAL_R),
        CAR_JS_INHIBIT(ControlsJoystick.CAR_JS_INHIBIT, true, "SCROLL", LanguageSystem.INPUT_JS_INHIBIT);

        public final boolean isMomentary;
        public final String systemName;
        public final LanguageEntry language;
        public final String defaultKeyName;
        public final ConfigKeyboard config;
        private final ControlsJoystick linkedJoystick;

        private boolean wasPressedThisCall;
        private boolean wasPressedLastCall;

        ControlsKeyboard(ControlsJoystick linkedJoystick, boolean isMomentary, String defaultKeyName, LanguageEntry language) {
            this.linkedJoystick = linkedJoystick;
            this.isMomentary = isMomentary;
            this.systemName = this.name().toLowerCase(Locale.ROOT).replaceFirst("_", ".");
            this.language = language;
            this.defaultKeyName = defaultKeyName;
            if (ConfigSystem.client.controls.keyboard.containsKey(systemName)) {
                this.config = ConfigSystem.client.controls.keyboard.get(systemName);
            } else {
                this.config = new ConfigKeyboard();
            }
        }

        /**
         * Returns true if the given key is currently pressed.  If our linked
         * joystick is pressed, return true.  If the joystick is not, but it
         * is bound, and we are using keyboard overrides, return false.
         * Otherwise return the actual key state.
         */
        public boolean isPressed() {
            wasPressedLastCall = wasPressedThisCall;
            if (linkedJoystick.isPressed()) {
                //Joystick pressed.
                wasPressedThisCall = true;
            } else if (linkedJoystick.isJoystickActive() && ConfigSystem.client.controlSettings.kbOverride.value) {
                //Joystick found, but not pressed, and is overriding keyboard inputs, so return false.
                wasPressedThisCall = false;
            } else {
                if (config.isMouseButton) {
                    //Mouse button binding: block when any mod GUI is open.
                    if (AGUIBase.activeInputGUI != null) {
                        wasPressedThisCall = false;
                    } else {
                        wasPressedThisCall = InterfaceManager.inputInterface.isMouseButtonPressed(config.keyCode);
                    }
                } else {
                    wasPressedThisCall = InterfaceManager.inputInterface.isKeyPressed(config.keyCode);
                }
                if (isMomentary && wasPressedLastCall) {
                    return false;
                }
            }
            return wasPressedThisCall;
        }

        /**
         * MUST be called after only a single call to {@link #wasPressedThisCall}
         */
        public boolean justReleased() {
            return !wasPressedThisCall && wasPressedLastCall;
        }
    }

    public enum ControlsJoystick {
        GENERAL_CUSTOM1(false, true, LanguageSystem.INPUT_CUSTOM1),
        GENERAL_CUSTOM2(false, true, LanguageSystem.INPUT_CUSTOM2),
        GENERAL_CUSTOM3(false, true, LanguageSystem.INPUT_CUSTOM3),
        GENERAL_CUSTOM4(false, true, LanguageSystem.INPUT_CUSTOM4),
		GENERAL_CUSTOM5(false, true, LanguageSystem.INPUT_CUSTOM5),
        GENERAL_CUSTOM6(false, true, LanguageSystem.INPUT_CUSTOM6),
        GENERAL_CUSTOM7(false, true, LanguageSystem.INPUT_CUSTOM7),
        GENERAL_CUSTOM8(false, true, LanguageSystem.INPUT_CUSTOM8),
        GENERAL_CUSTOM9(false, true, LanguageSystem.INPUT_CUSTOM9),
        GENERAL_CUSTOM10(false, true, LanguageSystem.INPUT_CUSTOM10),
        GENERAL_RELOAD(false, true, LanguageSystem.INPUT_GUN_RELOAD),

        AIRCRAFT_CAMLOCK(false, true, LanguageSystem.INPUT_CAMLOCK),
        AIRCRAFT_YAW(true, false, LanguageSystem.INPUT_YAW),
        AIRCRAFT_PITCH(true, false, LanguageSystem.INPUT_PITCH),
        AIRCRAFT_ROLL(true, false, LanguageSystem.INPUT_ROLL),
        AIRCRAFT_THROTTLE(true, false, LanguageSystem.INPUT_THROTTLE),
        AIRCRAFT_BRAKE(true, false, LanguageSystem.INPUT_BRAKE),
        AIRCRAFT_BRAKE_DIGITAL(false, false, LanguageSystem.INPUT_BRAKE),
        AIRCRAFT_GEAR(false, true, LanguageSystem.INPUT_GEAR),
        AIRCRAFT_FLAPS_U(false, true, LanguageSystem.INPUT_FLAPS_U),
        AIRCRAFT_FLAPS_D(false, true, LanguageSystem.INPUT_FLAPS_D),
        AIRCRAFT_ARCADE(false, true, LanguageSystem.INPUT_ARCADE),
        AIRCRAFT_PANEL(false, true, LanguageSystem.INPUT_PANEL),
        AIRCRAFT_PARK(false, true, LanguageSystem.INPUT_PARK),
        AIRCRAFT_RADIO(false, true, LanguageSystem.INPUT_RADIO),
        AIRCRAFT_GUN_FIRE(false, false, LanguageSystem.INPUT_GUN_FIRE),
        AIRCRAFT_GUN_SWITCH(false, true, LanguageSystem.INPUT_GUN_SWITCH),
        AIRCRAFT_ZOOM_I(false, true, LanguageSystem.INPUT_ZOOM_I),
        AIRCRAFT_ZOOM_O(false, true, LanguageSystem.INPUT_ZOOM_O),
        AIRCRAFT_CHANGEVIEW(false, true, LanguageSystem.INPUT_CHANGEVIEW),
        AIRCRAFT_LOOK_UD(true, false, LanguageSystem.INPUT_LOOK_UD),
        AIRCRAFT_LOOK_LR(true, false, LanguageSystem.INPUT_LOOK_LR),
        AIRCRAFT_LOOK_L(false, false, LanguageSystem.INPUT_LOOK_L),
        AIRCRAFT_LOOK_R(false, false, LanguageSystem.INPUT_LOOK_R),
        AIRCRAFT_LOOK_U(false, false, LanguageSystem.INPUT_LOOK_U),
        AIRCRAFT_LOOK_D(false, false, LanguageSystem.INPUT_LOOK_D),
        AIRCRAFT_LOOK_A(false, false, LanguageSystem.INPUT_LOOK_A),
        AIRCRAFT_TRIM_YAW_R(false, false, LanguageSystem.INPUT_TRIM_YAW_R),
        AIRCRAFT_TRIM_YAW_L(false, false, LanguageSystem.INPUT_TRIM_YAW_L),
        AIRCRAFT_TRIM_PITCH_U(false, false, LanguageSystem.INPUT_TRIM_PITCH_U),
        AIRCRAFT_TRIM_PITCH_D(false, false, LanguageSystem.INPUT_TRIM_PITCH_D),
        AIRCRAFT_TRIM_ROLL_R(false, false, LanguageSystem.INPUT_TRIM_ROLL_R),
        AIRCRAFT_TRIM_ROLL_L(false, false, LanguageSystem.INPUT_TRIM_ROLL_L),
        AIRCRAFT_REVERSE(false, true, LanguageSystem.INPUT_REVERSE),
        AIRCRAFT_JS_INHIBIT(false, true, LanguageSystem.INPUT_JS_INHIBIT),

        CAR_MOD(false, false, LanguageSystem.INPUT_MOD),
        CAR_CAMLOCK(false, true, LanguageSystem.INPUT_CAMLOCK),
        CAR_TURN(true, false, LanguageSystem.INPUT_TURN),
        CAR_GAS(true, false, LanguageSystem.INPUT_GAS),
        CAR_BRAKE(true, false, LanguageSystem.INPUT_BRAKE),
        CAR_BRAKE_DIGITAL(false, false, LanguageSystem.INPUT_BRAKE),
        CAR_PANEL(false, true, LanguageSystem.INPUT_PANEL),
        CAR_SHIFT_U(false, true, LanguageSystem.INPUT_SHIFT_U),
        CAR_SHIFT_D(false, true, LanguageSystem.INPUT_SHIFT_D),
        CAR_SHIFT_1(false, false, LanguageSystem.INPUT_SHIFT_1),
        CAR_SHIFT_2(false, false, LanguageSystem.INPUT_SHIFT_2),
        CAR_SHIFT_3(false, false, LanguageSystem.INPUT_SHIFT_3),
        CAR_SHIFT_4(false, false, LanguageSystem.INPUT_SHIFT_4),
        CAR_SHIFT_5(false, false, LanguageSystem.INPUT_SHIFT_5),
        CAR_SHIFT_6(false, false, LanguageSystem.INPUT_SHIFT_6),
        CAR_SHIFT_7(false, false, LanguageSystem.INPUT_SHIFT_7),
        CAR_SHIFT_8(false, false, LanguageSystem.INPUT_SHIFT_8),
        CAR_SHIFT_9(false, false, LanguageSystem.INPUT_SHIFT_9),
        CAR_SHIFT_R(false, false, LanguageSystem.INPUT_SHIFT_R),
        CAR_HORN(false, true, LanguageSystem.INPUT_HORN),
        CAR_PARK(false, true, LanguageSystem.INPUT_PARK),
        CAR_RADIO(false, true, LanguageSystem.INPUT_RADIO),
        CAR_GUN_FIRE(false, false, LanguageSystem.INPUT_GUN_FIRE),
        CAR_GUN_SWITCH(false, true, LanguageSystem.INPUT_GUN_SWITCH),
        CAR_ZOOM_I(false, true, LanguageSystem.INPUT_ZOOM_I),
        CAR_ZOOM_O(false, true, LanguageSystem.INPUT_ZOOM_O),
        CAR_CHANGEVIEW(false, true, LanguageSystem.INPUT_CHANGEVIEW),
        CAR_LOOK_UD(true, false, LanguageSystem.INPUT_LOOK_UD),
        CAR_LOOK_LR(true, false, LanguageSystem.INPUT_LOOK_LR),
        CAR_LOOK_L(false, false, LanguageSystem.INPUT_LOOK_L),
        CAR_LOOK_R(false, false, LanguageSystem.INPUT_LOOK_R),
        CAR_LOOK_U(false, false, LanguageSystem.INPUT_LOOK_U),
        CAR_LOOK_D(false, false, LanguageSystem.INPUT_LOOK_D),
        CAR_LOOK_A(false, false, LanguageSystem.INPUT_LOOK_A),
        CAR_LIGHTS(false, true, LanguageSystem.INPUT_LIGHTS),
        CAR_TURNSIGNAL_L(false, true, LanguageSystem.INPUT_TURNSIGNAL_L),
        CAR_TURNSIGNAL_R(false, true, LanguageSystem.INPUT_TURNSIGNAL_R),
        CAR_JS_INHIBIT(false, true, LanguageSystem.INPUT_JS_INHIBIT);

        public final boolean isAxis;
        public final boolean isMomentary;
        public final String systemName;
        public final LanguageEntry language;
        public final ConfigJoystick config;

        private boolean wasPressedLastCall;

        ControlsJoystick(boolean isAxis, boolean isMomentary, LanguageEntry language) {
            this.isAxis = isAxis;
            this.isMomentary = isMomentary;
            this.systemName = this.name().toLowerCase(Locale.ROOT).replaceFirst("_", ".");
            this.language = language;
            if (ConfigSystem.client.controls.joystick.containsKey(systemName)) {
                this.config = ConfigSystem.client.controls.joystick.get(systemName);
            } else {
                this.config = new ConfigJoystick();
            }
        }

        public boolean isJoystickActive() {
            return !joysticksInhibited && InterfaceManager.inputInterface.isJoystickPresent(config.joystickName);
        }

        public boolean isPressed() {
            if (isJoystickActive()) {
                if (isMomentary) {
                    if (wasPressedLastCall) {
                        wasPressedLastCall = InterfaceManager.inputInterface.getJoystickButtonValue(config.joystickName, config.buttonIndex);
                        return false;
                    } else {
                        wasPressedLastCall = InterfaceManager.inputInterface.getJoystickButtonValue(config.joystickName, config.buttonIndex);
                        return wasPressedLastCall;
                    }
                } else {
                    return InterfaceManager.inputInterface.getJoystickButtonValue(config.joystickName, config.buttonIndex);
                }
            } else {
                return false;
            }
        }

        private float getMultistateValue() {
            return InterfaceManager.inputInterface.getJoystickAxisValue(config.joystickName, config.buttonIndex);
        }

        private double getAxisState(boolean ignoreDeadzone) {
            double pollValue = getMultistateValue();
            if ((config.axisMaxTravel != config.axisMinTravel) && (ignoreDeadzone || Math.abs(pollValue) > ConfigSystem.client.controlSettings.joystickDeadZone.value)) {
                //Clamp the poll value to the defined axis bounds set during config to prevent over and under-runs.
                pollValue = Math.max(config.axisMinTravel, pollValue);
                pollValue = Math.min(config.axisMaxTravel, pollValue);

                //Divide the poll value plus the min bounds by the span to get it in the range of 0-1.
                pollValue = (pollValue - config.axisMinTravel) / (config.axisMaxTravel - config.axisMinTravel);

                //If axis is inverted, invert poll.
                if (config.invertedAxis) {
                    pollValue = 1 - pollValue;
                }

                //Now return the value.
                return pollValue;
            } else {
                return 0;
            }
        }

        public void setControl(String joystickName, int buttonIndex) {
            config.joystickName = joystickName;
            config.buttonIndex = buttonIndex;
            ConfigSystem.client.controls.joystick.put(systemName, config);
            ConfigSystem.saveToDisk();
        }

        public void setAxisControl(String joystickName, int buttonIndex, double axisMinTravel, double axisMaxTravel, boolean invertedAxis) {
            config.axisMinTravel = axisMinTravel;
            config.axisMaxTravel = axisMaxTravel;
            config.invertedAxis = invertedAxis;
            setControl(joystickName, buttonIndex);
        }

        public void clearControl() {
            setControl(null, NULL_COMPONENT);
        }
    }

    public enum ControlsKeyboardDynamic {
        CAR_SLOW(ControlsKeyboard.CAR_GAS, ControlsKeyboard.CAR_MOD, LanguageSystem.INPUT_SLOW),
        CAR_SHIFT_NU(ControlsKeyboard.CAR_SHIFT_U, ControlsKeyboard.CAR_MOD, LanguageSystem.INPUT_SHIFT_N),
        CAR_SHIFT_ND(ControlsKeyboard.CAR_SHIFT_D, ControlsKeyboard.CAR_MOD, LanguageSystem.INPUT_SHIFT_N);

        public final LanguageEntry language;
        public final ControlsKeyboard mainControl;
        public final ControlsKeyboard modControl;

        ControlsKeyboardDynamic(ControlsKeyboard mainControl, ControlsKeyboard modControl, LanguageEntry language) {
            this.language = language;
            this.mainControl = mainControl;
            this.modControl = modControl;
        }

        public boolean isPressed() {
            return this.modControl.isPressed() && this.mainControl.isPressed();
        }
    }
}
