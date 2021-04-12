package minecrafttransportsimulator.entities.instances;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPart.EffectorComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import net.minecraft.item.ItemStack;

public class PartEffector extends APart{
	private final Point3d[] lastBlocksModified;
	private final Point3d[] affectedBlocks;
	
	public PartEffector(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
		lastBlocksModified = new Point3d[definition.effector.blocksWide];
		affectedBlocks = new Point3d[definition.effector.blocksWide];
	}
	
	@Override
	public void update(){
		super.update();
		int startingIndex = -definition.effector.blocksWide/2;
		for(int i=0; i<definition.effector.blocksWide; ++i){
			int xOffset = startingIndex + i;
			affectedBlocks[i] = new Point3d(xOffset, 0, 0).rotateCoarse(localAngles).add(position);
			if(definition.effector.type.equals(EffectorComponentType.PLANTER) || definition.effector.type.equals(EffectorComponentType.PLOW)){
				affectedBlocks[i].add(0, -1, 0);
			}
		}
		
		for(byte i=0; i<affectedBlocks.length; ++i){
			if(!affectedBlocks[i].equals(lastBlocksModified[i])){
				switch(definition.effector.type){
					case FERTILIZER: {
						//Search all inventories for fertilizer and try to use it.
						for(APart part : entityOn.parts){
							if(part instanceof PartInteractable){
								if(part.definition.interactable.feedsVehicles){
									for(ItemStack stack : ((PartInteractable) part).inventory){
										if(world.fertilizeBlock(affectedBlocks[i], stack)){
											stack.shrink(1);
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
						List<ItemStack> drops = world.harvestBlock(affectedBlocks[i]);
						if(drops != null){
							Iterator<ItemStack> iterator = drops.iterator();
							while(iterator.hasNext()){
								ItemStack dropStack = iterator.next();
								for(APart part : entityOn.parts){
									if(part instanceof PartInteractable){
										((PartInteractable) part).addStackToInventory(dropStack);
										if(dropStack.isEmpty()){
											iterator.remove();
											break;
										}
									}
								}
							}
							
							//Check our drops.  If we couldn't add any of them to any inventory, drop them on the ground instead.
							for(ItemStack stack : drops){
								world.spawnItemStack(stack, position);
							}
						}
						break;
					}
					case PLANTER: {
						//Search all inventories for seeds and try to plant them.
						for(APart part : entityOn.parts){
							if(part instanceof PartInteractable){
								if(part.definition.interactable.feedsVehicles){
									for(ItemStack stack : ((PartInteractable) part).inventory){
										if(world.plantBlock(affectedBlocks[i], stack)){
											stack.shrink(1);
											break;
										}
									}
								}
							}
						}
						break;
					}
					case PLOW:{
						if(world.plowBlock(affectedBlocks[i])){
							//Harvest blocks on top of this block in case they need to be dropped.
							List<ItemStack> drops = world.harvestBlock(affectedBlocks[i].copy().add(0, 1, 0));
							if(drops != null){
								for(ItemStack stack : drops){
									if(stack.getCount() > 0){
										world.spawnItemStack(stack, position);
									}
								}
							}
						}
						break;
					}
				}
				lastBlocksModified[i] = affectedBlocks[i];
			}
		}
	}
}
