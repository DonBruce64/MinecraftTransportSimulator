package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;

public final class PartBrewingStand extends APart<EntityVehicleE_Powered>{
	private final TileEntityBrewingStandVehicle fakeBrewingStand;
	
	public PartBrewingStand(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		//Make sure we have registered our fake brewing stand.  If not, the game won't save it.
		if(TileEntity.getKey(TileEntityBrewingStandVehicle.class) == null){
			TileEntity.register("brewing_stand_vehicle", TileEntityBrewingStandVehicle.class);
		}
		fakeBrewingStand = new TileEntityBrewingStandVehicle();
		fakeBrewingStand.setWorld(vehicle.world);
		fakeBrewingStand.readFromNBT(dataTag);
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		if(!vehicle.locked){
			player.displayGUIChest(fakeBrewingStand);
		}else{
			MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
		}
		return true;
    }
	
	@Override
	public void updatePart(){
		super.updatePart();
		fakeBrewingStand.update();
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return fakeBrewingStand.writeToNBT(new NBTTagCompound()); 
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
	
	public class TileEntityBrewingStandVehicle extends TileEntityBrewingStand{
		@Override
		public boolean isUsableByPlayer(EntityPlayer player){
			return true; 
	    }
	}
}
