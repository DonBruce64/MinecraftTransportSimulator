package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartEffector;
import net.minecraft.item.ItemStack;

public class PartEffector extends APart{
	
	private final List<ItemStack> drops = new ArrayList<ItemStack>();
	
	//Variables used for drills.
	public int blocksBroken;
	private final Point3d flooredCenter = new Point3d();
	private final Map<BoundingBox, Point3d> boxLastPositionsFloored = new HashMap<BoundingBox, Point3d>();
	private final Map<BoundingBox, Integer> boxTimeSpentAtPosition = new HashMap<BoundingBox, Integer>();
	private final Set<Point3d> blockFlooredPositionsBrokeThisTick = new HashSet<Point3d>()
;	
	public PartEffector(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
		this.blocksBroken = data.getInteger("blocksBroken");
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//If we are active, do effector things.
			if(isActive && !world.isClient()){
				drops.clear();
				blockFlooredPositionsBrokeThisTick.clear();
				for(BoundingBox box : entityCollisionBoxes){
					switch(definition.effector.type){
						case FERTILIZER: {
							//Search all inventories for fertilizer and try to use it.
							for(APart part : entityOn.parts){
								if(part instanceof PartInteractable && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE) && part.definition.interactable.feedsVehicles){
									EntityInventoryContainer inventory = ((PartInteractable) part).inventory;
									for(int i=0; i<inventory.getSize(); ++i){
										ItemStack stack = inventory.getStack(i);
										if(world.fertilizeBlock(box.globalCenter, stack)){
											inventory.removeItems(i, 1, true);
											break;
										}
									}
								}
							}
							break;
						}
						case HARVESTER: {
							//Harvest drops, and add to inventories.
							drops.addAll(world.harvestBlock(box.globalCenter));
							break;
						}
						case PLANTER: {
							//Search all inventories for seeds and try to plant them.
							for(APart part : entityOn.parts){
								if(part instanceof PartInteractable && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE) && part.definition.interactable.feedsVehicles){
									EntityInventoryContainer inventory = ((PartInteractable) part).inventory;
									for(int i=0; i<inventory.getSize(); ++i){
										ItemStack stack = inventory.getStack(i);
										if(world.plantBlock(box.globalCenter, stack)){
											inventory.removeItems(i, 1, true);
											break;
										}
									}
								}
							}
							break;
						}
						case PLOW:{
							if(world.plowBlock(box.globalCenter)){
								//Harvest blocks on top of this block in case they need to be dropped.
								List<ItemStack> harvestedDrops = world.harvestBlock(box.globalCenter);
								if(!harvestedDrops.isEmpty()){
									for(ItemStack stack : harvestedDrops){
										if(stack.getCount() > 0){
											world.spawnItemStack(stack, position);
										}
									}
								}
							}
							break;
						}
						case SNOWPLOW:{
							world.removeSnow(box.globalCenter);
							break;
						}
						case DRILL:{
							if(!world.isAir(box.globalCenter)){
								float blockHardness = world.getBlockHardness(box.globalCenter);
								if(blockHardness <= definition.effector.drillHardness){
									if(!boxLastPositionsFloored.containsKey(box)){
										boxLastPositionsFloored.put(box, new Point3d());
										boxTimeSpentAtPosition.put(box, 0);
									}
									
									flooredCenter.set(Math.floor(box.globalCenter.x), Math.floor(box.globalCenter.y), Math.floor(box.globalCenter.z));
									if(boxLastPositionsFloored.get(box).equals(flooredCenter) && !blockFlooredPositionsBrokeThisTick.contains(flooredCenter)){
										int timeSpentBreaking = boxTimeSpentAtPosition.get(box);
										if(timeSpentBreaking >= definition.effector.drillSpeed*blockHardness/definition.effector.drillHardness){
											drops.addAll(world.getBlockDrops(flooredCenter));
											world.destroyBlock(flooredCenter, false);
											boxTimeSpentAtPosition.put(box, 0);
											blockFlooredPositionsBrokeThisTick.add(flooredCenter.copy());
											if(++blocksBroken == definition.effector.drillDurability){
												this.isValid = false;
											}else{
												InterfacePacket.sendToAllClients(new PacketPartEffector(this));
											}
										}else{
											boxTimeSpentAtPosition.put(box, timeSpentBreaking + 1);
										}
										break;
									}
									boxLastPositionsFloored.put(box, flooredCenter.copy());
								}
							}
							boxTimeSpentAtPosition.put(box, 0);
							break;
						}
					}
					
					//Handle any drops we got from our effector.
					if(!drops.isEmpty()){
						Iterator<ItemStack> iterator = drops.iterator();
						while(iterator.hasNext()){
							ItemStack dropStack = iterator.next();
							for(APart part : entityOn.parts){
								if(part instanceof PartInteractable && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE)){
									if(((PartInteractable) part).inventory.addStack(dropStack, true) == dropStack.getCount()){
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
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("effector_active"): return isActive ? 1 : 0;
			case("effector_drill_broken"): return blocksBroken;
			case("effector_drill_max"): return definition.effector.drillDurability;
			case("effector_drill_percentage"): return blocksBroken/(double)definition.effector.drillDurability;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	@Override
    public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setInteger("blocksBroken", blocksBroken);
    	return data;
    }
}
