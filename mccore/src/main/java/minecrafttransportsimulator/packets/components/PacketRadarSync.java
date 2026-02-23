package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable.RadarContactStub;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Packet used to sync radar contact data from server to client.
 * This allows radar to detect vehicles that are in loaded chunks but outside
 * client render distance.
 * 
 * @author don_bruce
 */
public class PacketRadarSync extends APacketBase {
    private final UUID radarEntityUUID;
    private final Point3D radarPosition;
    private final List<RadarContactData> aircraftContacts;
    private final List<RadarContactData> grounderContacts;
    private final List<UUID> trackedVehicleUUIDs;

    public PacketRadarSync(UUID radarEntityUUID, Point3D radarPosition, List<RadarContactData> aircraftContacts, List<RadarContactData> grounderContacts, List<UUID> trackedVehicleUUIDs) {
        super(null);
        this.radarEntityUUID = radarEntityUUID;
        this.radarPosition = radarPosition;
        this.aircraftContacts = aircraftContacts;
        this.grounderContacts = grounderContacts;
        this.trackedVehicleUUIDs = trackedVehicleUUIDs;
    }

    public PacketRadarSync(ByteBuf buf) {
        super(buf);
        this.radarEntityUUID = readUUIDFromBuffer(buf);
        this.radarPosition = readPoint3dFromBuffer(buf);

        int aircraftCount = buf.readInt();
        this.aircraftContacts = new ArrayList<>(aircraftCount);
        for (int i = 0; i < aircraftCount; i++) {
            aircraftContacts.add(new RadarContactData(readUUIDFromBuffer(buf), readPoint3dFromBuffer(buf), buf.readDouble()));
        }
        
        int grounderCount = buf.readInt();
        this.grounderContacts = new ArrayList<>(grounderCount);
        for (int i = 0; i < grounderCount; i++) {
            grounderContacts.add(new RadarContactData(readUUIDFromBuffer(buf), readPoint3dFromBuffer(buf), buf.readDouble()));
        }

        int trackedCount = buf.readInt();
        this.trackedVehicleUUIDs = new ArrayList<>(trackedCount);
        for (int i = 0; i < trackedCount; i++) {
            trackedVehicleUUIDs.add(readUUIDFromBuffer(buf));
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(radarEntityUUID, buf);
        writePoint3dToBuffer(radarPosition, buf);

        buf.writeInt(aircraftContacts.size());
        for (RadarContactData contact : aircraftContacts) {
            writeUUIDToBuffer(contact.uuid, buf);
            writePoint3dToBuffer(contact.position, buf);
            buf.writeDouble(contact.velocity);
        }
        
        buf.writeInt(grounderContacts.size());
        for (RadarContactData contact : grounderContacts) {
            writeUUIDToBuffer(contact.uuid, buf);
            writePoint3dToBuffer(contact.position, buf);
            buf.writeDouble(contact.velocity);
        }

        buf.writeInt(trackedVehicleUUIDs.size());
        for (UUID trackedUUID : trackedVehicleUUIDs) {
            writeUUIDToBuffer(trackedUUID, buf);
        }
    }

    @Override
    public void handle(AWrapperWorld world) {
        //Find the radar entity and update its radar contacts
        AEntityD_Definable<?> entity = world.getEntity(radarEntityUUID);
        if (entity != null) {
            entity.setRadarContacts(aircraftContacts, grounderContacts);
        }

        //Update tracking info for each tracked vehicle
        for (UUID trackedUUID : trackedVehicleUUIDs) {
            AEntityD_Definable<?> trackedVehicle = world.getEntity(trackedUUID);
            if (trackedVehicle != null) {
                trackedVehicle.addRadarTrackingThis(radarEntityUUID, radarPosition);
            }
        }
    }
    
    /**
     * Simple data class to hold radar contact information.
     * Used to sync data from server to client without needing full entity references.
     */
    public static class RadarContactData {
        public final UUID uuid;
        public final Point3D position;
        public final double velocity;
        
        public RadarContactData(UUID uuid, Point3D position, double velocity) {
            this.uuid = uuid;
            this.position = position;
            this.velocity = velocity;
        }
    }
}