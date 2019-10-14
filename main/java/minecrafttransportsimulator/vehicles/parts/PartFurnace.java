package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.packets.general.PacketClientChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;

public final class PartFurnace extends APart{
	private final TileEntityFurnaceVehicle fakeFurnace;
	
	public PartFurnace(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		//Make sure we have registered our fake furnace.  If not, the game won't save it.
		if(TileEntity.getKey(TileEntityFurnaceVehicle.class) == null){
			TileEntity.register("furnace_vehicle", TileEntityFurnaceVehicle.class);
		}
		fakeFurnace = new TileEntityFurnaceVehicle();
		fakeFurnace.setWorld(vehicle.world);
		fakeFurnace.readFromNBT(dataTag);
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		if(!vehicle.locked){
			player.displayGUIChest(fakeFurnace);
		}else{
			MTS.MTSNet.sendTo(new PacketClientChat("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
		}
		return true;
    }
	
	@Override
	public void updatePart(){
		super.updatePart();
		fakeFurnace.update();
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return fakeFurnace.writeToNBT(new NBTTagCompound()); 
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
	
	public class TileEntityFurnaceVehicle extends TileEntityFurnace{
		private boolean burningStateAtStartOfUpdate;
		private boolean runningUpdate;
		
		@Override
		public boolean isUsableByPlayer(EntityPlayer player){
			return true; 
	    }
		
		//Override this to prevent the furnace from setting blockstates for furnaces in the world that don't exist.
		//The only time this blockstate gets set is if the furnace changes burning states during the update() call,
		//so get the value at the start of the update and return that throughout the update call.
		//Once the update is done, we can update the cached value and return that.  Because MC won't see a change
		//during the update call, it won't set any blockstates of non-existent blocks.
		@Override
		public boolean isBurning(){
			return runningUpdate ? burningStateAtStartOfUpdate : super.isBurning();
	    }
		
		@Override
		public void update(){
			burningStateAtStartOfUpdate = isBurning();
			runningUpdate = true;
			super.update();
			runningUpdate = false;
		}
	}
}
