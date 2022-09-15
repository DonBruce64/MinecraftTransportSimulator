package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;

/**
 * Traffic signal controller tile entity.  Responsible for keeping the state of traffic
 * intersections.
 *
 * @author don_bruce
 */
public class TileEntitySignalController extends TileEntityDecor {
    private static final TransformationMatrix holoboxTransform = new TransformationMatrix();

    //Main settings for all operation.
    public boolean isRightHandDrive;
    public boolean timedMode;
    public boolean unsavedClientChangesPreset;
    public Axis mainDirectionAxis = Axis.NORTH;

    //Settings for trigger operation.
    public Point3D intersectionCenterPoint;

    //Settings for timed operation.
    public int greenMainTime = 20 * 20;
    public int greenCrossTime = 10 * 20;
    public int yellowMainTime = 2 * 20;
    public int yellowCrossTime = 2 * 20;
    public int allRedTime = 20;

    /*Locations of blocks where signals are.**/
    public final Set<Point3D> componentLocations = new HashSet<>();
    private final Set<Point3D> missingLocations = new HashSet<>();

    /**
     * Signal blocks used in this controller.  Based on components.
     **/
    public final Map<Axis, Set<SignalGroup>> signalGroups = new HashMap<>();
    public final Set<TileEntityPole_TrafficSignal> controlledSignals = new HashSet<>();

    /**
     * Lane counts and intersection widths.
     **/
    public final Map<Axis, IntersectionProperties> intersectionProperties = new HashMap<>();

    public TileEntitySignalController(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);
        initializeController(data);
    }

    @Override
    public void update() {
        super.update();
        //Check every 1 seconds to make sure controlled components are in their correct states.
        //This could have changed due to chunkloading or the components being destroyed.
        //We also check if we're doing changes on the client, as that needs to happen instantly.
        if (ticksExisted % 20 == 0 || unsavedClientChangesPreset) {
            //Check for any missing components, if we are missing some.
            if (!missingLocations.isEmpty()) {
                Iterator<Point3D> iterator = missingLocations.iterator();
                while (iterator.hasNext()) {
                    Point3D poleLocation = iterator.next();
                    TileEntityPole pole = world.getTileEntity(poleLocation);
                    if (pole != null) {
                        iterator.remove();
                        for (Axis axis : Axis.values()) {
                            if (axis.xzPlanar) {
                                ATileEntityPole_Component component = pole.components.get(axis);
                                if (component instanceof TileEntityPole_TrafficSignal) {
                                    TileEntityPole_TrafficSignal signal = (TileEntityPole_TrafficSignal) component;
                                    intersectionProperties.get(axis).isActive = true;
                                    signal.linkedController = this;
                                    controlledSignals.add(signal);
                                }
                            }
                        }
                    }
                }
            }
        }

        //All valid poles and components found.  Update signal blocks that have signals..
        for (Set<SignalGroup> signalGroupSet : signalGroups.values()) {
            for (SignalGroup signalGroup : signalGroupSet) {
                if (signalGroup.laneCount != 0) {
                    signalGroup.update();
                }
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
        //Clear found poles so signals know we don't exist anymore and to remove their references.
        clearFoundPoles();
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.SIGNAL_CONTROLLER));
        return true;
    }

    /**
     * Updates all components and creates the signal groups for an intersection.  Also updates
     * the settings for the controller based on the saved NBT.  Use this whenever the controller
     * settings are changed, either by placing the block the first time, or updating via the GUI.
     * If the GUI is used, pass null in here to prevent data re-parsing.  Otherwise, pass-in the data.
     */
    public void initializeController(IWrapperNBT data) {
        if (data == null) {
            data = save(InterfaceManager.coreInterface.getNewNBTWrapper());
        }

        //Load state data.
        isRightHandDrive = data.getBoolean("isRightHandDrive");
        timedMode = data.getBoolean("timedMode");
        String axisName = data.getString("mainDirectionAxis");
        mainDirectionAxis = axisName.isEmpty() ? Axis.NORTH : Axis.valueOf(axisName);

        intersectionCenterPoint = data.getPoint3d("intersectionCenterPoint");
        if (intersectionCenterPoint.isZero()) {
            intersectionCenterPoint.set(position);
        }

        //Got saved lane info.
        for (Axis axis : Axis.values()) {
            if (axis.xzPlanar) {
                intersectionProperties.put(axis, new IntersectionProperties(data.getDataOrNew(axis.name() + "properties")));
            }
        }

        if (data.getBoolean("hasCustomTimes")) {
            greenMainTime = data.getInteger("greenMainTime");
            greenCrossTime = data.getInteger("greenCrossTime");
            yellowMainTime = data.getInteger("yellowMainTime");
            yellowCrossTime = data.getInteger("yellowCrossTime");
            allRedTime = data.getInteger("allRedTime");
        }

        //Set new component locations.
        componentLocations.clear();
        componentLocations.addAll(data.getPoint3dsCompact("componentLocations"));

        //Create all signal groups.
        signalGroups.clear();
        for (Axis axis : Axis.values()) {
            if (axis.xzPlanar) {
                Set<SignalGroup> signalSet = new HashSet<>();
                signalSet.add(new SignalGroupCenter(axis, data.getDataOrNew(axis.name() + SignalDirection.CENTER.name())));
                signalSet.add(new SignalGroupLeft(axis, data.getDataOrNew(axis.name() + SignalDirection.LEFT.name())));
                signalSet.add(new SignalGroupRight(axis, data.getDataOrNew(axis.name() + SignalDirection.RIGHT.name())));
                signalGroups.put(axis, signalSet);
            }
        }

        //Set all signals to red, except the main-center ones.
        for (Set<SignalGroup> signalGroupSet : signalGroups.values()) {
            for (SignalGroup signalGroup : signalGroupSet) {
                if (signalGroup.isMainSignal && signalGroup.direction.equals(SignalDirection.CENTER)) {
                    signalGroup.requestedLight = signalGroup.getGreenLight();
                } else {
                    signalGroup.requestedLight = signalGroup.getRedLight();
                }
                if (signalGroup.requestedLight.equals(signalGroup.currentLight)) {
                    signalGroup.requestedLight = null;
                }
                signalGroup.currentCooldown = 0;
            }
        }

        //Clear all found poles as they won't be found anymore for the set groups.
        clearFoundPoles();
    }

    /**
     * Clear found pole variables.  This is done on controller init or when we are removed.
     */
    public void clearFoundPoles() {
        controlledSignals.clear();
        missingLocations.clear();
        missingLocations.addAll(componentLocations);
    }

    @Override
    protected void renderHolographicBoxes(TransformationMatrix transform) {
        //Render lane holo-boxes if we are a signal controller that's being edited.
        if (unsavedClientChangesPreset || InterfaceManager.renderingInterface.shouldRenderBoundingBoxes()) {
            for (Set<SignalGroup> signalGroupSet : signalGroups.values()) {
                for (SignalGroup signalGroup : signalGroupSet) {
                    if (signalGroup.signalLineWidth != 0 && intersectionProperties.get(signalGroup.axis).isActive) {
                        //Get relative center coord.
                        //First start with center signal line, which is distance from center of intersection to the edge of the stop line..
                        Point3D boxRelativeCenter = signalGroup.signalLineCenter.copy();
                        //Add 8 units to center on the box which is 16 units long.
                        boxRelativeCenter.add(0, 0, 8);
                        //Rotate box based on signal orientation to proper signal.
                        boxRelativeCenter.rotate(signalGroup.axis.rotation);

                        //Add delta from controller to intersection center.
                        boxRelativeCenter.add(intersectionCenterPoint).subtract(position);
                        boxRelativeCenter.y += 1;

                        //Create bounding box and transform for it..
                        BoundingBox box = new BoundingBox(boxRelativeCenter, signalGroup.signalLineWidth / 2D, 1, 8);
                        holoboxTransform.set(transform).applyTranslation(boxRelativeCenter).applyRotation(signalGroup.axis.rotation);
                        box.renderHolographic(holoboxTransform, null, signalGroup.direction.equals(SignalDirection.LEFT) ? ColorRGB.BLUE : (signalGroup.direction.equals(SignalDirection.RIGHT) ? ColorRGB.YELLOW : ColorRGB.GREEN));
                    }
                }
            }
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setBoolean("isRightHandDrive", isRightHandDrive);
        data.setBoolean("timedMode", timedMode);
        data.setString("mainDirectionAxis", mainDirectionAxis.name());

        data.setPoint3d("intersectionCenterPoint", intersectionCenterPoint);

        for (Axis axis : intersectionProperties.keySet()) {
            if (intersectionProperties.containsKey(axis)) {
                data.setData(axis.name() + "properties", intersectionProperties.get(axis).getData());
            }
        }

        data.setBoolean("hasCustomTimes", true);
        data.setInteger("greenMainTime", greenMainTime);
        data.setInteger("greenCrossTime", greenCrossTime);
        data.setInteger("yellowMainTime", yellowMainTime);
        data.setInteger("yellowCrossTime", yellowCrossTime);
        data.setInteger("allRedTime", allRedTime);

        data.setPoint3dsCompact("componentLocations", componentLocations);
        for (Set<SignalGroup> signalGroupSet : signalGroups.values()) {
            for (SignalGroup signalGroup : signalGroupSet) {
                data.setData(signalGroup.axis.name() + signalGroup.direction.name(), signalGroup.getData());
            }
        }
        return data;
    }

    public static class IntersectionProperties {
        public boolean isActive;
        public int centerLaneCount;
        public int leftLaneCount;
        public int rightLaneCount;
        public double roadWidth;
        public double centerDistance;
        public double centerOffset;

        public IntersectionProperties(IWrapperNBT data) {
            this.centerLaneCount = data.getInteger("centerLaneCount");
            this.leftLaneCount = data.getInteger("leftLaneCount");
            this.rightLaneCount = data.getInteger("rightLaneCount");
            this.roadWidth = data.getDouble("roadWidth");
            this.centerDistance = data.getDouble("centerDistance");
            this.centerOffset = data.getDouble("centerOffset");
        }

        public IWrapperNBT getData() {
            IWrapperNBT data = InterfaceManager.coreInterface.getNewNBTWrapper();
            data.setInteger("centerLaneCount", centerLaneCount);
            data.setInteger("leftLaneCount", leftLaneCount);
            data.setInteger("rightLaneCount", rightLaneCount);
            data.setDouble("roadWidth", roadWidth);
            data.setDouble("centerDistance", centerDistance);
            data.setDouble("centerOffset", centerOffset);
            return data;
        }
    }

    public abstract class SignalGroup {
        public final Axis axis;
        public final SignalDirection direction;
        public final boolean isMainSignal;
        public boolean isActive;

        protected LightType currentLight;
        protected LightType requestedLight;
        protected int currentCooldown;
        protected boolean stateChangeRequested;

        //Parameters for this signal boxes bounds.  These are all based with a south-facing reference.
        //when checking, the point will be rotated to be in this reference plane.
        public final int laneCount;
        public final double signalLineWidth;
        public final Point3D signalLineCenter;

        private SignalGroup(Axis axis, SignalDirection direction, IWrapperNBT data) {
            this.axis = axis;
            this.direction = direction;
            this.isMainSignal = axis.equals(mainDirectionAxis) || axis.equals(mainDirectionAxis.getOpposite());

            //Get saved light status.
            String currentLightName = data.getString("currentLight");
            if (!currentLightName.isEmpty()) {
                currentLight = LightType.valueOf(currentLightName);
            } else {
                currentLight = getRedLight();
            }
            String requestedLightName = data.getString("requestedLight");
            if (!requestedLightName.isEmpty()) {
                requestedLight = LightType.valueOf(requestedLightName);
            }
            currentCooldown = data.getInteger("currentCooldown");

            //Create hitbox bounds.
            IntersectionProperties properties = intersectionProperties.get(axis);
            int totalLaneCount = properties.leftLaneCount + properties.centerLaneCount + properties.rightLaneCount;
            double laneWidth = totalLaneCount != 0 ? properties.roadWidth / totalLaneCount : 0;
            switch (direction) {
                case CENTER: {
                    this.laneCount = properties.centerLaneCount;
                    this.signalLineWidth = properties.centerLaneCount * laneWidth;
                    this.signalLineCenter = new Point3D(properties.centerOffset + (properties.leftLaneCount + properties.centerLaneCount / 2D) * laneWidth, 0, properties.centerDistance);
                    break;
                }
                case LEFT: {
                    this.laneCount = properties.leftLaneCount;
                    this.signalLineWidth = properties.leftLaneCount * laneWidth;
                    this.signalLineCenter = new Point3D(properties.centerOffset + (properties.leftLaneCount / 2D) * laneWidth, 0, properties.centerDistance);
                    break;
                }
                case RIGHT: {
                    this.laneCount = properties.rightLaneCount;
                    this.signalLineWidth = properties.rightLaneCount * laneWidth;
                    this.signalLineCenter = new Point3D(properties.centerOffset + (properties.leftLaneCount + properties.centerLaneCount + properties.rightLaneCount / 2D) * laneWidth, 0, properties.centerDistance);
                    break;
                }
                default:
                    throw new IllegalStateException("We'll never get here, shut up compiler!");
            }
        }

        protected void update() {
            if (currentCooldown > 0) {
                --currentCooldown;
            }
            if (requestedLight != null) {
                //Currently changing lights.  Handle this logic instead of signal-based logic.
                if (currentCooldown == 0) {
                    currentLight = getNextLight();
                    if (currentLight.equals(requestedLight)) {
                        requestedLight = null;
                        //Need to double cooldown here to prevent us from changing the next tick due to the
                        //Opposite light being red.  This prevents us from grabbing the "next" cycle.
                        currentCooldown = currentLight.equals(getRedLight()) ? allRedTime + 20 : 0;
                    } else {
                        currentCooldown = getSignalCooldown();
                    }
                }
            } else if (!stateChangeRequested) {
                if (requestedLight == null && currentCooldown == 0) {
                    //Do either time or trigger-based logic.
                    if (!currentLight.equals(getGreenLight())) {
                        if (timedMode) {
                            //If we are a signal, and not currently changing, and are not green, try to turn green.
                            if (direction.equals(SignalDirection.CENTER)) {
                                stateChangeRequested = true;
                            }
                        } else {
                            //See if we have a vehicle in our intersection bounds and need to change other signals.
                            //We only do this once every 2 seconds, and only if we aren't a main-central intersection.
                            if (ticksExisted % 40 == 0) {
                                if (isMainSignal && direction.equals(SignalDirection.CENTER)) {
                                    //Just wait until the other signals don't have any cooldown, then set them red.
                                    stateChangeRequested = true;
                                } else {
                                    for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
                                        Point3D adjustedPos = vehicle.position.copy().subtract(intersectionCenterPoint).reOrigin(axis.rotation);
                                        if (adjustedPos.x > signalLineCenter.x - signalLineWidth / 2D && adjustedPos.x < signalLineCenter.x + signalLineWidth / 2D && adjustedPos.z > signalLineCenter.z && adjustedPos.z < signalLineCenter.z + 16) {
                                            //Vehicle present.  If we are blocked, send the respective signal states to the other signals to change them.
                                            //Flag this signal as pending changes to blocked signals to avoid checking until those signals change.
                                            stateChangeRequested = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (stateChangeRequested) {
                //Check for blocking signals.  If some are present, don't change state.
                //Instead, set those to red if we can, and change when able.
                boolean foundBlockingSignal = false;
                for (Set<SignalGroup> signalGroupSet : signalGroups.values()) {
                    for (SignalGroup otherSignal : signalGroupSet) {
                        if (!otherSignal.currentLight.equals(otherSignal.getRedLight()) && isSignalBlocking(otherSignal)) {
                            //Found blocking signal.  Set to red if able.  Don't do any further checks.
                            foundBlockingSignal = true;
                            if (otherSignal.requestedLight == null && otherSignal.currentCooldown == 0) {
                                otherSignal.requestedLight = otherSignal.getRedLight();
                                otherSignal.currentCooldown = otherSignal.getSignalCooldown();
                            }
                        }
                    }
                }
                if (!foundBlockingSignal) {
                    //No blocking signals, or signals were just un-blocked.  Do state-change.
                    requestedLight = getGreenLight();
                    currentCooldown = getSignalCooldown();
                    stateChangeRequested = false;
                }
            }
        }

        protected abstract LightType getNextLight();

        protected abstract LightType getRedLight();

        protected abstract LightType getGreenLight();

        protected abstract int getSignalCooldown();

        protected abstract boolean isSignalBlocking(SignalGroup otherSignal);

        protected IWrapperNBT getData() {
            IWrapperNBT data = InterfaceManager.coreInterface.getNewNBTWrapper();
            data.setString("currentLight", currentLight.name());
            if (requestedLight != null) {
                data.setString("requestedLight", requestedLight.name());
            }
            data.setInteger("currentCooldown", currentCooldown);
            return data;
        }
    }

    private class SignalGroupCenter extends SignalGroup {

        private SignalGroupCenter(Axis axis, IWrapperNBT data) {
            super(axis, SignalDirection.CENTER, data);
            this.requestedLight = LightType.STOP_LIGHT;
        }

        @Override
        protected LightType getNextLight() {
            switch (currentLight) {
                case GO_LIGHT:
                    return LightType.CAUTION_LIGHT;
                case CAUTION_LIGHT:
                    return LightType.STOP_LIGHT;
                case STOP_LIGHT:
                    return LightType.GO_LIGHT;
                default:
                    return null;
            }
        }

        @Override
        protected LightType getRedLight() {
            return LightType.STOP_LIGHT;
        }

        @Override
        protected LightType getGreenLight() {
            return LightType.GO_LIGHT;
        }

        @Override
        protected int getSignalCooldown() {
            switch (currentLight) {
                case GO_LIGHT:
                    return isMainSignal ? greenMainTime : greenCrossTime;
                case CAUTION_LIGHT:
                    return isMainSignal ? yellowMainTime : yellowCrossTime;
                case STOP_LIGHT:
                    return allRedTime;
                default:
                    return 0;
            }
        }

        @Override
        protected boolean isSignalBlocking(SignalGroup otherSignal) {
            switch (Axis.getFromRotation(otherSignal.axis.rotation.angles.y - axis.rotation.angles.y, true)) {
                case SOUTH: { //Same direction.
                    return false;
                }
                case EAST: { //Other signal to the right.
                    switch (otherSignal.direction) {
                        case CENTER:
                        case RIGHT:
                            return true;
                        case LEFT:
                            return !isRightHandDrive;
                    }
                }
                case NORTH: { //Opposite direction.
                    switch (otherSignal.direction) {
                        case CENTER:
                            return false;
                        case LEFT:
                            return !isRightHandDrive;
                        case RIGHT:
                            return isRightHandDrive;
                    }
                }
                case WEST: { //Other signal to the left.
                    switch (otherSignal.direction) {
                        case CENTER:
                        case LEFT:
                            return true;
                        case RIGHT:
                            return isRightHandDrive;
                    }
                }
                default:
                    return true; //Unknown direction.
            }
        }
    }

    private class SignalGroupLeft extends SignalGroup {

        private SignalGroupLeft(Axis axis, IWrapperNBT data) {
            super(axis, SignalDirection.LEFT, data);
            this.requestedLight = LightType.STOP_LIGHT_LEFT;
        }

        @Override
        protected LightType getNextLight() {
            switch (currentLight) {
                case GO_LIGHT_LEFT:
                    return LightType.CAUTION_LIGHT_LEFT;
                case CAUTION_LIGHT_LEFT:
                    return LightType.STOP_LIGHT_LEFT;
                case STOP_LIGHT_LEFT:
                    return LightType.GO_LIGHT_LEFT;
                default:
                    return null;
            }
        }

        @Override
        protected LightType getRedLight() {
            return LightType.STOP_LIGHT_LEFT;
        }

        @Override
        protected LightType getGreenLight() {
            return LightType.GO_LIGHT_LEFT;
        }

        @Override
        protected int getSignalCooldown() {
            switch (currentLight) {
                case GO_LIGHT_LEFT:
                    return (isMainSignal ? greenMainTime : greenCrossTime) / 4;
                case CAUTION_LIGHT_LEFT:
                    return (isMainSignal ? yellowMainTime : yellowCrossTime) / 4;
                case STOP_LIGHT_LEFT:
                    return allRedTime;
                default:
                    return 0;
            }
        }

        @Override
        protected boolean isSignalBlocking(SignalGroup otherSignal) {
            switch (Axis.getFromRotation(otherSignal.axis.rotation.angles.y - axis.rotation.angles.y, true)) {
                case SOUTH: { //Same direction.
                    return false;
                }
                case EAST: { //Other signal to the right.
                    switch (otherSignal.direction) {
                        case CENTER:
                            return true;
                        case LEFT:
                            return !isRightHandDrive;
                        case RIGHT:
                            return false;
                    }
                }
                case NORTH: { //Opposite direction.
                    switch (otherSignal.direction) {
                        case CENTER:
                            return !isRightHandDrive;
                        case LEFT:
                            return false;
                        case RIGHT:
                            return true;
                    }
                }
                case WEST: { //Other signal to the left.
                    switch (otherSignal.direction) {
                        case CENTER:
                        case LEFT:
                            return !isRightHandDrive;
                        case RIGHT:
                            return false;
                    }
                }
                default:
                    return true; //Unknown direction.
            }
        }
    }

    private class SignalGroupRight extends SignalGroup {

        private SignalGroupRight(Axis axis, IWrapperNBT data) {
            super(axis, SignalDirection.RIGHT, data);
            this.requestedLight = LightType.STOP_LIGHT_RIGHT;
        }

        @Override
        protected LightType getNextLight() {
            switch (currentLight) {
                case GO_LIGHT_RIGHT:
                    return LightType.CAUTION_LIGHT_RIGHT;
                case CAUTION_LIGHT_RIGHT:
                    return LightType.STOP_LIGHT_RIGHT;
                case STOP_LIGHT_RIGHT:
                    return LightType.GO_LIGHT_RIGHT;
                default:
                    return null;
            }
        }

        @Override
        protected LightType getRedLight() {
            return LightType.STOP_LIGHT_RIGHT;
        }

        @Override
        protected LightType getGreenLight() {
            return LightType.GO_LIGHT_RIGHT;
        }

        @Override
        protected int getSignalCooldown() {
            switch (currentLight) {
                case GO_LIGHT_RIGHT:
                    return (isMainSignal ? greenMainTime : greenCrossTime) / 4;
                case CAUTION_LIGHT_RIGHT:
                    return (isMainSignal ? yellowMainTime : yellowCrossTime) / 4;
                case STOP_LIGHT_RIGHT:
                    return allRedTime;
                default:
                    return 0;
            }
        }

        @Override
        protected boolean isSignalBlocking(SignalGroup otherSignal) {
            switch (Axis.getFromRotation(otherSignal.axis.rotation.angles.y - axis.rotation.angles.y, true)) {
                case SOUTH: { //Same direction.
                    return false;
                }
                case EAST: { //Other signal to the right.
                    switch (otherSignal.direction) {
                        case CENTER:
                            return true;
                        case LEFT:
                            return false;
                        case RIGHT:
                            return isRightHandDrive;
                    }
                }
                case NORTH: { //Opposite direction.
                    switch (otherSignal.direction) {
                        case CENTER:
                            return isRightHandDrive;
                        case LEFT:
                            return true;
                        case RIGHT:
                            return false;
                    }
                }
                case WEST: { //Other signal to the left.
                    switch (otherSignal.direction) {
                        case CENTER:
                            return true;
                        case LEFT:
                            return false;
                        case RIGHT:
                            return isRightHandDrive;
                    }
                }
                default:
                    return true; //Unknown direction.
            }
        }
    }

    public enum LightType {
        STOP_LIGHT,
        CAUTION_LIGHT,
        GO_LIGHT,

        STOP_LIGHT_LEFT,
        CAUTION_LIGHT_LEFT,
        GO_LIGHT_LEFT,

        STOP_LIGHT_RIGHT,
        CAUTION_LIGHT_RIGHT,
        GO_LIGHT_RIGHT;

        public final String lowercaseName;

        LightType() {
            this.lowercaseName = name().toLowerCase();
        }
    }

    public enum SignalDirection {
        CENTER,
        LEFT,
        RIGHT
    }
}
