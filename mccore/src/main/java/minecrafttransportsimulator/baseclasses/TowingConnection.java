package minecrafttransportsimulator.baseclasses;

import java.util.UUID;

import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;

/**
 * Class for easier save/load of towing connections.
 *
 * @author don_bruce
 */
public class TowingConnection {
    private final UUID hitchEntityUUID;
    private final UUID hookupEntityUUID;
    public final int hitchGroupIndex;
    public final int hitchConnectionIndex;
    public final int hookupGroupIndex;
    public final int hookupConnectionIndex;
    public EntityVehicleF_Physics towingVehicle;
    public AEntityE_Interactable<?> towingEntity;
    public EntityVehicleF_Physics towedVehicle;
    public AEntityE_Interactable<?> towedEntity;
    public JSONConnectionGroup hitchConnectionGroup;
    public JSONConnection hitchConnection;
    public JSONConnectionGroup hookupConnectionGroup;
    public JSONConnection hookupConnection;
    public final Point3D hitchPriorPosition = new Point3D();
    public final Point3D hitchCurrentPosition = new Point3D();
    public final Point3D hookupPriorPosition = new Point3D();
    public final Point3D hookupCurrentPosition = new Point3D();

    public TowingConnection(AEntityE_Interactable<?> hitchEntity, int hitchGroupIndex, int hitchConnectionIndex, AEntityE_Interactable<?> hookupEntity, int hookupGroupIndex, int hookupConnectionIndex) {
        this.hitchEntityUUID = hitchEntity.uniqueUUID;
        this.hookupEntityUUID = hookupEntity.uniqueUUID;
        this.hitchGroupIndex = hitchGroupIndex;
        this.hitchConnectionIndex = hitchConnectionIndex;
        this.hookupGroupIndex = hookupGroupIndex;
        this.hookupConnectionIndex = hookupConnectionIndex;
        this.towingVehicle = hitchEntity instanceof APart ? ((APart) hitchEntity).vehicleOn : (EntityVehicleF_Physics) hitchEntity;
        this.towedVehicle = hookupEntity instanceof APart ? ((APart) hookupEntity).vehicleOn : (EntityVehicleF_Physics) hookupEntity;
        initConnection(hitchEntity.world);
    }

    public TowingConnection(IWrapperNBT data) {
        this.hitchEntityUUID = data.getUUID("hitchEntityUUID");
        this.hookupEntityUUID = data.getUUID("hookupEntityUUID");
        this.hitchGroupIndex = data.getInteger("hitchGroupIndex");
        this.hitchConnectionIndex = data.getInteger("hitchConnectionIndex");
        this.hookupGroupIndex = data.getInteger("hookupGroupIndex");
        this.hookupConnectionIndex = data.getInteger("hookupConnectionIndex");
    }

    /**
     * Tries to initialize the connection, finding all entities.  Returns true if the connection
     * was initialized, false if an entity could not be found.  It is likely that this method will
     * not return true on the first tick of the entity this connection was saved on due to differing load
     * times of entities in the world.  Just call it during later ticks for a few calls until you think
     * too much time has passed and the entities just don't exist in the world.
     */
    public boolean initConnection(AWrapperWorld world) {
        if (towingEntity == null) {
            towingEntity = world.getEntity(hitchEntityUUID);
            if (towingEntity != null) {
                towingVehicle = towingEntity instanceof APart ? ((APart) towingEntity).vehicleOn : (EntityVehicleF_Physics) towingEntity;
                if (towingEntity.definition.connectionGroups.size() > hitchGroupIndex) {
                    hitchConnectionGroup = towingEntity.definition.connectionGroups.get(hitchGroupIndex);
                    if (hitchConnectionGroup.connections.size() > hitchConnectionIndex) {
                        hitchConnection = hitchConnectionGroup.connections.get(hitchConnectionIndex);
                    }
                }
            }
        }

        if (towedEntity == null) {
            towedEntity = world.getEntity(hookupEntityUUID);
            if (towedEntity != null) {
                towedVehicle = towedEntity instanceof APart ? ((APart) towedEntity).vehicleOn : (EntityVehicleF_Physics) towedEntity;
                if (towedEntity.definition.connectionGroups.size() > hookupGroupIndex) {
                    hookupConnectionGroup = towedEntity.definition.connectionGroups.get(hookupGroupIndex);
                    if (hookupConnectionGroup.connections.size() > hookupConnectionIndex) {
                        hookupConnection = hookupConnectionGroup.connections.get(hookupConnectionIndex);
                    }
                }
            }
        }
        return hitchConnection != null && hookupConnection != null && towedVehicle != towingVehicle;
    }

    public IWrapperNBT save(IWrapperNBT data) {
        data.setUUID("hitchEntityUUID", hitchEntityUUID);
        data.setUUID("hookupEntityUUID", hookupEntityUUID);
        data.setInteger("hitchGroupIndex", hitchGroupIndex);
        data.setInteger("hitchConnectionIndex", hitchConnectionIndex);
        data.setInteger("hookupGroupIndex", hookupGroupIndex);
        data.setInteger("hookupConnectionIndex", hookupConnectionIndex);
        return data;
    }
}
