package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPanel;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityTowingChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Base entity class containing towing states and methods.
 * At this level as not all multiparts can be towed.
 *
 * @author don_bruce
 */
public abstract class AEntityG_Towable<JSONDefinition extends AJSONPartProvider> extends AEntityF_Multipart<JSONDefinition> {
    //Connection data.
    public TowingConnection towedByConnection;
    private TowingConnection savedTowedByConnection;
    public final List<TowingConnection> towingConnections = new ArrayList<>();
    private final List<TowingConnection> savedTowingConnections = new ArrayList<>();
    private final List<TowingConnection> disconnectedTowingConnections = new ArrayList<>();
    private final List<TowingConnection> savedDisconnectedTowingConnections = new ArrayList<>();

    public AEntityG_Towable(AWrapperWorld world, IWrapperPlayer placingPlayer, AItemSubTyped<JSONDefinition> item, IWrapperNBT data) {
        super(world, placingPlayer, item, data);
        if (data != null) {
            //Load towing data.
            IWrapperNBT towData = data.getData("towedByConnection");
            if (towData != null) {
                this.savedTowedByConnection = new TowingConnection(towData);
            }

            int towingConnectionCount = data.getInteger("towingConnectionCount");
            for (int i = 0; i < towingConnectionCount; ++i) {
                towData = data.getData("towingConnection" + i);
                if (towData != null) {
                    this.savedTowingConnections.add(new TowingConnection(towData));
                }
            }

            //Load disabled connections.
            towingConnectionCount = data.getInteger("disconnectedTowingConnectionCount");
            for (int i = 0; i < towingConnectionCount; ++i) {
                towData = data.getData("disconnectedTowingConnection" + i);
                if (towData != null) {
                    this.savedDisconnectedTowingConnections.add(new TowingConnection(towData));
                }
            }
        }
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("EntityG_Level", true);

        //Do validity checks for towing variables.  We could do this whenever we disconnect,
        //but there are tons of ways this could happen.  The trailer could blow up, the 
        //part-hitch could have been blown up, the trailer could have gotten wrenched, the
        //part hitch could have gotten wrenched, etc.  And that doesn't even count what the
        //thing towing us could have done! 
        if (towedByConnection != null) {
            if (!towedByConnection.towingEntity.isValid) {
                towedByConnection.towingVehicle.disconnectTrailer(towedByConnection.towingVehicle.towingConnections.indexOf(towedByConnection));
            }
        }
        if (!towingConnections.isEmpty()) {
            //First functional expression here in the whole codebase, history in the making!
            //Is what this used to say before we fixed things...
            for (int i = 0; i < towingConnections.size(); ++i) {
                TowingConnection connection = towingConnections.get(i);
                if (!connection.towedEntity.isValid) {
                    disconnectTrailer(i);
                    --i;
                }
            }
        }

        //See if we need to link connections.
        //We need to wait on this in case the entity didn't load at the same time.
        //That being said, it may be the vehicle we are loading is in another chunk.
        //As such we wait only some time, and if we caon't find all entities, we remove
        //them from the listing of entities to find.
        //Only do this once a second, and if we hit 5 seconds, bail.
        //We do this linking both ways, as we don't know if the towed vehicle or the towing vehicle will load first.
        if (savedTowedByConnection != null) {
            if (ticksExisted % 20 == 0 && ticksExisted > 0) {
                if (ticksExisted <= 100 || world.isClient()) {
                    if (savedTowedByConnection.initConnection(world)) {
                        savedTowedByConnection.towingVehicle.connectTrailer(savedTowedByConnection, false);
                    }
                } else {
                    savedTowedByConnection = null;
                    InterfaceManager.coreInterface.logError("Could not hook-up trailer to entity towing it.  Did the JSON or pack change?");
                }
            }
        }

        if (!savedTowingConnections.isEmpty()) {
            if (ticksExisted % 20 == 0 && ticksExisted > 0) {
                if (ticksExisted <= 100 || world.isClient()) {
                    for (int i = 0; i < savedTowingConnections.size(); ++i) {
                        TowingConnection savedTowingConnection = savedTowingConnections.get(i);
                        try {
                            if (savedTowingConnection.initConnection(world)) {
                                connectTrailer(savedTowingConnection, false);
                                --i;
                            }
                        } catch (Exception e) {
                            InterfaceManager.coreInterface.logError("Could not connect a trailer to the entity towing it.  Did the JSON or pack change?");
                        }
                    }
                } else {
                    savedTowingConnections.clear();
                    InterfaceManager.coreInterface.logError("Could not connect trailer(s) to the entity towing them.  Did the JSON or pack change?");
                }
            }
        }
        if (!savedDisconnectedTowingConnections.isEmpty()) {
            if (ticksExisted % 20 == 0) {
                if (ticksExisted <= 100 || world.isClient()) {
                    for (int i = 0; i < savedDisconnectedTowingConnections.size(); ++i) {
                        TowingConnection savedTowingConnection = savedDisconnectedTowingConnections.get(i);
                        try {
                            if (savedTowingConnection.initConnection(world)) {
                                disconnectedTowingConnections.add(savedTowingConnection);
                                --i;
                            }
                        } catch (Exception e) {
                            InterfaceManager.coreInterface.logError("Could not restore saved disconnected trailer(s) to the entity towing them.  Did the JSON or pack change?");
                        }
                    }
                } else {
                    savedDisconnectedTowingConnections.clear();
                    InterfaceManager.coreInterface.logError("Could not restore saved disconnected trailer(s) to the entity towing them.  Did the JSON or pack change?");
                }
            }
        }

        //If we have a connection request, handle it now.
        if (towingConnectionVar.isActive) {
        	//Don't handle requests on the client.  These get packets.
            if (!world.isClient()) {
                handleConnectionRequest(this, (int) towingConnectionVar.currentValue - 1);
            }
            towingConnectionVar.setTo(0, false);
        } else if (!world.isClient() && !snapConnectionIndexes.isEmpty() && ticksExisted % (10 / snapConnectionIndexes.size()) == 0) {
            if (++lastSnapConnectionTried == snapConnectionIndexes.size()) {
                lastSnapConnectionTried = 0;
            }
            if (!connectionGroupsIndexesInUse.contains(lastSnapConnectionTried)) {
            	towingConnectionVar.setTo(lastSnapConnectionTried + 1, false);
                bypassConnectionPacket = true;
            }
        }
        //Also check parts, in case they got a request.
        for (APart part : allParts) {
            if (part.towingConnectionVar.isActive) {
            	//Don't handle requests on the client.  These get packets.
                if (!world.isClient()) {
                    handleConnectionRequest(part, (int) part.towingConnectionVar.currentValue - 1);
                }
                part.towingConnectionVar.setTo(0, false);
            } else if (!world.isClient() && towedByConnection == null && !part.snapConnectionIndexes.isEmpty() && part.ticksExisted % (10 / part.snapConnectionIndexes.size()) == 0) {
                if (++part.lastSnapConnectionTried == part.snapConnectionIndexes.size()) {
                    part.lastSnapConnectionTried = 0;
                }
                if (!part.connectionGroupsIndexesInUse.contains(part.lastSnapConnectionTried)) {
                	part.towingConnectionVar.setTo(part.lastSnapConnectionTried + 1, false);
                    part.bypassConnectionPacket = true;
                    break;
                }
            }
        }

        world.endProfiling();
    }

    /**
     * Returns true if the main update call for this entity should be blocked.
     * This allows the entity update to be delayed to a later point in the code.
     */
    public boolean blockMainUpdateCall() {
        return towedByConnection != null;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        if (variable.startsWith("connection")) {
            //Format is (hitch/hookup)_groupIndex_connectionIndex_animationType.
            TowingConnection foundConnection = null;
            String[] variableData = variable.split("_");
            if (variableData.length >= 3) {
                boolean isHookup = false;
                int groupIndex = Integer.parseInt(variableData[1]) - 1;
                int connectionIndex = variableData.length == 4 ? Integer.parseInt(variableData[2]) - 1 : -1;
                if (towedByConnection != null) {
                    if (towedByConnection.hookupGroupIndex == groupIndex && (connectionIndex == -1 || towedByConnection.hookupConnectionIndex == connectionIndex)) {
                        isHookup = true;
                        foundConnection = towedByConnection;
                    }
                }
                if (foundConnection == null && !towingConnections.isEmpty()) {
                    for (TowingConnection towingConnection : towingConnections) {
                        if (towingConnection.hitchGroupIndex == groupIndex && (connectionIndex == -1 || towingConnection.hitchConnectionIndex == connectionIndex)) {
                            foundConnection = towingConnection;
                            break;
                        }
                    }
                }
                variable = variableData[variableData.length == 4 ? 3 : 2];
                final boolean finalIsHookup = isHookup;
                final TowingConnection finalfoundConnection = foundConnection;
                if (foundConnection != null) {
                    switch (variable) {
                        case ("connected"):
                            return ComputedVariable.ONE_VARIABLE;
                        case ("pitch"): {
                            final Point3D helperPoint = new Point3D();
                            return new ComputedVariable(this, variable, partialTicks -> {
                                if (finalIsHookup) {
                                    return helperPoint.set(0, 0, 1).rotate(finalfoundConnection.towingVehicle.orientation).reOrigin(orientation).getAngles(false).x;
                                } else {
                                    return helperPoint.set(0, 0, 1).rotate(finalfoundConnection.towedVehicle.orientation).reOrigin(orientation).getAngles(false).x;
                                }
                            }, false);
                        }
                        case ("yaw"): {
                            final Point3D helperPoint = new Point3D();
                            return new ComputedVariable(this, variable, partialTicks -> {
                                if (finalIsHookup) {
                                    return helperPoint.set(0, 0, 1).rotate(finalfoundConnection.towingVehicle.orientation).reOrigin(orientation).getAngles(false).y;
                                } else {
                                    return helperPoint.set(0, 0, 1).rotate(finalfoundConnection.towedVehicle.orientation).reOrigin(orientation).getAngles(false).y;
                                }
                            }, false);
                        }
                        case ("roll"): {
                            final Point3D helperPoint = new Point3D();
                            return new ComputedVariable(this, variable, partialTicks -> {
                                if (finalIsHookup) {
                                    return helperPoint.set(0, 0, 1).rotate(finalfoundConnection.towingVehicle.orientation).reOrigin(orientation).getAngles(false).z;
                                } else {
                                    return helperPoint.set(0, 0, 1).rotate(finalfoundConnection.towedVehicle.orientation).reOrigin(orientation).getAngles(false).z;
                                }
                            }, false);
                        }
                    }
                } else if (variable.equals("present")) {
                    return new ComputedVariable(this, variable, partialTicks -> definition.connectionGroups != null && definition.connectionGroups.size() > groupIndex ? 1 : 0, true);
                }
            }

            //Invalid variable, or connection not yet set.
            return ComputedVariable.ZERO_VARIABLE;
        } else {
            return super.createComputedVariable(variable, createDefaultIfNotPresent);
        }
    }

    @Override
    public void doPostUpdateLogic() {
        super.doPostUpdateLogic();
        //If we are towing entities, update them now.
        if (!towingConnections.isEmpty()) {
            world.beginProfiling("TowedEntities", true);
            for (TowingConnection connection : towingConnections) {
                connection.hitchPriorPosition.set(connection.hitchCurrentPosition);
                connection.hitchCurrentPosition.set(connection.hitchConnection.pos).multiply(connection.towingEntity.scale).rotate(connection.towingEntity.orientation).add(connection.towingEntity.position);
                connection.towedVehicle.update();
                connection.towedVehicle.doPostUpdateLogic();
                //If the towed vehicle is no longer towed, it means we disconnected from this entity.
                //Bail rather than continue to avoid a CME.
                if (connection.towedVehicle.towedByConnection == null) {
                    break;
                }
            }
            world.endProfiling();
        }
        if (!disconnectedTowingConnections.isEmpty()) {
            world.beginProfiling("DisconnectedEntities", true);
            for (TowingConnection connection : disconnectedTowingConnections) {
                connection.hitchCurrentPosition.set(connection.hitchConnection.pos).multiply(connection.towingEntity.scale).rotate(connection.towingEntity.orientation).add(connection.towingEntity.position);
                connection.hookupCurrentPosition.set(connection.hookupConnection.pos).multiply(connection.towedEntity.scale).rotate(connection.towedEntity.orientation).add(connection.towedEntity.position);
                if (!connection.towingEntity.isValid || !connection.towedEntity.isValid || !connection.hitchCurrentPosition.isDistanceToCloserThan(connection.hookupCurrentPosition, connection.hitchConnection.distance * 2)) {
                    disconnectedTowingConnections.remove(connection);
                    connection.towingEntity.connectionGroupsIndexesInUse.remove(connection.hitchGroupIndex);
                    connection.towedEntity.connectionGroupsIndexesInUse.remove(connection.hookupGroupIndex);
                    break;
                }
            }
            world.endProfiling();
        }
    }

    @Override
    public boolean canCollideWith(AEntityB_Existing entityToCollide) {
        return super.canCollideWith(entityToCollide) && !(entityToCollide instanceof AEntityG_Towable) || (!areWeTowedBy((AEntityG_Towable<?>) entityToCollide) && !areWeTowing((AEntityG_Towable<?>) entityToCollide));
    }

    protected boolean areWeTowedBy(AEntityG_Towable<?> entity) {
        if (towedByConnection == null) {
            return false;
        } else if (entity == towedByConnection.towingVehicle) {
            return true;
        } else {
            return towedByConnection.towingVehicle.areWeTowedBy(entity);
        }
    }

    protected boolean areWeTowing(AEntityG_Towable<?> entity) {
        if (!towingConnections.isEmpty()) {
            for (TowingConnection connection : towingConnections) {
                if (entity == connection.towedVehicle) {
                    return true;
                } else if (connection.towedVehicle.areWeTowing(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method block for handling a trailer connection request.
     * If this request is on a hitch, then this entity will try to find an entity to tow.
     * If this request is on a hookup, and we aren't currently being towed, then this entity will try to become towed by another entity.
     * In all cases, the definer is on our side, either as ourselves or a part.
     */
    private void handleConnectionRequest(AEntityE_Interactable<?> connectionDefiner, int connectionGroupIndex) {
        JSONConnectionGroup requestedGroup = connectionDefiner.definition.connectionGroups.get(connectionGroupIndex);
        TowingConnection connectionToDisconnect = null;

        //Check if this is a connect or disconnect request.
        if (requestedGroup.isHitch) {
            for (TowingConnection connection : towingConnections) {
                if (connectionDefiner.equals(connection.towingEntity) && connection.hitchGroupIndex == connectionGroupIndex) {
                    connectionToDisconnect = connection;
                    break;
                }
            }
        }
        if (requestedGroup.isHookup && towedByConnection != null && towedByConnection.hookupGroupIndex == connectionGroupIndex) {
            connectionToDisconnect = towedByConnection;
        }

        if (connectionToDisconnect == null) {
            TrailerConnectionResult result;
            List<AEntityG_Towable<?>> entitiesToCheck = new ArrayList<>(world.getEntitiesOfType(EntityVehicleF_Physics.class));

            if (requestedGroup.isHitch) {
                //Find an entity to tow.
                for (AEntityG_Towable<?> testEntity : entitiesToCheck) {
                    result = checkIfTrailerCanConnect(this, connectionDefiner, connectionGroupIndex, testEntity, testEntity, -1);
                    if (result.skip) {
                        for (APart testPart : testEntity.allParts) {
                            result = checkIfTrailerCanConnect(this, connectionDefiner, connectionGroupIndex, testEntity, testPart, -1);
                            if (!result.skip) {
                                result.handlePacket(connectionDefiner);
                                return;
                            }
                        }
                    } else {
                        result.handlePacket(connectionDefiner);
                        return;
                    }
                }
            }
            if (requestedGroup.isHookup) {
                //Find entity that can tow us.
                for (AEntityG_Towable<?> testEntity : entitiesToCheck) {
                    result = testEntity.checkIfTrailerCanConnect(testEntity, testEntity, -1, this, connectionDefiner, connectionGroupIndex);
                    if (result.skip) {
                        for (APart testPart : testEntity.allParts) {
                            result = testEntity.checkIfTrailerCanConnect(testEntity, testPart, -1, this, connectionDefiner, connectionGroupIndex);
                            if (!result.skip) {
                                result.handlePacket(connectionDefiner);
                                return;
                            }
                        }
                    } else {
                        result.handlePacket(connectionDefiner);
                        return;
                    }
                }
            }
            TrailerConnectionResult.NOTFOUND.handlePacket(connectionDefiner);
        } else {
            if (connectionToDisconnect.equals(towedByConnection)) {
                towedByConnection.towingVehicle.disconnectTrailer(towedByConnection.towingVehicle.towingConnections.indexOf(towedByConnection));
                TrailerConnectionResult.DISCONNECTED.handlePacket(this);
            } else {
                disconnectTrailer(towingConnections.indexOf(connectionToDisconnect));
                TrailerConnectionResult.DISCONNECTED.handlePacket(this);
            }
        }
    }

    /**
     * Checks if the other entity can be connected to this entity.  The other entity may be a trailer we
     * want to connect, or it may be a trailer that has requested to connect to us.  In all cases, we
     * are the main entity and will start towing the trailer if the connection is successful.
     * For connection indexes, a -1 will allow for any index, while a value other than -1 will try to connect
     * using only that connection group.
     */
    private TrailerConnectionResult checkIfTrailerCanConnect(AEntityG_Towable<?> hitchEntity, AEntityE_Interactable<?> hitchConnectionDefiner, int requestedHitchGroupIndex, AEntityG_Towable<?> hookupEntity, AEntityE_Interactable<?> hookupConnectionDefiner, int requestedHookupGroupIndex) {
        //Make sure we should be connecting.
        if (hitchConnectionDefiner.connectionGroupsIndexesInUse.contains(requestedHitchGroupIndex) || hookupConnectionDefiner.connectionGroupsIndexesInUse.contains(requestedHookupGroupIndex)) {
            return TrailerConnectionResult.ALREADY_TOWED; //Entity is either connected, or just disconnected.
        } else if (hookupEntity.towedByConnection != null) {
            return TrailerConnectionResult.ALREADY_TOWED; //Entity is already hooked up.
        } else if (hookupEntity.equals(hitchEntity)) {
            return TrailerConnectionResult.FEEDBACK_LOOP; //We can't connect to ourself.
        } else if (hitchEntity.isAlreadyTowing(hitchEntity, hookupEntity)) {
            return TrailerConnectionResult.FEEDBACK_LOOP; //Vehicle we want to connect to is towing us already somewhere, or is being towed by us.
        }

        //Init variables.
        boolean matchingConnection = false;
        boolean trailerInRange = false;

        //First make sure the entity is even somewhat close.
        if (hitchConnectionDefiner.position.isDistanceToCloserThan(hookupConnectionDefiner.position, 25)) {
            //If we or the other entity don't have connection groups, don't bother checking.
            if (hitchConnectionDefiner.definition.connectionGroups != null && !hitchConnectionDefiner.definition.connectionGroups.isEmpty() && hookupConnectionDefiner.definition.connectionGroups != null && !hookupConnectionDefiner.definition.connectionGroups.isEmpty()) {
                //Find the requested hitch connection index.
                for (int hitchGroupIndex = 0; hitchGroupIndex < hitchConnectionDefiner.definition.connectionGroups.size(); ++hitchGroupIndex) {
                    JSONConnectionGroup hitchConnectionGroup = hitchConnectionDefiner.definition.connectionGroups.get(hitchGroupIndex);
                    if (hitchConnectionGroup.isHitch && (requestedHitchGroupIndex == -1 || hitchGroupIndex == requestedHitchGroupIndex)) {
                        //Found valid hitch group, check for hookup.
                        for (int hookupGroupIndex = 0; hookupGroupIndex < hookupConnectionDefiner.definition.connectionGroups.size(); ++hookupGroupIndex) {
                            JSONConnectionGroup hookupConnectionGroup = hookupConnectionDefiner.definition.connectionGroups.get(hookupGroupIndex);
                            if (hookupConnectionGroup.isHookup && (requestedHookupGroupIndex == -1 || hookupGroupIndex == requestedHookupGroupIndex)) {
                                //We can potentially connect these two entities with this hitch/hookup group
                                //as they match our requested connection parameters.  See if we actually can.
                                //Iterate though connections on the groups to find two that match types and are close enough.
                                for (int hitchConnectionIndex = 0; hitchConnectionIndex < hitchConnectionGroup.connections.size(); ++hitchConnectionIndex) {
                                    JSONConnection hitchConnection = hitchConnectionGroup.connections.get(hitchConnectionIndex);
                                    Point3D hitchPos = hitchConnection.pos.copy().multiply(hitchConnectionDefiner.scale).rotate(hitchConnectionDefiner.orientation).add(hitchConnectionDefiner.position);
                                    for (int hookupConnectionIndex = 0; hookupConnectionIndex < hookupConnectionGroup.connections.size(); ++hookupConnectionIndex) {
                                        JSONConnection hookupConnection = hookupConnectionGroup.connections.get(hookupConnectionIndex);
                                        Point3D hookupPos = hookupConnection.pos.copy().multiply(hookupConnectionDefiner.scale).rotate(hookupConnectionDefiner.orientation).add(hookupConnectionDefiner.position);

                                        //Check if we are somewhat close before we check matching type.
                                        //This allows us to warn players that that hitch they are trying to connect to (close to)
                                        //is the wrong type.
                                        if (hitchPos.isDistanceToCloserThan(hookupPos, hitchConnection.distance + 10)) {
                                            boolean validType = hitchConnection.type.equals(hookupConnection.type);
                                            boolean validDistance = hitchPos.isDistanceToCloserThan(hookupPos, hitchConnection.distance);
                                            if (validType && validDistance) {
                                                connectTrailer(new TowingConnection(hitchConnectionDefiner, hitchGroupIndex, hitchConnectionIndex, hookupConnectionDefiner, hookupGroupIndex, hookupConnectionIndex), true);
                                                return TrailerConnectionResult.CONNECTED;
                                            } else if (validType) {
                                                matchingConnection = true;
                                            } else if (validDistance) {
                                                trailerInRange = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //Return results.
        if (matchingConnection && !trailerInRange) {
            return TrailerConnectionResult.TOOFAR;
        } else if (!matchingConnection && trailerInRange) {
            return TrailerConnectionResult.WRONGHITCH;
        } else if (matchingConnection && trailerInRange) {
            return TrailerConnectionResult.MISMATCH;
        } else {
            return TrailerConnectionResult.NOTFOUND;
        }
    }

    private boolean isAlreadyTowing(AEntityG_Towable<?> vehicleTowing, AEntityG_Towable<?> vehicleToTow) {
        for (TowingConnection connection : towingConnections) {
            if (connection.towedVehicle.equals(vehicleTowing) || connection.towedVehicle.equals(vehicleToTow)) {
                return true;
            } else {
                for (TowingConnection nextConnection : connection.towedVehicle.towingConnections) {
                    if (((AEntityG_Towable<?>) nextConnection.towedVehicle).isAlreadyTowing(vehicleTowing, vehicleToTow)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Method block for connecting a trailer to this entity.
     */
    public void connectTrailer(TowingConnection connection, boolean notifyClient) {
        towingConnections.add(connection);
        connection.towedVehicle.towedByConnection = connection;
        connection.towingEntity.connectionGroupsIndexesInUse.add(connection.hitchGroupIndex);
        connection.towedEntity.connectionGroupsIndexesInUse.add(connection.hookupGroupIndex);

        //Need to set initial values to avoid bad-syncing.
        connection.hitchCurrentPosition.set(connection.hitchConnection.pos).rotate(connection.towingEntity.orientation).add(connection.towingEntity.position);
        connection.hookupCurrentPosition.set(connection.hookupConnection.pos).rotate(connection.towedEntity.orientation).add(connection.towedEntity.position);

        //Clear saved connection from other vehicle, if we have it.  If we don't, we'll double-connect.
        ((AEntityG_Towable<?>) connection.towedVehicle).savedTowedByConnection = null;
        savedTowingConnections.removeIf(testConnection -> connection.hitchConnectionGroup.equals(testConnection.hitchConnectionGroup) && connection.hitchConnectionIndex == testConnection.hitchConnectionIndex);

        //Clear connection variables, since our connections have changed and those affect them.
        computedVariables.entrySet().removeIf(mapEntry -> mapEntry.getKey().startsWith("connection"));

        //Handle connection update requests.
        if (!world.isClient()) {
            if (notifyClient) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityTowingChange(this, connection));
            }
        } else if (AGUIBase.activeInputGUI instanceof GUIPanel) {
            ((GUIPanel) AGUIBase.activeInputGUI).handleConnectionChange(connection);
        }
    }

    /**
     * Method block for disconnecting the specified trailer connection index from this entity.
     */
    public void disconnectTrailer(int connectionIndex) {
        TowingConnection connection = towingConnections.remove(connectionIndex);
        connection.towedVehicle.towedByConnection = null;

        if (connection.hitchConnectionGroup.isSnap || connection.hookupConnectionGroup.isSnap) {
            disconnectedTowingConnections.add(connection);
        } else {
            connection.towingEntity.connectionGroupsIndexesInUse.remove(connection.hitchGroupIndex);
            connection.towedEntity.connectionGroupsIndexesInUse.remove(connection.hookupGroupIndex);
        }
        
        computedVariables.entrySet().removeIf(mapEntry -> mapEntry.getKey().startsWith("connection"));

        if (!world.isClient()) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityTowingChange(this, connectionIndex));
        } else if (AGUIBase.activeInputGUI instanceof GUIPanel) {
            ((GUIPanel) AGUIBase.activeInputGUI).handleConnectionChange(connection);
        }
    }

    /**
     * Method-block for disconnecting all connections from this entity.  Used when this
     * entity is removed from the world to prevent lingering connections.  Mainly done in item form,
     * as during removal it will be marked invalid, so all entities connected to it will automatically
     * disconnect; this just ensures it won't try to re-connect to those entities if re-spawned.
     * As such, this method does NOT send packets to clients as it's assumed the entity will be gone
     * on those clients by the time the packet arrives.
     */
    public void disconnectAllConnections() {
        towingConnections.clear();
        towedByConnection = null;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        //Save towing data.
        if (towedByConnection != null) {
            data.setData("towedByConnection", towedByConnection.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        }

        int towingConnectionIndex = 0;
        for (TowingConnection towingEntry : towingConnections) {
            data.setData("towingConnection" + (towingConnectionIndex++), towingEntry.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        }
        for (TowingConnection towingEntry : savedTowingConnections) {
            data.setData("towingConnection" + (towingConnectionIndex++), towingEntry.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        }
        data.setInteger("towingConnectionCount", towingConnectionIndex);

        towingConnectionIndex = 0;
        for (TowingConnection towingEntry : disconnectedTowingConnections) {
            data.setData("disconnectedTowingConnection" + (towingConnectionIndex++), towingEntry.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        }
        data.setInteger("disconnectedTowingConnectionCount", towingConnectionIndex);

        return data;
    }

    /**
     * Emum for easier functions for trailer connections.
     */
    private enum TrailerConnectionResult {
        FEEDBACK_LOOP(true, LanguageSystem.INTERACT_TRAILER_FEEDBACKLOOP),
        ALREADY_TOWED(true, LanguageSystem.INTERACT_TRAILER_ALREADYTOWED),
        NOTFOUND(true, LanguageSystem.INTERACT_TRAILER_NOTFOUND),
        TOOFAR(true, LanguageSystem.INTERACT_TRAILER_TOOFAR),
        WRONGHITCH(true, LanguageSystem.INTERACT_TRAILER_WRONGHITCH),
        MISMATCH(true, LanguageSystem.INTERACT_TRAILER_MISMATCH),
        CONNECTED(false, LanguageSystem.INTERACT_TRAILER_CONNECTED),
        DISCONNECTED(false, LanguageSystem.INTERACT_TRAILER_DISCONNECTED);

        private final boolean skip;
        private final LanguageEntry language;

        TrailerConnectionResult(boolean skip, LanguageEntry language) {
            this.skip = skip;
            this.language = language;
        }

        private void handlePacket(AEntityE_Interactable<?> messageSource) {
            if (messageSource.bypassConnectionPacket) {
                messageSource.bypassConnectionPacket = false;
            } else {
                for (IWrapperPlayer player : messageSource.world.getPlayersWithin(new BoundingBox(messageSource.position, 16, 16, 16))) {
                    player.sendPacket(new PacketPlayerChatMessage(player, language));
                }
            }
        }
    }
}
