package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Class for easier save/load of trailer connections.
 * 
 * @author don_bruce
 */
public class TrailerConnection{
	private final String hitchEntityUUID;
	private final String hookupEntityUUID;
	private final int hitchGroupIndex;
	private final int hitchConnectionIndex;
	private final int hookupGroupIndex;
	private final int hookupConnectionIndex;
	public AEntityD_Interactable<?> hitchEntity;
	public AEntityD_Interactable<?> hitchBaseEntity;
	public AEntityD_Interactable<?> hookupEntity;
	public AEntityD_Interactable<?> hookupBaseEntity;
	public JSONConnectionGroup hitchConnectionGroup;
	public JSONConnection hitchConnection;
	public JSONConnectionGroup hookupConnectionGroup;
	public JSONConnection hookupConnection;
	
	public TrailerConnection(AEntityD_Interactable<?> hitchEntity, int hitchGroupIndex, int hitchConnectionIndex, AEntityD_Interactable<?> hookupEntity, int hookupGroupIndex, int hookupConnectionIndex){
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
		this.hitchEntityUUID = data.getString("hitchEntityUUID");
		this.hookupEntityUUID = data.getString("hookupEntityUUID");
		this.hitchGroupIndex = data.getInteger("hitchGroupIndex");
		this.hitchConnectionIndex = data.getInteger("hitchConnectionIndex");
		this.hookupGroupIndex = data.getInteger("hookupGroupIndex");
		this.hookupConnectionIndex = data.getInteger("hookupConnectionIndex");
	}
	
	public boolean setConnection(WrapperWorld world){
		if(hitchEntity == null){
			hitchEntity = AEntityA_Base.getEntity(world, hitchEntityUUID);
		}
		if(hitchEntity != null){
			hitchBaseEntity = hitchEntity instanceof APart ? ((APart) hitchEntity).entityOn : hitchEntity;
			hitchConnectionGroup = hitchEntity.definition.connectionGroups.get(hitchGroupIndex);
			hitchConnection = hitchConnectionGroup.connections.get(hitchConnectionIndex);
		}
		if(hookupEntity == null){
			hookupEntity = AEntityA_Base.getEntity(world, hookupEntityUUID);
		}
		if(hookupEntity != null){
			hookupBaseEntity = hookupEntity instanceof APart ? ((APart) hookupEntity).entityOn : hookupEntity;
			hookupConnectionGroup = hookupEntity.definition.connectionGroups.get(hookupGroupIndex);
			hookupConnection = hookupConnectionGroup.connections.get(hookupConnectionIndex);
		}
		return hitchConnection != null && hookupConnection != null;
	}
	
	public Point3d getHitchOffset(){
		return hitchConnection.pos.copy();
	}
	
	public Point3d getHookupOffset(){
		return hookupConnection.pos.copy();
	}
	
	public WrapperNBT getData(){
		WrapperNBT data = new WrapperNBT();
		data.setString("hitchEntityUUID", hitchEntityUUID);
		data.setString("hookupEntityUUID", hookupEntityUUID);
		data.setInteger("hitchGroupIndex", hitchGroupIndex);
		data.setInteger("hitchConnectionIndex", hitchConnectionIndex);
		data.setInteger("hookupGroupIndex", hookupGroupIndex);
		data.setInteger("hookupGroupIndex", hookupGroupIndex);
		return data;
	}
}
