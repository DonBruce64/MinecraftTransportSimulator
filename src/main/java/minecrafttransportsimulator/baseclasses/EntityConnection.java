package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Class for easier save/load of entity connections.  In all cases, the "other" entity is the
 * one the main entity is connected to.  So if this is present as a hookup connection, then
 * other would be the hitch connection on the other entity.  Similarly, if this is an entity that
 * is towing another, the the entity being towed will be the "other".
 * 
 * @author don_bruce
 */
public class EntityConnection{
	private final String entityUUID;
	private final String otherEntityUUID;
	public final int groupIndex;
	public final int connectionIndex;
	public final int otherGroupIndex;
	public final int otherConnectionIndex;
	private AEntityD_Interactable<?> entity;
	public AEntityD_Interactable<?> otherEntity;
	public AEntityD_Interactable<?> otherBaseEntity;
	public JSONConnection connection;
	public JSONConnection otherConnection;
	
	public EntityConnection(AEntityD_Interactable<?> entity, int groupIndex, int connectionIndex, AEntityD_Interactable<?> otherEntity, int otherGroupIndex, int otherConnectionIndex){
		this.entityUUID = entity.uniqueUUID;
		this.otherEntityUUID = otherEntity.uniqueUUID;
		this.groupIndex = groupIndex;
		this.connectionIndex = connectionIndex;
		this.otherGroupIndex = otherGroupIndex;
		this.otherConnectionIndex = otherConnectionIndex;
		this.entity = entity;
		this.otherEntity = otherEntity;
		setConnection(entity.world);
	}
	
	public EntityConnection(WrapperNBT data){
		this.entityUUID = data.getString("entityUUID");
		this.otherEntityUUID = data.getString("otherEntityUUID");
		this.groupIndex = data.getInteger("groupIndex");
		this.connectionIndex = data.getInteger("connectionIndex");
		this.otherGroupIndex = data.getInteger("otherGroupIndex");
		this.otherConnectionIndex = data.getInteger("otherConnectionIndex");
	}
	
	public boolean setConnection(WrapperWorld world){
		if(entity == null){
			entity = AEntityA_Base.getEntity(world, entityUUID);
		}
		if(entity != null){
			connection = entity.definition.connectionGroups.get(groupIndex).connections.get(connectionIndex);
		}
		if(otherEntity == null){
			otherEntity = AEntityA_Base.getEntity(world, otherEntityUUID);
		}
		if(otherEntity != null){
			otherBaseEntity = otherEntity instanceof APart ? ((APart) otherEntity).entityOn : otherEntity;
			otherConnection = otherEntity.definition.connectionGroups.get(otherGroupIndex).connections.get(otherConnectionIndex);
		}
		return connection != null && otherConnection != null;
	}
	
	public Point3d getOffset(){
		return connection.pos.copy();
	}
	
	public Point3d getOtherOffset(){
		return otherConnection.pos.copy();
	}
	
	public EntityConnection getInverse(){
		return new EntityConnection(otherEntity, otherGroupIndex, otherConnectionIndex, entity, groupIndex, connectionIndex);
	}
	
	public WrapperNBT getData(){
		WrapperNBT data = new WrapperNBT();
		data.setString("entityUUID", entityUUID);
		data.setString("otherEntityUUID", otherEntityUUID);
		data.setInteger("groupIndex", groupIndex);
		data.setInteger("otherGroupIndex", otherGroupIndex);
		data.setInteger("connectionIndex", connectionIndex);
		data.setInteger("otherConnectionIndex", otherConnectionIndex);
		return data;
	}
}
