package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityTowingChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;

/**Base entity class containing towing states and methods.
 * At this level as not all multiparts can be towed.
 * 
 * @author don_bruce
 */
public abstract class AEntityG_Towable<JSONDefinition extends AJSONPartProvider> extends AEntityF_Multipart<JSONDefinition>{
	//Connection data.
	public TowingConnection towedByConnection;
	public final Set<TowingConnection> towingConnections = new HashSet<TowingConnection>();
	private TowingConnection savedTowedByConnection;
	private final Set<TowingConnection> savedTowingConnections = new HashSet<TowingConnection>();
	public static final String TOWING_CONNECTION_REQUEST_VARIABLE = "connection_requested";
	
	public AEntityG_Towable(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		//Load towing data.
		WrapperNBT towData = data.getData("towedByConnection");
		if(towData != null){
			this.savedTowedByConnection = new TowingConnection(towData);
		}
		
		int towingConnectionCount = data.getInteger("towingConnectionCount");
		for(int i=0; i<towingConnectionCount; ++i){
			towData = data.getData("towingConnection" + i);
			if(towData != null){
				this.savedTowingConnections.add(new TowingConnection(towData));
			}
		}
	}
	
	@Override
	public void update(){
		super.update();
		//Do validity checks for towing variables.  We could do this whenever we disconnect,
		//but there are tons of ways this could happen.  The trailer could blow up, the 
		//part-hitch could have been blown up, the trailer could have gotten wrenched, the
		//part hitch could have gotten wrenched, etc.  And that doesn't even count what the
		//thing towing us could have done! 
		world.beginProfiling("EntityG_Level", true);
		if(towedByConnection != null){
			if(!towedByConnection.towingEntity.isValid){
				towedByConnection = null;
			}
		}
		if(!towingConnections.isEmpty()){
			//First functional expression here in the whole codebase, history in the making!
			towingConnections.removeIf(connection -> !connection.towedEntity.isValid);
		}
		
		//See if we need to link connections.
		//We need to wait on this in case the entity didn't load at the same time.
		//That being said, it may be the vehicle we are loading is in another chunk.
		//As such we wait only some time, and if we caon't find all entities, we remove
		//them from the listing of entities to find.
		//Only do this once a second, and if we hit 5 seconds, bail.
		if(savedTowedByConnection != null){
			if(ticksExisted%20 == 0){
				if(ticksExisted <= 100){
					try{
						if(savedTowedByConnection.initConnection(world)){
							towedByConnection = savedTowedByConnection;
							savedTowedByConnection = null;
						}
					}catch(Exception e){
						savedTowedByConnection = null;
						InterfaceCore.logError("Could not hook-up trailer to entity towing it.  Did the JSON or pack change?");
					}
				}else{
					savedTowedByConnection = null;
					InterfaceCore.logError("Could not hook-up trailer to entity towing it.  Did the JSON or pack change?");
				}
			}
		}
		if(!savedTowingConnections.isEmpty()){
			if(ticksExisted%20 == 0){
				if(ticksExisted <= 100){
					Iterator<TowingConnection> iterator = savedTowingConnections.iterator();
					while(iterator.hasNext()){
						TowingConnection savedTowingConnection = iterator.next();
						try{
							if(savedTowingConnection.initConnection(world)){
								towingConnections.add(savedTowingConnection);
								iterator.remove();
							}
						}catch(Exception e){
							iterator.remove();
							InterfaceCore.logError("Could not connect trailer(s) to the entity towing them.  Did the JSON or pack change?");
						}
					}
				}else{
					savedTowingConnections.clear();
					InterfaceCore.logError("Could not connect trailer(s) to the entity towing them.  Did the JSON or pack change?");
				}
			}
		}
		
		//If we have a connection request, handle it now.
		int connectionRequestIndex = (int) getVariable(TOWING_CONNECTION_REQUEST_VARIABLE);
		if(connectionRequestIndex != 0){
			if(!world.isClient()){
				//Don't handle requests on the client.  These get packets.
				handleConnectionRequest(this, connectionRequestIndex - 1);
			}
			setVariable(TOWING_CONNECTION_REQUEST_VARIABLE, 0);
		}
		//also check parts, in case they got a request.
		for(APart part : parts){
			connectionRequestIndex = (int) part.getVariable(TOWING_CONNECTION_REQUEST_VARIABLE);
			if(connectionRequestIndex != 0){
				if(!world.isClient()){
					//Don't handle requests on the client.  These get packets.
					handleConnectionRequest(part, connectionRequestIndex - 1);
				}
				part.setVariable(TOWING_CONNECTION_REQUEST_VARIABLE, 0);
			}
		}
		
		world.endProfiling();
	}
	
	/**
	 * Returns true if the main update call for this entity should be blocked.
	 * This allows the entity update to be delayed to a later point in the code.
	 */
	public boolean blockMainUpdateCall(){
		return savedTowedByConnection != null || towedByConnection != null;
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		//Check if this is a hookup or hitch variable.
		if(variable.startsWith("connection")){
			//Format is (hitch/hookup)_groupIndex_connectionIndex_animationType.
			TowingConnection foundConnection = null;
			String[] variableData = variable.split("_");
			if(variableData.length == 4){
				boolean isHookup = false;
				int groupIndex = Integer.valueOf(variableData[1]) - 1;
				int connectionIndex = Integer.valueOf(variableData[2]) - 1;
				if(towedByConnection != null){
					if(towedByConnection.hookupGroupIndex == groupIndex && towedByConnection.hookupConnectionIndex == connectionIndex){
						isHookup = true;
						foundConnection = towedByConnection;
					}
				}
				if(foundConnection == null && !towingConnections.isEmpty()){
					for(TowingConnection towingConnection : towingConnections){
						if(towingConnection.hookupGroupIndex == groupIndex && towingConnection.hookupConnectionIndex == connectionIndex){
							foundConnection = towingConnection;
							break;
						}
					}
				}
				if(foundConnection != null){
					switch(variableData[3]){
						case("connected"): return 1;
						case("pitch"): return isHookup ? new Point3D(0, 0, 1).rotate(foundConnection.towedVehicle.orientation).reOrigin(orientation).getAngles(false).x : new Point3D(0, 0, 1).rotate(foundConnection.towingVehicle.orientation).reOrigin(orientation).getAngles(false).x;
						case("yaw"): return isHookup ? new Point3D(0, 0, 1).rotate(foundConnection.towedVehicle.orientation).reOrigin(orientation).getAngles(false).y : new Point3D(0, 0, 1).rotate(foundConnection.towingVehicle.orientation).reOrigin(orientation).getAngles(false).y;
						case("roll"): return isHookup ? new Point3D(0, 0, 1).rotate(foundConnection.towedVehicle.orientation).reOrigin(orientation).getAngles(false).z : new Point3D(0, 0, 1).rotate(foundConnection.towingVehicle.orientation).reOrigin(orientation).getAngles(false).z;
					}
				}
			}
		}
		
		//Not a towing variable, check others.
		return super.getRawVariableValue(variable, partialTicks);
	}
	
    @Override
	public void updatePostMovement(){
    	super.updatePostMovement();
		//If we are towing entities, update them now.
		if(!towingConnections.isEmpty()){
			world.beginProfiling("TowedEntities", true);
			for(TowingConnection connection : towingConnections){
				connection.hitchPriorPosition.set(connection.hitchCurrentPosition);
				connection.hitchCurrentPosition.set(connection.hitchConnection.pos).rotate(connection.towingEntity.orientation).add(connection.towingEntity.position);
				connection.towedVehicle.update();
			}
			world.endProfiling();
		}
	}
	
	@Override
	public boolean canCollideWith(AEntityB_Existing entityToCollide){
		return super.canCollideWith(entityToCollide) && !matchesTowing(entityToCollide) && !matchesTowed(entityToCollide);
	}
	
	protected boolean matchesTowing(AEntityB_Existing entityToCollide){
		if(towedByConnection == null){
			return false;
		}else if(entityToCollide.equals(towedByConnection.towingVehicle)){
			return true;
		}else{
			return towedByConnection.towingVehicle.matchesTowing(entityToCollide);
		}
	}
	
	protected boolean matchesTowed(AEntityB_Existing entityToCollide){
		if(towingConnections.isEmpty()){
			return false;
		}else{
			for(TowingConnection connection : towingConnections){
				if(entityToCollide.equals(connection.towedVehicle)){
					return true;
				}else if(connection.towedVehicle.matchesTowed(entityToCollide)){
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Method block for handling a trailer connection request.
	 * If this request is made on a hookup group, then this entity will try to become towed by another entity.
	 * If this request is a hitch and NOT a hookup, then this entity will try to find an entity to tow.
	 * In all cases, the definer is on our side, either as ourselves or a part.
	 */
	private void handleConnectionRequest(AEntityE_Interactable<?> connectionDefiner, int connectionGroupIndex){
		JSONConnectionGroup requestedGroup = connectionDefiner.definition.connectionGroups.get(connectionGroupIndex);
		TowingConnection connectionToDisconnect = null;
		
		//Check if this is a connect or disconnect request.
		if(requestedGroup.isHookup && towedByConnection != null){
			connectionToDisconnect = towedByConnection;
		}else if(requestedGroup.isHitch){
			for(TowingConnection connection : towingConnections){
				if(connectionDefiner.equals(connection.towingEntity)){
					connectionToDisconnect = connection;
					break;
				}
			}
		}
		
		if(connectionToDisconnect == null){
			TrailerConnectionResult result;
			List<AEntityG_Towable<?>> entitiesToCheck = new ArrayList<AEntityG_Towable<?>>();
			entitiesToCheck.addAll(world.getEntitiesOfType(EntityVehicleF_Physics.class));
			
			if(requestedGroup.isHookup){
				//Find entity that can tow us.
				for(AEntityG_Towable<?> testEntity : entitiesToCheck){
					result = testEntity.checkIfTrailerCanConnect(testEntity, testEntity, -1, this, connectionDefiner, connectionGroupIndex);
					if(result.skip){
						for(APart testPart : testEntity.parts){
							result = testEntity.checkIfTrailerCanConnect(testEntity, testPart, -1, this, connectionDefiner, connectionGroupIndex);
							if(!result.skip){
								result.handlePacket(this);
								return;
							}
						}
					}else{
						result.handlePacket(this);
						return;
					}
				}
			}else{
				//Find an entity to tow.
				for(AEntityG_Towable<?> testEntity : entitiesToCheck){
					result = checkIfTrailerCanConnect(this, connectionDefiner, connectionGroupIndex, testEntity, testEntity, -1);
					if(result.skip){
						for(APart testPart : testEntity.parts){
							result = checkIfTrailerCanConnect(this, connectionDefiner, connectionGroupIndex, testEntity, testPart, -1);
							if(!result.skip){
								result.handlePacket(this);
								return;
							}
						}
					}else{
						result.handlePacket(this);
						return;
					}
				}
			}
			TrailerConnectionResult.NOTFOUND.handlePacket(this);
		}else{
			if(connectionToDisconnect.equals(towedByConnection)){
				towedByConnection.towingVehicle.disconnectTrailer(towedByConnection);
				TrailerConnectionResult.DISCONNECTED.handlePacket(this);
			}else{
				disconnectTrailer(connectionToDisconnect);
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
	 * 
	 */
	private TrailerConnectionResult checkIfTrailerCanConnect(AEntityG_Towable<?> hitchEntity, AEntityE_Interactable<?> hitchConnectionDefiner, int requestedHitchGroupIndex, AEntityG_Towable<?> hookupEntity, AEntityE_Interactable<?> hookupConnectionDefiner, int requestedHookupGroupIndex){
		//Make sure we should be connecting.
		if(hookupEntity.towedByConnection != null){
			return TrailerConnectionResult.ALREADY_TOWED; //Entity is already hooked up.
		}else if(hookupEntity.equals(hitchEntity)){
			return TrailerConnectionResult.FEEDBACK_LOOP; //We can't connect to ourself.
		}else if(hitchEntity.isAlreadyTowing(hitchEntity, hookupEntity)){
			return TrailerConnectionResult.FEEDBACK_LOOP; //Vehicle we want to connect to is towing us already somewhere, or is being towed by us.
		}
		
		//Init variables.
		boolean matchingConnection = false;
		boolean trailerInRange = false;
		
		//First make sure the entity is in-even somewhat close.
		if(hitchConnectionDefiner.position.isDistanceToCloserThan(hookupConnectionDefiner.position, 25)){
			//If we or the other entity don't have connection groups, don't bother checking.
			if(hitchConnectionDefiner.definition.connectionGroups != null && !hitchConnectionDefiner.definition.connectionGroups.isEmpty() && hookupConnectionDefiner.definition.connectionGroups != null && !hookupConnectionDefiner.definition.connectionGroups.isEmpty()){
				//Find the requested hitch connection index.
				for(int hitchGroupIndex=0; hitchGroupIndex<hitchConnectionDefiner.definition.connectionGroups.size(); ++hitchGroupIndex){
					JSONConnectionGroup hitchConnectionGroup = hitchConnectionDefiner.definition.connectionGroups.get(hitchGroupIndex);
					if(hitchConnectionGroup.isHitch && (requestedHitchGroupIndex == -1 || hitchGroupIndex == requestedHitchGroupIndex)){
						//Found valid hitch group, check for hookup.
						for(int hookupGroupIndex=0; hookupGroupIndex<hookupConnectionDefiner.definition.connectionGroups.size(); ++hookupGroupIndex){
							JSONConnectionGroup hookupConnectionGroup = hookupConnectionDefiner.definition.connectionGroups.get(hookupGroupIndex);
							if(hookupConnectionGroup.isHookup && (requestedHookupGroupIndex == -1 || hookupGroupIndex == requestedHookupGroupIndex)){
								//We can potentially connect these two entities with this hitch/hookup group
								//as they match our requested connection parameters.  See if we actually can.
								//Iterate though connections on the groups to find two that match types and are close enough.
								for(int hitchConnectionIndex=0; hitchConnectionIndex<hitchConnectionGroup.connections.size(); ++hitchConnectionIndex){
									JSONConnection hitchConnection = hitchConnectionGroup.connections.get(hitchConnectionIndex);
									Point3D hitchPos = hitchConnection.pos.copy().rotate(hitchConnectionDefiner.orientation).add(hitchConnectionDefiner.position);
									for(int hookupConnectionIndex=0; hookupConnectionIndex<hookupConnectionGroup.connections.size(); ++hookupConnectionIndex){
										JSONConnection hookupConnection = hookupConnectionGroup.connections.get(hookupConnectionIndex);
										Point3D hookupPos = hookupConnection.pos.copy().rotate(hookupConnectionDefiner.orientation).add(hookupConnectionDefiner.position);
										
										//Check if we are somewhat close before we check matching type.
										//This allows us to warn players that that hitch they are trying to connect to (close to)
										//is the wrong type.
										if(hitchPos.isDistanceToCloserThan(hookupPos, hitchConnection.distance + 10)){
											boolean validType = hitchConnection.type.equals(hookupConnection.type);
											boolean validDistance = hitchPos.isDistanceToCloserThan(hookupPos,  hitchConnection.distance + 5);
											if(validType && validDistance){
												connectTrailer(new TowingConnection(hitchConnectionDefiner, hitchGroupIndex, hitchConnectionIndex, hookupConnectionDefiner, hookupGroupIndex, hookupConnectionIndex));
												return TrailerConnectionResult.CONNECTED;
											}else if(validType){
												matchingConnection = true;
											}else if(validDistance){
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
		if(matchingConnection && !trailerInRange){
			return TrailerConnectionResult.TOOFAR;
		}else if(!matchingConnection && trailerInRange){
			return TrailerConnectionResult.WRONGHITCH;
		}else if(matchingConnection && trailerInRange){
			return TrailerConnectionResult.MISMATCH;
		}else{
			return TrailerConnectionResult.NOTFOUND;
		}
	}
	
	private boolean isAlreadyTowing(AEntityG_Towable<?> vehicleTowing, AEntityG_Towable<?> vehicleToTow){
		for(TowingConnection connection : towingConnections){
			if(connection.towedVehicle.equals(vehicleTowing) || connection.towedVehicle.equals(vehicleToTow)){
				return true; //We can't 
			}else{
				for(TowingConnection nextConnection : connection.towedVehicle.towingConnections){
					if(((AEntityG_Towable<?>) nextConnection.towedVehicle).isAlreadyTowing(vehicleTowing, vehicleToTow)){
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
	public void connectTrailer(TowingConnection connection){
		towingConnections.add(connection);
		connection.towedVehicle.towedByConnection = connection;
		//Need to set initial values to avoid bad-syncing.
		connection.hitchCurrentPosition.set(connection.hitchConnection.pos).rotate(connection.towingEntity.orientation).add(connection.towingEntity.position);
		connection.hookupCurrentPosition.set(connection.hookupConnection.pos).rotate(connection.towedEntity.orientation).add(connection.towedEntity.position);
		if(!world.isClient()){
			InterfacePacket.sendToAllClients(new PacketEntityTowingChange(connection, true));
		}
	}
	
	/**
	 * Method block for disconnecting the trailer with the passed-in connection from this entity.
	 */
	public void disconnectTrailer(TowingConnection connection){
		towingConnections.remove(connection);
		connection.towedVehicle.towedByConnection = null;
		if(!world.isClient()){
			InterfacePacket.sendToAllClients(new PacketEntityTowingChange(connection, false));
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
	public void disconnectAllConnections(){
		towingConnections.clear();
		towedByConnection = null;
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		//Save towing data.
		if(towedByConnection != null){
			data.setData("towedByConnection", towedByConnection.save(InterfaceCore.getNewNBTWrapper()));
		}
		
		int towingConnectionIndex = 0;
		for(TowingConnection towingEntry : towingConnections){
			data.setData("towingConnection" + (towingConnectionIndex++), towingEntry.save(InterfaceCore.getNewNBTWrapper()));
		}
		data.setInteger("towingConnectionCount", towingConnectionIndex);
		return data;
	}
	
	/**
	 * Emum for easier functions for trailer connections.
	 */
	private static enum TrailerConnectionResult{
		FEEDBACK_LOOP(true, JSONConfigLanguage.INTERACT_TRAILER_FEEDBACKLOOP),
		ALREADY_TOWED(true, JSONConfigLanguage.INTERACT_TRAILER_ALREADYTOWED),
		NOTFOUND(true, JSONConfigLanguage.INTERACT_TRAILER_NOTFOUND),
		TOOFAR(false, JSONConfigLanguage.INTERACT_TRAILER_TOOFAR),
		WRONGHITCH(false, JSONConfigLanguage.INTERACT_TRAILER_WRONGHITCH),
		MISMATCH(false, JSONConfigLanguage.INTERACT_TRAILER_MISMATCH),
		CONNECTED(false, JSONConfigLanguage.INTERACT_TRAILER_CONNECTED),
		DISCONNECTED(false, JSONConfigLanguage.INTERACT_TRAILER_DISCONNECTED);
		
		private boolean skip;
		private LanguageEntry language;
		
		private TrailerConnectionResult(boolean skip, LanguageEntry language){
			this.skip = skip;
			this.language = language;
		}
		
		private void handlePacket(AEntityG_Towable<?> messageSource){
			for(WrapperEntity entity : messageSource.world.getEntitiesWithin(new BoundingBox(messageSource.position, 16, 16, 16))){
				if(entity instanceof WrapperPlayer){
					((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, language));
				}
			}
		}
	}
}
