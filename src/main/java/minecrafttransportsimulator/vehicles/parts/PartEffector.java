package minecrafttransportsimulator.vehicles.parts;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart.EffectorComponentType;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.item.ItemStack;

public class PartEffector extends APart{
	protected final Point3i[] lastBlocksModified;
	protected final Point3i[] affectedBlocks;
	
	public PartEffector(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, WrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
		lastBlocksModified = new Point3i[definition.effector.blocksWide];
		affectedBlocks = new Point3i[definition.effector.blocksWide];
	}
	
	@Override
	public void update(){
		super.update();
		int startingIndex = -definition.effector.blocksWide/2;
		for(int i=0; i<definition.effector.blocksWide; ++i){
			int xOffset = startingIndex + i;
			Point3d partAffectorPosition = new Point3d(xOffset, 0, 0).rotateCoarse(totalRotation).add(worldPos);
			affectedBlocks[i] = new Point3i(partAffectorPosition);
			if(definition.effector.type.equals(EffectorComponentType.PLANTER) || definition.effector.type.equals(EffectorComponentType.PLOW)){
				affectedBlocks[i].add(0, -1, 0);
			}
		}
		
		for(byte i=0; i<affectedBlocks.length; ++i){
			if(!affectedBlocks[i].equals(lastBlocksModified[i])){
				switch(definition.effector.type){
					case FERTILIZER: {
						//Search all inventories for fertilizer and try to use it.
						for(APart part : vehicle.parts){
							if(part instanceof PartInteractable){
								WrapperInventory inventory = ((PartInteractable) part).inventory;
								if(inventory != null && part.definition.interactable.feedsVehicles){
									for(int j=0; j<inventory.getSize(); ++j){
										if(vehicle.world.fertilizeBlock(affectedBlocks[i], inventory.getStackInSlot(j))){
											inventory.decrementSlot(j);
											break;
										}
									}
								}
							}
						}
						break;
					}
					case HARVESTER: {
						//Harvest drops, and add to inventories.
						List<ItemStack> drops = vehicle.world.harvestBlock(affectedBlocks[i]);
						if(drops != null){
							for(APart part : vehicle.parts){
								if(part instanceof PartInteractable){
									WrapperInventory inventory = ((PartInteractable) part).inventory;
									if(inventory != null){
										Iterator<ItemStack> iterator = drops.iterator();
										while(iterator.hasNext()){
											ItemStack stack = iterator.next();
											if(inventory.addStack(stack)){
												iterator.remove();
												break;
											}
										}
									}
								}
							}
							
							//Check our drops.  If we couldn't add any of them to any inventory, drop them on the ground instead.
							for(ItemStack stack : drops){
								if(stack.getCount() > 0){
									vehicle.world.spawnItemStack(stack, worldPos);
								}
							}
						}
						break;
					}
					case PLANTER: {
						//Search all inventories for seeds and try to plant them.
						for(APart part : vehicle.parts){
							if(part instanceof PartInteractable){
								WrapperInventory inventory = ((PartInteractable) part).inventory;
								if(inventory != null && part.definition.interactable.feedsVehicles){
									for(int j=0; j<inventory.getSize(); ++j){
										if(vehicle.world.plantBlock(affectedBlocks[i], inventory.getStackInSlot(j))){
											inventory.decrementSlot(j);
											break;
										}
									}
								}
							}
						}
						break;
					}
					case PLOW:{
						vehicle.world.plowBlock(affectedBlocks[i]);
						break;
					}
				}
				lastBlocksModified[i] = affectedBlocks[i];
			}
		}
	}
}
