package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;

/**Packet sent to fluid loaders  on clients to change what part they are connected to.
 * 
 * @author don_bruce
 */
public class PacketTileEntityFluidLoaderConnection extends APacketTileEntity<TileEntityFluidLoader>{
	private final int vehicleID;
	private final Point3d partOffset;
	private final boolean connect;
	
	public PacketTileEntityFluidLoaderConnection(TileEntityFluidLoader loader, boolean connect){
		super(loader);
		this.vehicleID = loader.connectedPart.vehicle.lookupID;
		this.partOffset = loader.connectedPart.placementOffset;
		this.connect = connect;
	}
	
	public PacketTileEntityFluidLoaderConnection(ByteBuf buf){
		super(buf);
		this.vehicleID = buf.readInt();
		this.partOffset = readPoint3dFromBuffer(buf);
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(vehicleID);
		buf.writeBoolean(connect);
		writePoint3dToBuffer(partOffset, buf);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityFluidLoader loader){
		for(AEntityBase entity : AEntityBase.createdClientEntities){
			if(entity.lookupID == vehicleID){
				EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
				if(connect){
					loader.connectedPart = (PartInteractable) vehicle.getPartAtLocation(partOffset);
					loader.getTank().resetAmountDispensed();
				}else{
					loader.connectedPart = null;
				}
				return true;
			}
		}
		return true;
	}
}
