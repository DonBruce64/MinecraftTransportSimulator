package minecrafttransportsimulator.vehicles.parts;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class PartEffector extends APart{
	protected final Point3i[] lastBlocksModified;
	protected final Point3i[] affectedBlocks;
	
	public PartEffector(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, IWrapperNBT data, APart parentPart){
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
			if(definition.effector.type.equals("planter") || definition.effector.type.equals("plow")){
				affectedBlocks[i].add(0, -1, 0);
			}
		}
		
		for(byte i=0; i<affectedBlocks.length; ++i){
			if(!affectedBlocks[i].equals(lastBlocksModified[i])){
				switch(definition.effector.type){
					case("fertilizer"): {
						//Search all inventories for fertilizer and try to use it.
						for(APart part : vehicle.parts){
							if(part instanceof PartInteractable){
								IWrapperInventory inventory = ((PartInteractable) part).inventory;
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
					case("harvester"): {
						//Harvest drops, and add to inventories.
						List<IWrapperItemStack> drops = vehicle.world.harvestBlock(affectedBlocks[i]);
						if(drops != null){
							for(APart part : vehicle.parts){
								if(part instanceof PartInteractable){
									IWrapperInventory inventory = ((PartInteractable) part).inventory;
									if(inventory != null){
										Iterator<IWrapperItemStack> iterator = drops.iterator();
										while(iterator.hasNext()){
											IWrapperItemStack stack = iterator.next();
											if(inventory.addStack(stack)){
												iterator.remove();
												break;
											}
										}
									}
								}
							}
							
							//Check our drops.  If we couldn't add any of them to any inventory, drop them on the ground instead.
							for(IWrapperItemStack stack : drops){
								if(stack.getSize() > 0){
									vehicle.world.spawnItemStack(stack, worldPos);
								}
							}
						}
						break;
					}
					case("planter"): {
						//Search all inventories for seeds and try to plant them.
						for(APart part : vehicle.parts){
							if(part instanceof PartInteractable){
								IWrapperInventory inventory = ((PartInteractable) part).inventory;
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
					case("plow"):{
						vehicle.world.plowBlock(affectedBlocks[i]);
						break;
					}
				}
				lastBlocksModified[i] = affectedBlocks[i];
			}
		}
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
}
