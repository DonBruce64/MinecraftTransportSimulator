package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;

/**Chest tile entity.
 *
 * @author don_bruce
 */
public class TileEntityChest extends TileEntityDecor{
	
	public final EntityInventoryContainer inventory;
	
	public TileEntityChest(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, position, placingPlayer, data);
		this.inventory = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), (int) (definition.decor.inventoryUnits*9F));
		world.addEntity(inventory);
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.INVENTORY_CHEST));
		return true;
	}

	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("inventory_count"): {
				if(inventory != null){
					return inventory.getCount();
				}else{
					return 0;
				}
			}
			case("inventory_percent"): {
				if(inventory != null){
					return inventory.getCount()/(double)inventory.getSize();
				}else{
					return 0;
				}
			}
			case("inventory_capacity"): {
				if(inventory != null){
					return inventory.getSize();
				}else{
					return 0;
				}
			}
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		if(inventory != null){
			data.setData("inventory", inventory.save(new WrapperNBT()));
		}
		return data;
	}
}
