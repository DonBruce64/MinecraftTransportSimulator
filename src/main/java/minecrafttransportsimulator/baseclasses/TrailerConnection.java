package minecrafttransportsimulator.baseclasses;

import java.util.UUID;

import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Class for easier save/load of trailer connections.
 * 
 * @author don_bruce
 */
public class TrailerConnection{
	private final UUID hitchEntityUUID;
	private final UUID hookupEntityUUID;
	public final int hitchGroupIndex;
	public final int hitchConnectionIndex;
	public final int hookupGroupIndex;
	public final int hookupConnectionIndex;
	public AEntityE_Interactable<?> hitchEntity;
	public EntityVehicleF_Physics hitchVehicle;
	public AEntityE_Interactable<?> hookupEntity;
	public EntityVehicleF_Physics hookupVehicle;
	public JSONConnectionGroup hitchConnectionGroup;
	public JSONConnection hitchConnection;
	public JSONConnectionGroup hookupConnectionGroup;
	public JSONConnection hookupConnection;
	public Point3dPlus hitchPriorPosition = new Point3dPlus();
	public Point3dPlus hitchCurrentPosition = new Point3dPlus();
	public Point3dPlus hookupPriorPosition = new Point3dPlus();
	public Point3dPlus hookupCurrentPosition = new Point3dPlus();
	
	public TrailerConnection(AEntityE_Interactable<?> hitchEntity, int hitchGroupIndex, int hitchConnectionIndex, AEntityE_Interactable<?> hookupEntity, int hookupGroupIndex, int hookupConnectionIndex){
		this.hitchEntityUUID = hitchEntity.uniqueUUID;
		this.hookupEntityUUID = hookupEntity.uniqueUUID;
		this.hitchGroupIndex = hitchGroupIndex;
		this.hitchConnectionIndex = hitchConnectionIndex;
		this.hookupGroupIndex = hookupGroupIndex;
		this.hookupConnectionIndex = hookupConnectionIndex;
		this.hitchEntity = hitchEntity;
		this.hookupEntity = hookupEntity;
		setConnection(hitchEntity.world);
	}
	
	public TrailerConnection(WrapperNBT data){
		this.hitchEntityUUID = data.getUUID("hitchEntityUUID");
		this.hookupEntityUUID = data.getUUID("hookupEntityUUID");
		this.hitchGroupIndex = data.getInteger("hitchGroupIndex");
		this.hitchConnectionIndex = data.getInteger("hitchConnectionIndex");
		this.hookupGroupIndex = data.getInteger("hookupGroupIndex");
		this.hookupConnectionIndex = data.getInteger("hookupConnectionIndex");
	}
	
	public boolean setConnection(WrapperWorld world){
		if(hitchEntity == null){
			hitchEntity = world.getEntity(hitchEntityUUID);
		}
		if(hitchEntity != null){
			hitchVehicle = hitchEntity instanceof APart ? ((APart) hitchEntity).vehicleOn : (EntityVehicleF_Physics) hitchEntity;
			hitchConnectionGroup = hitchEntity.definition.connectionGroups.get(hitchGroupIndex);
			hitchConnection = hitchConnectionGroup.connections.get(hitchConnectionIndex);
		}
		if(hookupEntity == null){
			hookupEntity = world.getEntity(hookupEntityUUID);
		}
		if(hookupEntity != null){
			hookupVehicle = hookupEntity instanceof APart ? ((APart) hookupEntity).vehicleOn : (EntityVehicleF_Physics) hookupEntity;
			hookupConnectionGroup = hookupEntity.definition.connectionGroups.get(hookupGroupIndex);
			hookupConnection = hookupConnectionGroup.connections.get(hookupConnectionIndex);
		}
		return hitchConnection != null && hookupConnection != null;
	}

	public void update(){
		hitchPriorPosition.set(hitchConnection.pos);
		hitchEntity.prevOrientation.transform(hitchPriorPosition);
		hitchPriorPosition.add(hitchEntity.prevPosition);
		
		hitchCurrentPosition.set(hitchConnection.pos);
		hitchEntity.orientation.transform(hitchCurrentPosition);
		hitchCurrentPosition.add(hitchEntity.position);
		
		hookupPriorPosition.set(hookupConnection.pos);
		hookupEntity.prevOrientation.transform(hookupPriorPosition);
		hookupPriorPosition.add(hookupEntity.prevPosition);
		
		hookupCurrentPosition.set(hookupConnection.pos);
		hookupEntity.orientation.transform(hookupCurrentPosition);
		hookupCurrentPosition.add(hookupEntity.position);
	}
	
	public WrapperNBT getData(){
		WrapperNBT data = new WrapperNBT();
		data.setUUID("hitchEntityUUID", hitchEntityUUID);
		data.setUUID("hookupEntityUUID", hookupEntityUUID);
		data.setInteger("hitchGroupIndex", hitchGroupIndex);
		data.setInteger("hitchConnectionIndex", hitchConnectionIndex);
		data.setInteger("hookupGroupIndex", hookupGroupIndex);
		data.setInteger("hookupGroupIndex", hookupGroupIndex);
		return data;
	}
}
