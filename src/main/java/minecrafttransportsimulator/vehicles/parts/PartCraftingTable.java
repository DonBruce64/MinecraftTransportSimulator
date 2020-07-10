package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PartCraftingTable extends APart{
	
	public PartCraftingTable(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		if(!vehicle.locked){
			player.displayGui(new CraftingTableInterfaceVehicle(vehicle.world, vehicle.getPosition()));
		}else{
			MTS.MTSNet.sendTo(new PacketChat("interact.failure.vehiclelocked"), (EntityPlayerMP) player);
		}
		return true;
    }
	
	@Override
	public NBTTagCompound getData(){
		return new NBTTagCompound();
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
	
	private class CraftingTableInterfaceVehicle extends BlockWorkbench.InterfaceCraftingTable{
		//Need to save these as they're private and we need them for our overridden method.
		private World worldMap;
		private BlockPos posMap;
		
		public CraftingTableInterfaceVehicle(World world, BlockPos pos){
			super(world, pos);
			this.worldMap = world;
			this.posMap = pos;
		}
		
		@Override
		public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player){
            return new ContainerWorkbench(playerInventory, worldMap, posMap){
            	@Override
                public boolean canInteractWith(EntityPlayer playerIn){
            		return true;
                }
            };
        }
	}
}
